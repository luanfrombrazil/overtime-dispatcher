package io.github.luanfrombrazil.overtimer.service.scheduler;

import io.github.luanfrombrazil.overtimer.model.Configuracoes;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Aviso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.LimiteDeUso;
import io.github.luanfrombrazil.overtimer.model.Direcionamento;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.ExecucaoProtocolo;
import io.github.luanfrombrazil.overtimer.model.HorarioFuncionamento;
import io.github.luanfrombrazil.overtimer.model.ItemFila;
import io.github.luanfrombrazil.overtimer.model.Modelo;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.ProjetoService;
import io.github.luanfrombrazil.overtimer.service.ProtocoloService;
import io.github.luanfrombrazil.overtimer.service.agente.Agente;
import io.github.luanfrombrazil.overtimer.service.agente.Agentes;
import io.github.luanfrombrazil.overtimer.service.agente.AreaDeTrabalho;
import io.github.luanfrombrazil.overtimer.service.agente.Execucao;
import io.github.luanfrombrazil.overtimer.service.agente.ResultadoExecucao;
import io.github.luanfrombrazil.overtimer.service.comunicacao.NotificacaoService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

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
 * <p>
 * Os protocolos ligados entram na mesma fila, um de cada vez com as Tasks: quando a agenda de um chega, ele passa
 * na frente da próxima Task, com as mesmas regras de horário de funcionamento, limite de uso e máximo por
 * intervalo. Além dos ciclos do intervalo, o agendador acorda sozinho no próximo horário de protocolo. Um horário
 * que passou com o agendador ocupado ou fora do horário de funcionamento roda uma vez, assim que der.
 */
public class Agendador {

    public static final Duration INTERVALO_PADRAO = Duration.ofMinutes(5);
    /** Valor de {@link #usarIntervalo} para não limitar as execuções por intervalo. */
    public static final int SEM_LIMITE = 0;
    private static final LimiteDeUso SEM_LIMITE_DE_USO = new LimiteDeUso(100, 0);
    public static final Duration LIMITE_DA_EXECUCAO = Duration.ofMinutes(60);
    private static final int LINHAS_EM_MEMORIA = 300;
    /** Linhas de log guardadas por execução de protocolo no histórico. */
    private static final int LINHAS_NO_HISTORICO = 500;
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
    /** Protocolos a rodar pela agenda; null quando o agendador só cuida das Tasks. */
    private final ProtocoloService protocolos;
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
    /** Onde registrar os avisos do fim das execuções; null para não avisar. */
    private volatile NotificacaoService notificacoes;
    /** Provedores no limite de uso que já foram avisados; saem daqui quando o uso baixa. */
    private final Set<Provedor> avisadosNoLimite = ConcurrentHashMap.newKeySet();

    /**
     * Início da conta da agenda de cada protocolo, pelo id: a última execução (a do histórico, ao abrir a app) ou, se
     * ele nunca rodou, quando o agendador o viu ligado pela primeira vez. Desligado, acompanha o relógio, para não rodar o que
     * passou enquanto estava desligado.
     */
    private final Map<String, LocalDateTime> contaDaAgenda = new ConcurrentHashMap<>();
    /** Protocolos pedidos pelo "Rodar agora", pelo id, que rodam no próximo ciclo mesmo fora da agenda. */
    private final Set<String> pedidos = ConcurrentHashMap.newKeySet();
    /** Ciclo marcado para o próximo horário de protocolo; null se não há nenhum. */
    private ScheduledFuture<?> proximoProtocolo;
    /** Relógio da agenda dos protocolos; trocado nos testes. */
    private volatile Clock relogio = Clock.systemDefaultZone();

    public Agendador(ProjetoService projetos, Agentes agentes, AreaDeTrabalho areaDeTrabalho) {
        this(projetos, null, agentes, areaDeTrabalho);
    }

    public Agendador(ProjetoService projetos, ProtocoloService protocolos, Agentes agentes,
                     AreaDeTrabalho areaDeTrabalho) {
        this.projetos = projetos;
        this.protocolos = protocolos;
        this.agentes = agentes;
        this.areaDeTrabalho = areaDeTrabalho;
        projetos.aoCriarTask(this::reter);
        if (protocolos != null) {
            // Agenda trocada, protocolo ligado ou criado: o próximo horário pode ter mudado.
            protocolos.aoMudar(p -> marcarProtocolo());
        }
    }

