package io.github.luanfrombrazil.overtimer.service.scheduler;

import io.github.luanfrombrazil.overtimer.model.Estado;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Entrega o trabalho da fila aos agentes, um de cada vez: subtarefas ou, quando não há subtarefas, a Task inteira.
 * <p>
 * Enquanto está rodando, faz um ciclo a cada {@link #INTERVALO} (e também logo depois de {@link #agora()}):
 * pega o próximo trabalho liberado, executa no agente da Task e repete até a fila esvaziar. Pausar não
 * interrompe a execução em andamento; só impede que a próxima comece.
 * <p>
 * Uma Task criada com o agendador rodando só é executada a partir do próximo ciclo do intervalo (os de
 * {@link #agora()} não contam), para dar tempo de criar as subtarefas dela. Se esse ciclo está a menos de meio
 * intervalo, ela espera um ciclo a mais.
 * <p>
 * Ainda não respeita o horário de funcionamento nem a reserva (módulo de consumo).
 */
public class Agendador {

    public static final Duration INTERVALO = Duration.ofMinutes(5);
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
        ciclos = executor.scheduleWithFixedDelay(this::cicloDoIntervalo, 0, INTERVALO.toMillis(),
                TimeUnit.MILLISECONDS);
        avisarSituacao();
    }

    public synchronized void pausar() {
        rodando = false;
        if (ciclos != null) {
            ciclos.cancel(false);
        }
        avisarSituacao();
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
            return new Situacao(false, "Pausado · intervalo " + INTERVALO.toMinutes() + " min");
        }
        String atual = emExecucao;
        if (atual != null) {
            return new Situacao(true, "Rodando · " + atual);
        }
        Instant proximo = proximoCiclo;
        return new Situacao(true, "Rodando · fila vazia" + (proximo == null ? ""
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
                && Duration.between(Instant.now(), proximo).compareTo(INTERVALO.dividedBy(2)) < 0;
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

    /** Ciclo do intervalo: libera as Tasks retidas que chegaram a vez e marca quando será o próximo. */
    private void cicloDoIntervalo() {
        ciclosDoIntervalo++;
        proximoCiclo = null;
        try {
            ciclo();
        } finally {
            proximoCiclo = Instant.now().plus(INTERVALO);
            avisarSituacao();
        }
    }

    /** Executa o trabalho liberado até a fila esvaziar ou o agendador ser pausado. */
    void ciclo() {
        try {
            while (rodando) {
                Optional<ProjetoService.Proxima> proxima = projetos.proxima(this::liberada);
                if (proxima.isEmpty()) {
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
        emExecucao = agente.get().provedor() + " em " + (subtarefa == null ? task.titulo() : subtarefa.titulo());
        avisarSituacao();
        if (!taskId.equals(ultimaTaskIniciada)) {
            ultimaTaskIniciada = taskId;
            ouvintes.forEach(o -> o.taskIniciada(projetoId, taskId));
        }
        registrar(projetoId, taskId, hora() + " " + nome + " · " + agente.get().provedor());

        ResultadoExecucao resultado;
        boolean sessaoNova = task.sessao() == null;
        try {
            Path pasta = areaDeTrabalho.preparar(projeto, task);
            String sessao = sessaoNova ? UUID.randomUUID().toString() : task.sessao();
            Execucao execucao = new Execucao(pasta, Prompt.montar(projeto, task, subtarefa, AreaDeTrabalho.branch(task)),
                    sessao, sessaoNova, task.esforco(), projeto.permissoes(), projeto.usaGit(), LIMITE_DA_EXECUCAO);
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
        boolean verificar = subtarefa != null && subtarefa.requerVerificacao();
        Estado estado = !sucesso ? Estado.ERRO : verificar ? Estado.ATENCAO : Estado.CONCLUIDO;
        registrar(projetoId, taskId, hora() + " " + switch (estado) {
            case CONCLUIDO -> "Concluída";
            case ATENCAO -> "Aguardando verificação";
            default -> "Falhou: " + primeiraLinha(resultado.resposta());
        });
        projetos.marcar(projetoId, taskId, subtarefaId, estado, resultado.resposta());
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

    private static String primeiraLinha(String texto) {
        return texto == null ? "" : texto.strip().lines().findFirst().orElse("");
    }
}
