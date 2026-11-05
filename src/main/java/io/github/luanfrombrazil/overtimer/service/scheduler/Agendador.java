package io.github.luanfrombrazil.overtimer.service.scheduler;

import io.github.luanfrombrazil.overtimer.model.Configuracoes;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.LimiteDeUso;
import io.github.luanfrombrazil.overtimer.model.Direcionamento;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.HorarioFuncionamento;
import io.github.luanfrombrazil.overtimer.model.Modelo;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.ProjetoService;
import io.github.luanfrombrazil.overtimer.service.agente.Agente;
import io.github.luanfrombrazil.overtimer.service.agente.Agentes;
import io.github.luanfrombrazil.overtimer.service.agente.AreaDeTrabalho;
import io.github.luanfrombrazil.overtimer.service.agente.Execucao;
import io.github.luanfrombrazil.overtimer.service.agente.ResultadoExecucao;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.ToIntFunction;

/**
 * Entrega o trabalho da fila aos agentes, um de cada vez: subtarefas ou, quando não há subtarefas, a Task inteira.
 * <p>
 * Enquanto está rodando, faz um ciclo a cada {@linkplain #usarIntervalo intervalo} (contado do fim do ciclo
 * anterior; padrão {@link #INTERVALO_PADRAO}) e também logo depois de {@link #agora()}: pega o próximo trabalho
 * liberado, executa no agente da Task e repete até a fila esvaziar ou chegar ao máximo de execuções por intervalo.
 * Pausar não interrompe a execução em andamento; só impede que a próxima comece.
 * <p>
 * O máximo conta as execuções que começaram desde o último ciclo do intervalo, inclusive as dos ciclos de
 * {@link #agora()}; o contador zera a cada ciclo do intervalo.
 * <p>
 * Uma Task criada com o agendador rodando só é executada a partir do próximo ciclo do intervalo (os de
 * {@link #agora()} não contam), para dar tempo de criar as subtarefas dela. Se esse ciclo está a menos de meio
 * intervalo, ela espera um ciclo a mais.
 * <p>
 * Só começa uma execução dentro do {@linkplain #usarHorario horário de funcionamento}; a que já está em andamento
 * quando o horário fecha vai até o fim. Fora do horário, além dos ciclos do intervalo, acorda sozinho quando o
 * horário abre.
 * <p>
 * Não começa uma Task num provedor cujo {@linkplain #usarConsumo uso medido} (sessão ou semanal) chegou ao limite de
 * uso dele nas configurações; pula para a próxima Task de outro provedor. Uso desconhecido não impede.
 */
public class Agendador {

    public static final Duration INTERVALO_PADRAO = Duration.ofMinutes(5);
    /** Valor de {@link #usarIntervalo} para não limitar as execuções por intervalo. */
    public static final int SEM_LIMITE = 0;
    private static final LimiteDeUso SEM_LIMITE_DE_USO = new LimiteDeUso(100, 0);
    public static final Duration LIMITE_DA_EXECUCAO = Duration.ofMinutes(60);
    private static final int LINHAS_EM_MEMORIA = 300;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** O que a barra superior mostra. */
    public record Situacao(boolean rodando, String texto) {
    }

    /** Avisado na thread do agendador; quem atualiza a tela deve passar para a thread do Swing. */
    public interface Ouvinte {
        void situacao(Situacao situacao);

        void log(String projetoId, String linha);

        /**
         * Uma Task começou a ser executada. As subtarefas seguidas da mesma Task avisam uma vez só, na primeira.
         */
        default void taskIniciada(String projetoId, String taskId) {
        }
    }