    void usarRelogio(Clock relogio) {
        this.relogio = relogio;
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
     * Aplica o que das configurações vale para o agendador: horário de funcionamento, intervalo, máximo, os prompts
     * das próximas execuções e os avisos que ele registra.
     */
    public void aplicar(Configuracoes configuracoes) {
        limitesDeUso = configuracoes.limitesDeUso();
        prompts = configuracoes.prompts();
        NotificacaoService servico = notificacoes;
        if (servico != null) {
            servico.aplicar(configuracoes.notificacoes());
        }
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
     * Onde registrar os avisos do fim de cada execução: verificação, direcionamento e falha. Sem isso, não avisa.
     */
    public void usarNotificacoes(NotificacaoService notificacoes) {
        this.notificacoes = notificacoes;
    }

    /**
     * Se a Task pode começar pelo limite de uso do provedor que vai executá-la. Sem agente, deixa passar: a
     * execução registra o erro de falta de agente.
     */
    private boolean dentroDoLimiteDeUso(Task task) {
        return dentroDoLimiteDeUso(task.provedor());
    }

    private boolean dentroDoLimiteDeUso(Provedor pedido) {
        Optional<Agente> agente = agentes.para(pedido);
        if (agente.isEmpty()) {
            return true;
        }
        Provedor provedor = agente.get().provedor();
        return limiteDeUso(provedor).permite(usoDe(provedor));
    }

    private LimiteDeUso limiteDeUso(Provedor provedor) {
        return limitesDeUso.getOrDefault(provedor, SEM_LIMITE_DE_USO);
    }

    /** Avisa que o provedor chegou ao limite de uso, uma vez só até ele sair do limite. */
    private void avisarLimiteDeUso(Provedor provedor) {
        if (!avisadosNoLimite.add(provedor)) {
            return;
        }
        NotificacaoService servico = notificacoes;
        if (servico == null) {
            return;
        }
        try {
            servico.registrar(Aviso.LIMITE_DE_USO, provedor + " no limite de uso",
                    "Uso em " + usoDe(provedor) + "%, limite da automação em " + limiteDeUso(provedor).automacao()
                            + "%. As Tasks dele esperam o uso baixar.", null, null);
        } catch (RuntimeException e) {
            System.err.println("Agendador: notificação não registrada: " + e);
        }
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
        marcarProtocolo();
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

    /**
     * Rodando, marca um ciclo para o próximo horário de protocolo ainda por vir; nos outros casos, desmarca. Um
     * horário que já passou (ex.: provedor no limite de uso) fica para os ciclos do intervalo, para não virar um laço.
     */
    private synchronized void marcarProtocolo() {
        if (proximoProtocolo != null) {
            proximoProtocolo.cancel(false);
            proximoProtocolo = null;
        }
        if (!rodando || protocolos == null || executor.isShutdown()) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        protocolos.protocolos().stream()
                .filter(Protocolo::ativo)
                .map(p -> p.agenda().proxima(contaDaAgenda.getOrDefault(p.id(), agora)))
                .flatMap(Optional::stream)
                .filter(quando -> quando.isAfter(agora))
                .min(LocalDateTime::compareTo)
                .ifPresent(quando -> proximoProtocolo = executor.schedule(this::ciclo,
                        Duration.between(agora, quando).plusSeconds(1).toMillis(), TimeUnit.MILLISECONDS));
    }

    /**
     * Começa a contar a agenda dos protocolos vistos pela primeira vez e faz a dos desligados acompanhar o relógio.
     * Roda em todo ciclo, mesmo fora do horário de funcionamento, para o horário que passar com ele fechado ainda
     * rodar quando abrir.
     */
    private void acompanharAgendas() {
        if (protocolos == null) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        for (Protocolo protocolo : protocolos.protocolos()) {
            if (protocolo.ativo()) {
                // Ao abrir a app, conta da última execução salva: o horário que passou com ela fechada roda uma vez.
                contaDaAgenda.computeIfAbsent(protocolo.id(), id -> protocolos.ultimaExecucao(id)
                        .map(ExecucaoProtocolo::inicioEm).orElse(agora));
            } else {
                contaDaAgenda.put(protocolo.id(), agora);
            }
        }
    }

    /**
     * Pede para o protocolo rodar no próximo ciclo, mesmo fora da agenda ou desligado, e faz esse ciclo já. Valem o
     * horário de funcionamento, o limite de uso e o máximo por intervalo; com o agendador pausado, o pedido espera
     * ele ser ligado.
     */
    public void rodarAgora(String protocoloId) {
        pedidos.add(protocoloId);
        agora();
    }

    /**
     * O protocolo pedido pelo "Rodar agora" ou, se não há, o ligado cuja agenda já chegou; em ambos, só se o provedor
     * não estiver no limite de uso.
     */
    private Optional<Protocolo> protocoloDaVez() {
        if (protocolos == null) {
            return Optional.empty();
        }
        acompanharAgendas();
        for (String id : List.copyOf(pedidos)) {
            Optional<Protocolo> pedido = protocolos.buscar(id);
            if (pedido.isEmpty()) {
                pedidos.remove(id);
            } else if (dentroDoLimiteDeUso(pedido.get().provedor())) {
                return pedido;
            }
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        for (Protocolo protocolo : protocolos.protocolos()) {
            if (!protocolo.ativo()) {
                continue;
            }
            LocalDateTime desde = contaDaAgenda.getOrDefault(protocolo.id(), agora);
            boolean chegou = protocolo.agenda().proxima(desde).map(q -> !q.isAfter(agora)).orElse(false);
            if (chegou && dentroDoLimiteDeUso(protocolo.provedor())) {
                return Optional.of(protocolo);
            }
        }
        return Optional.empty();
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
            // Quem saiu do limite volta a poder ser avisado na próxima vez que entrar.
            avisadosNoLimite.removeIf(provedor -> limiteDeUso(provedor).permite(usoDe(provedor)));
            acompanharAgendas();
            while (rodando && dentroDoHorario() && !limiteAtingido()) {
                Optional<Protocolo> protocolo = protocoloDaVez();
                if (protocolo.isPresent()) {
                    executar(protocolo.get());
                    continue;
                }
                Optional<ProjetoService.Proxima> proxima =
                        projetos.proxima(task -> liberada(task) && dentroDoLimiteDeUso(task));
                if (proxima.isEmpty()) {
                    // Sobrou trabalho só em provedores no limite: a barra superior diz qual.
                    Optional<Provedor> bloqueado = projetos.proxima(this::liberada)
                            .flatMap(p -> agentes.para(p.task().provedor()))
                            .map(Agente::provedor);
                    noLimiteDeUso = bloqueado
                            .map(p -> p + " no limite de uso (" + usoDe(p) + "%)")
                            .orElse(null);
                    bloqueado.ifPresent(this::avisarLimiteDeUso);
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
            marcarProtocolo();
            avisarSituacao();
        }
    }

    /**
     * Roda o protocolo numa sessão nova, na pasta de trabalho dele. A agenda conta a partir do início, mesmo que a
     * execução falhe: o horário não fica sendo tentado a cada ciclo. Com sucesso, a fila de entrada enviada sai. Cada
     * execução, mesmo a que não chegou a um agente, vai para o histórico do protocolo.
     */
    private void executar(Protocolo protocolo) throws InterruptedException {
        String id = protocolo.id();
        LocalDateTime inicio = LocalDateTime.now(relogio);
        pedidos.remove(id);
        contaDaAgenda.put(id, inicio);
        // Uma Task interrompida por um protocolo conta como um começo novo quando voltar.
        ultimaTaskIniciada = null;
        List<String> log = new ArrayList<>();
        Consumer<String> anotar = linha -> {
            if (log.size() < LINHAS_NO_HISTORICO) {
                log.add(linha);
            }
            registrarProtocolo(id, linha);
        };

        Optional<Agente> agente = agentes.para(protocolo.provedor());
        if (agente.isEmpty()) {
            String motivo = protocolo.provedor() == Provedor.AUTOMATICO
                    ? "Nenhum agente disponível: instale o Claude Code."
                    : protocolo.provedor() + " ainda não é suportado ou não está instalado.";
            anotar.accept(hora() + " " + protocolo.nome() + " · " + motivo);
            guardarExecucao(id, inicio, false, motivo, log);
            return;
        }
        execucoesNoIntervalo++;
        emExecucao = agente.get().provedor() + " em " + protocolo.nome();
        avisarSituacao();
        anotar.accept(hora() + " " + protocolo.nome() + " · " + agente.get().provedor());

        ResultadoExecucao resultado;
        try {
            Path pasta = areaDeTrabalho.preparar(protocolo);
            Execucao execucao = Execucao.doProtocolo(pasta, PromptProtocolo.montar(protocolo, this::nomeDe),
                    UUID.randomUUID().toString(), true, protocolo.esforco(), protocolo.acessos(), LIMITE_DA_EXECUCAO);
            resultado = agente.get().executar(execucao, linha -> anotar.accept("  " + linha));
        } catch (IOException | RuntimeException e) {
            resultado = ResultadoExecucao.falha(e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            emExecucao = null;
        }

        boolean sucesso = resultado.sucesso() && !Prompt.falhou(resultado.resposta());
        anotar.accept(hora() + " " + (sucesso ? "Concluído" : "Falhou: " + primeiraLinha(resultado.resposta())));
        // Só os itens que foram no prompt: os que chegaram durante a execução ficam para a próxima.
        if (sucesso && !protocolo.filaDeEntrada().isEmpty()) {
            try {
                protocolos.tirarDaFila(id, protocolo.filaDeEntrada().stream().map(ItemFila::id)
                        .collect(Collectors.toSet()));
            } catch (RuntimeException e) {
                anotar.accept(hora() + " Fila de entrada não atualizada: " + e.getMessage());
            }
        }
        guardarExecucao(id, inicio, sucesso, resultado.resposta(), log);
    }

    private void guardarExecucao(String id, LocalDateTime inicio, boolean sucesso, String resultado,
                                 List<String> log) {
        try {
            protocolos.registrarExecucao(id,
                    ExecucaoProtocolo.de(inicio, LocalDateTime.now(relogio), sucesso, resultado, log));
        } catch (RuntimeException e) {
            registrarProtocolo(id, hora() + " Histórico não gravado: " + e.getMessage());
        }
    }

    /** Nome do protocolo ou projeto com o id dado, para a origem dos itens da fila; o id se não existe mais. */
    private String nomeDe(String id) {
        return protocolos.buscar(id).map(Protocolo::nome)
                .or(() -> projetos.buscar(id).map(Projeto::nome))
                .orElse(id);
    }

    /** Linha do log do protocolo nesta sessão da app, pelo id dele, como o log dos projetos. */
    private void registrarProtocolo(String protocoloId, String linha) {
        Deque<String> linhas = logs.computeIfAbsent(protocoloId, id -> new ArrayDeque<>());
        synchronized (linhas) {
            linhas.addLast(linha);
            while (linhas.size() > LINHAS_EM_MEMORIA) {
                linhas.removeFirst();
            }
        }
        ouvintes.forEach(o -> o.log(protocoloId, linha));
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
            if (projetos.marcar(projetoId, taskId, subtarefaId, Estado.ERRO, motivo).isPresent()) {
                notificar(Aviso.FALHAS, "Falhou", projeto, task, nome, motivo);
            }
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
        Optional<Projeto> marcado;
        if (resultado.sucesso()) {
            // O agente rodou e leu as respostas que havia: fica só o pedido novo, se ele fez um.
            Direcionamento novo = pedido.map(Prompt.Pedido::direcionamento).orElse(null);
            String texto = novo == null ? resultado.resposta() : novo.resumo();
            marcado = projetos.marcar(projetoId, taskId, subtarefaId, estado, texto, novo);
        } else {
            // A execução nem terminou (tempo esgotado, CLI fora do ar): as respostas ficam para a próxima tentativa.
            marcado = projetos.marcar(projetoId, taskId, subtarefaId, estado, resultado.resposta());
        }
        // Excluída no meio-tempo: não há o que avisar.
        if (marcado.isEmpty()) {
            return;
        }
        if (pedido.isPresent()) {
            // A primeira pergunta dá a ideia do que o agente precisa.
            String pergunta = pedido.get().direcionamento().perguntas().stream()
                    .map(Direcionamento.Pergunta::texto).findFirst().orElse(null);
            notificar(Aviso.DIRECIONAMENTO, "Aguardando direcionamento", projeto, task, nome, pergunta);
        } else if (estado == Estado.ATENCAO) {
            notificar(Aviso.VERIFICACAO, "Aguardando verificação", projeto, task, nome, null);
        } else if (estado == Estado.ERRO) {
            notificar(Aviso.FALHAS, "Falhou", projeto, task, nome, primeiraLinha(resultado.resposta()));
        } else if (estado == Estado.CONCLUIDO && marcado.get().tasks().stream()
                .anyMatch(t -> t.id().equals(taskId) && t.finalizada())) {
            // A última subtarefa (ou a Task inteira) concluiu: a Task está finalizada. Finalizada, ela não recebe
            // subtarefas novas, então isto acontece uma vez por Task.
            notificar(Aviso.SESSAO_CONCLUIDA, "Task concluída", projeto, task, task.titulo(), null);
        }
    }

    /** Registra a notificação do fim da execução: "Projeto · Task › subtarefa" e, se houver, o detalhe. */
    private void notificar(Aviso tipo, String titulo, Projeto projeto, Task task, String nome, String detalhe) {
        NotificacaoService servico = notificacoes;
        if (servico == null) {
            return;
        }
        String texto = projeto.nome() + " · " + nome + (detalhe == null || detalhe.isBlank() ? "" : "\n" + detalhe);
        try {
            servico.registrar(tipo, titulo, texto, projeto.id(), task.id());
        } catch (RuntimeException e) {
            // Um aviso que não saiu não pode parar a fila.
            System.err.println("Agendador: notificação não registrada: " + e);
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