    private final ProjetoService projetos;
    private final Agentes agentes;
    private final AreaDeTrabalho areaDeTrabalho;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(tarefa -> {
        Thread thread = new Thread(tarefa, "agendador");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, Deque<String>> logs = new ConcurrentHashMap<>();
    private final List<Ouvinte> ouvintes = new CopyOnWriteArrayList<>();
    private volatile boolean rodando;
    private volatile String emExecucao;
    /** Id da última Task que começou a rodar, para avisar {@link Ouvinte#taskIniciada} só quando ela muda. */
    private volatile String ultimaTaskIniciada;
    /** Quando começa o próximo ciclo do intervalo; null enquanto um deles está em andamento. */
    private volatile Instant proximoCiclo;
    /** Quantos ciclos do intervalo já começaram. */
    private volatile long ciclosDoIntervalo;
    /** Tasks recém-criadas, pelo id, e o ciclo do intervalo a partir do qual podem ser executadas. */
    private final Map<String, Long> retidas = new ConcurrentHashMap<>();
    private ScheduledFuture<?> ciclos;
    /** Horário em que pode começar uma execução; até receber um, roda sempre. */
    private volatile HorarioFuncionamento horario = HorarioFuncionamento.sempre();
    /** Ciclo marcado para quando o horário de funcionamento abrir; null se não há nenhum. */
    private ScheduledFuture<?> abertura;
    private volatile Duration intervalo = INTERVALO_PADRAO;
    /** Máximo de execuções que podem começar em cada intervalo; {@link #SEM_LIMITE} para não limitar. */
    private volatile int maximoPorIntervalo = SEM_LIMITE;
    /** Execuções que começaram desde o último ciclo do intervalo. */
    private volatile int execucoesNoIntervalo;
    /** Limite de uso de cada provedor; até receber as configurações, não limita. */
    private volatile Map<Provedor, LimiteDeUso> limitesDeUso = Map.of();
    /** Modelos de prompt e regras das variáveis; até receber as configurações, os padrão. */
    private volatile Configuracoes.Prompts prompts = Configuracoes.Prompts.padrao();
    /** Uso medido de cada provedor, de 0 a 100, ou negativo quando desconhecido. */
    private volatile ToIntFunction<Provedor> uso = provedor -> -1;
    /** Por que o último ciclo parou com trabalho na fila por causa do limite de uso; null se não parou por isso. */
    private volatile String noLimiteDeUso;

    public Agendador(ProjetoService projetos, Agentes agentes, AreaDeTrabalho areaDeTrabalho) {
        this.projetos = projetos;
        this.agentes = agentes;
        this.areaDeTrabalho = areaDeTrabalho;
        projetos.aoCriarTask(this::reter);
    }

    public void aoMudar(Ouvinte ouvinte) {
        ouvintes.add(ouvinte);
    }

    public boolean rodando() {
        return rodando;
    }

    public synchronized void iniciar() {
        if (rodando) {
            return;
        }
        rodando = true;
        // Ligar o agendador é uma decisão do usuário: o que estava retido entra já no primeiro ciclo.
        retidas.clear();
        proximoCiclo = Instant.now();
        ciclos = executor.scheduleWithFixedDelay(this::cicloDoIntervalo, 0, intervalo.toMillis(),
                TimeUnit.MILLISECONDS);
        avisarSituacao();
    }

    /**
     * Troca o intervalo entre os ciclos e o máximo de execuções por intervalo (ao abrir a app e ao salvar as
     * configurações). Esperando o próximo ciclo, ele passa a ser contado com o intervalo novo a partir do fim do
     * último.
     */
    public synchronized void usarIntervalo(Duration novo, int maximo) {
        if (novo.isNegative() || novo.isZero() || maximo < 0) {
            throw new IllegalArgumentException("intervalo inválido: " + novo + ", máximo " + maximo);
        }
        Duration antigo = intervalo;
        intervalo = novo;
        maximoPorIntervalo = maximo;
        if (rodando && !novo.equals(antigo) && !executor.isShutdown()) {
            ciclos.cancel(false);
            Instant proximo = proximoCiclo;
            long espera = novo.toMillis();
            if (proximo != null) {
                Instant fimDoUltimo = proximo.minus(antigo);
                espera = Math.max(0, Duration.between(Instant.now(), fimDoUltimo.plus(novo)).toMillis());
                proximoCiclo = Instant.now().plusMillis(espera);
            }
            // Com um ciclo em andamento, o fim dele marca o próximo já com o intervalo novo.
            ciclos = executor.scheduleWithFixedDelay(this::cicloDoIntervalo, espera, novo.toMillis(),
                    TimeUnit.MILLISECONDS);
        }
        avisarSituacao();
        // Um máximo maior pode liberar o que estava esperando.
        agora();
    }

    /**
     * Aplica o que das configurações vale para o agendador: horário de funcionamento, intervalo, máximo e os prompts
     * das próximas execuções.
     */
    public void aplicar(Configuracoes configuracoes) {
        limitesDeUso = configuracoes.limitesDeUso();
        prompts = configuracoes.prompts();
        usarHorario(configuracoes.horarioFuncionamento());
        Configuracoes.Execucao execucao = configuracoes.execucao();
        usarIntervalo(Duration.ofMinutes(execucao.intervaloMinutos()), execucao.maximoPorIntervalo());
    }

    public Duration intervalo() {
        return intervalo;
    }

    /**
     * De onde ler o uso de cada provedor (0 a 100, negativo quando desconhecido). Sem isso, o limite de uso não
     * impede nada.
     */
    public void usarConsumo(ToIntFunction<Provedor> uso) {
        this.uso = uso;
    }

    /**
     * Se a Task pode começar pelo limite de uso do provedor que vai executá-la. Sem agente, deixa passar: a
     * execução registra o erro de falta de agente.
     */
    private boolean dentroDoLimiteDeUso(Task task) {
        Optional<Agente> agente = agentes.para(task.provedor());
        if (agente.isEmpty()) {
            return true;
        }
        Provedor provedor = agente.get().provedor();
        return limitesDeUso.getOrDefault(provedor, SEM_LIMITE_DE_USO).permite(usoDe(provedor));
    }

    private int usoDe(Provedor provedor) {
        try {
            return uso.applyAsInt(provedor);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private boolean limiteAtingido() {
        int maximo = maximoPorIntervalo;
        return maximo != SEM_LIMITE && execucoesNoIntervalo >= maximo;
    }

    public synchronized void pausar() {
        rodando = false;
        if (ciclos != null) {
            ciclos.cancel(false);
        }
        marcarAbertura();
        avisarSituacao();
    }

    /**
     * Troca o horário de funcionamento (ao abrir a app e ao salvar as configurações). Se o horário novo já está
     * aberto, faz um ciclo na hora.
     */
    public void usarHorario(HorarioFuncionamento novo) {
        horario = novo.copia();
        marcarAbertura();
        avisarSituacao();
        agora();
    }

    /** Se uma execução pode começar agora. */
    public boolean dentroDoHorario() {
        return horario.permite(LocalDateTime.now());
    }

    /**
     * Fora do horário e rodando, marca um ciclo para quando ele abrir; nos outros casos, desmarca. O segundo a mais
     * garante que o relógio já passou da virada do segmento.
     */
    private synchronized void marcarAbertura() {
        if (abertura != null) {
            abertura.cancel(false);
            abertura = null;
        }
        if (!rodando || executor.isShutdown() || dentroDoHorario()) {
            return;
        }
        horario.proximaLiberacao(LocalDateTime.now()).ifPresent(quando -> {
            long espera = Duration.between(Instant.now(), quando.atZone(ZoneId.systemDefault()).toInstant())
                    .plusSeconds(1).toMillis();
            // O fim do ciclo chama marcarAbertura de novo: se o relógio se adiantou ao timer, marca outra vez.
            abertura = executor.schedule(this::ciclo, Math.max(0, espera), TimeUnit.MILLISECONDS);
        });
    }

    /** Faz um ciclo já, sem esperar o intervalo (ex.: uma subtarefa nova ou uma verificação aprovada). */
    public void agora() {
        if (rodando) {
            executor.execute(this::ciclo);
        }
    }

    /** Interrompe a execução em andamento e para o agendador (ao fechar a app). */
    public void encerrar() {
        rodando = false;
        agentes.todos().forEach(Agente::cancelar);
        executor.shutdownNow();
    }

    /** Últimas linhas do log do projeto nesta sessão da app. */
    public List<String> log(String projetoId) {
        Deque<String> linhas = logs.get(projetoId);
        if (linhas == null) {
            return List.of();
        }
        synchronized (linhas) {
            return List.copyOf(linhas);
        }
    }

    /** Se um agente está rodando agora. */
    public boolean executando() {
        return emExecucao != null;
    }

    public Situacao situacao() {
        if (!rodando) {
            return new Situacao(false, "Pausado · intervalo " + intervalo.toMinutes() + " min"
                    + (maximoPorIntervalo == SEM_LIMITE ? "" : " · até " + maximoPorIntervalo));
        }
        String atual = emExecucao;
        if (atual != null) {
            return new Situacao(true, "Rodando · " + atual);
        }
        LocalDateTime agora = LocalDateTime.now();
        if (!horario.permite(agora)) {
            return new Situacao(true, "Rodando · fora do horário" + horario.proximaLiberacao(agora)
                    .map(quando -> " · volta " + quando(agora, quando))
                    .orElse(" · nenhum horário liberado"));
        }
        Instant proximo = proximoCiclo;
        String bloqueio = noLimiteDeUso;
        String motivo = limiteAtingido() ? "limite de " + maximoPorIntervalo + " por intervalo"
                : bloqueio != null ? bloqueio : "fila vazia";
        return new Situacao(true, "Rodando · " + motivo + (proximo == null ? ""
                : " · próximo ciclo às " + LocalTime.ofInstant(proximo, ZoneId.systemDefault()).format(HORA)));
    }

    /**
     * Segura a Task recém-criada até o próximo ciclo do intervalo, ou o seguinte se o próximo está a menos de meio
     * intervalo. Com o agendador parado não segura: ela entra quando ele for ligado.
     */
    private void reter(Task task) {
        if (!rodando) {
            return;
        }
        // Lê o contador antes do horário: no meio da virada de um ciclo, erra para o lado de esperar mais.
        long atual = ciclosDoIntervalo;
        Instant proximo = proximoCiclo;
        boolean imediato = proximo != null
                && Duration.between(Instant.now(), proximo).compareTo(intervalo.dividedBy(2)) < 0;
        retidas.put(task.id(), atual + (imediato ? 2 : 1));
    }

    private boolean liberada(Task task) {
        Long aPartirDe = retidas.get(task.id());
        if (aPartirDe == null) {
            return true;
        }
        if (aPartirDe <= ciclosDoIntervalo) {
            retidas.remove(task.id());
            return true;
        }
        return false;
    }

    /**
     * Ciclo do intervalo: zera o contador de execuções, libera as Tasks retidas que chegaram a vez e marca quando
     * será o próximo.
     */
    private void cicloDoIntervalo() {
        ciclosDoIntervalo++;
        execucoesNoIntervalo = 0;
        proximoCiclo = null;
        try {
            ciclo();
        } finally {
            proximoCiclo = Instant.now().plus(intervalo);
            avisarSituacao();
        }
    }

    /** Executa o trabalho liberado até a fila esvaziar, o máximo do intervalo ou o agendador ser pausado. */
    void ciclo() {
        try {
            noLimiteDeUso = null;
            while (rodando && dentroDoHorario() && !limiteAtingido()) {
                Optional<ProjetoService.Proxima> proxima =
                        projetos.proxima(task -> liberada(task) && dentroDoLimiteDeUso(task));
                if (proxima.isEmpty()) {
                    // Sobrou trabalho só em provedores no limite: a barra superior diz qual.
                    noLimiteDeUso = projetos.proxima(this::liberada)
                            .flatMap(p -> agentes.para(p.task().provedor()))
                            .map(a -> a.provedor() + " no limite de uso (" + usoDe(a.provedor()) + "%)")
                            .orElse(null);
                    break;
                }
                executar(proxima.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            // Um erro inesperado (ex.: disco cheio) não pode virar um laço: espera o próximo ciclo.
            System.err.println("Agendador: " + e);
        } finally {
            emExecucao = null;
            // Fila vazia: a próxima execução, mesmo que da mesma Task, conta como um começo novo.
            ultimaTaskIniciada = null;
            // Parou por ter saído do horário (ou acabou de entrar nele): acorda na próxima abertura.
            marcarAbertura();
            avisarSituacao();
        }
    }

    private void executar(ProjetoService.Proxima proxima) throws InterruptedException {
        Projeto projeto = proxima.projeto();
        Task task = proxima.task();
        // Sem subtarefa, a Task roda inteira.
        Subtarefa subtarefa = proxima.subtarefa();
        String subtarefaId = subtarefa == null ? null : subtarefa.id();
        String nome = subtarefa == null ? task.titulo() : task.titulo() + " › " + subtarefa.titulo();
        String projetoId = projeto.id();
        String taskId = task.id();

        Optional<Agente> agente = agentes.para(task.provedor());
        if (agente.isEmpty()) {
            String motivo = task.provedor() == null || task.provedor() == Provedor.AUTOMATICO
                    ? "Nenhum agente disponível: instale o Claude Code."
                    : task.provedor() + " ainda não é suportado ou não está instalado.";
            registrar(projetoId, taskId, hora() + " " + nome + " · " + motivo);
            projetos.marcar(projetoId, taskId, subtarefaId, Estado.ERRO, motivo);
            return;
        }
        if (projetos.marcar(projetoId, taskId, subtarefaId, Estado.EM_ANDAMENTO, null).isEmpty()) {
            return;
        }
        // Só conta o que chegou a ir para um agente; a falta de agente acima não gasta o intervalo.
        execucoesNoIntervalo++;
        emExecucao = agente.get().provedor() + " em " + (subtarefa == null ? task.titulo() : subtarefa.titulo());
        avisarSituacao();
        if (!taskId.equals(ultimaTaskIniciada)) {
            ultimaTaskIniciada = taskId;
            ouvintes.forEach(o -> o.taskIniciada(projetoId, taskId));
        }
        registrar(projetoId, taskId, hora() + " " + nome + " · " + agente.get().provedor());

        ResultadoExecucao resultado;
        boolean sessaoNova = task.sessao() == null;
        Direcionamento respondido = subtarefa == null ? task.direcionamento() : subtarefa.direcionamento();
        // Com a sessão em que o agente perguntou, basta mandar as respostas; sem ela, vai o pedido completo com elas.
        boolean continuar = respondido != null && respondido.respondido() && !sessaoNova;
        try {
            Path pasta = areaDeTrabalho.preparar(projeto, task);
            String sessao = sessaoNova ? UUID.randomUUID().toString() : task.sessao();
            String branch = AreaDeTrabalho.branch(task);
            String prompt = continuar ? Prompt.continuar(projeto, task, subtarefa, respondido, branch, prompts)
                    : Prompt.montar(projeto, task, subtarefa, branch, prompts);
            Execucao execucao = new Execucao(pasta, prompt,
                    sessao, sessaoNova, subtarefa == null ? task.esforco() : subtarefa.esforcoOu(task.esforco()),
                    subtarefa == null ? Modelo.PADRAO : subtarefa.modelo(), projeto.permissoes(), projeto.usaGit(),
                    subtarefa != null && subtarefa.push(), LIMITE_DA_EXECUCAO);
            resultado = agente.get().executar(execucao, linha -> registrar(projetoId, taskId, "  " + linha));
        } catch (IOException | RuntimeException e) {
            resultado = ResultadoExecucao.falha(e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            emExecucao = null;
        }

        if (resultado.sessao() != null && !resultado.sessao().equals(task.sessao())) {
            projetos.definirSessao(projetoId, taskId, resultado.sessao());
        } else if (!resultado.sucesso() && !sessaoNova && resultado.sessao() == null) {
            // A sessão antiga não abriu (apagada ou de outra máquina): a próxima tentativa começa uma nova.
            projetos.definirSessao(projetoId, taskId, null);
        }

        boolean sucesso = resultado.sucesso() && !Prompt.falhou(resultado.resposta());
        Optional<Prompt.Pedido> pedido = sucesso ? Prompt.pedido(resultado.resposta()) : Optional.empty();
        boolean verificar = subtarefa != null && subtarefa.requerVerificacao();
        Estado estado = !sucesso ? Estado.ERRO : pedido.isPresent() || verificar ? Estado.ATENCAO : Estado.CONCLUIDO;
        registrar(projetoId, taskId, hora() + " " + (pedido.isPresent() ? "Aguardando direcionamento"
                : switch (estado) {
            case CONCLUIDO -> "Concluída";
            case ATENCAO -> "Aguardando verificação";
            default -> "Falhou: " + primeiraLinha(resultado.resposta());
        }));
        if (resultado.sucesso()) {
            // O agente rodou e leu as respostas que havia: fica só o pedido novo, se ele fez um.
            Direcionamento novo = pedido.map(Prompt.Pedido::direcionamento).orElse(null);
            String texto = novo == null ? resultado.resposta() : novo.resumo();
            projetos.marcar(projetoId, taskId, subtarefaId, estado, texto, novo);
        } else {
            // A execução nem terminou (tempo esgotado, CLI fora do ar): as respostas ficam para a próxima tentativa.
            projetos.marcar(projetoId, taskId, subtarefaId, estado, resultado.resposta());
        }
    }

    private void registrar(String projetoId, String taskId, String linha) {
        Deque<String> linhas = logs.computeIfAbsent(projetoId, id -> new ArrayDeque<>());
        synchronized (linhas) {
            linhas.addLast(linha);
            while (linhas.size() > LINHAS_EM_MEMORIA) {
                linhas.removeFirst();
            }
        }
        try {
            projetos.registrarLog(projetoId, taskId, linha);
        } catch (RuntimeException e) {
            System.err.println("Agendador: log não gravado: " + e.getMessage());
        }
        ouvintes.forEach(o -> o.log(projetoId, linha));
    }

    private void avisarSituacao() {
        Situacao situacao = situacao();
        ouvintes.forEach(o -> o.situacao(situacao));
    }

    private static String hora() {
        return LocalTime.now().format(HORA);
    }

    /** "às 22:00", "amanhã às 07:00" ou "sáb às 00:00". */
    private static String quando(LocalDateTime agora, LocalDateTime quando) {
        long dias = ChronoUnit.DAYS.between(agora.toLocalDate(), quando.toLocalDate());
        String dia = dias == 0 ? "" : dias == 1 ? "amanhã "
                : quando.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.of("pt", "BR")).replace(".", "") + " ";
        return dia + "às " + quando.format(HORA);
    }

    private static String primeiraLinha(String texto) {
        return texto == null ? "" : texto.strip().lines().findFirst().orElse("");
    }
}
