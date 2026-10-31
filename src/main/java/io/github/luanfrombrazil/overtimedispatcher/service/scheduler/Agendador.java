package io.github.luanfrombrazil.overtimedispatcher.service.scheduler;

import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Provedor;
import io.github.luanfrombrazil.overtimedispatcher.model.Subtarefa;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import io.github.luanfrombrazil.overtimedispatcher.service.ProjetoService;
import io.github.luanfrombrazil.overtimedispatcher.service.agente.Agente;
import io.github.luanfrombrazil.overtimedispatcher.service.agente.Agentes;
import io.github.luanfrombrazil.overtimedispatcher.service.agente.AreaDeTrabalho;
import io.github.luanfrombrazil.overtimedispatcher.service.agente.Execucao;
import io.github.luanfrombrazil.overtimedispatcher.service.agente.ResultadoExecucao;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
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
 * Entrega as subtarefas da fila aos agentes, uma de cada vez.
 * <p>
 * Enquanto está rodando, faz um ciclo a cada {@link #INTERVALO} (e também logo depois de {@link #agora()}):
 * pega a próxima subtarefa liberada, executa no agente da Task e repete até a fila esvaziar. Pausar não
 * interrompe a execução em andamento; só impede que a próxima comece.
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
    private volatile LocalTime proximoCiclo;
    private ScheduledFuture<?> ciclos;

    public Agendador(ProjetoService projetos, Agentes agentes, AreaDeTrabalho areaDeTrabalho) {
        this.projetos = projetos;
        this.agentes = agentes;
        this.areaDeTrabalho = areaDeTrabalho;
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
        ciclos = executor.scheduleWithFixedDelay(this::ciclo, 0, INTERVALO.toMillis(), TimeUnit.MILLISECONDS);
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

    public Situacao situacao() {
        if (!rodando) {
            return new Situacao(false, "Pausado · intervalo " + INTERVALO.toMinutes() + " min");
        }
        String atual = emExecucao;
        if (atual != null) {
            return new Situacao(true, "Rodando · " + atual);
        }
        LocalTime proximo = proximoCiclo;
        return new Situacao(true, "Rodando · fila vazia" + (proximo == null ? "" : " · próximo ciclo às " + proximo.format(HORA)));
    }

    /** Executa as subtarefas liberadas até a fila esvaziar ou o agendador ser pausado. */
    void ciclo() {
        try {
            while (rodando) {
                Optional<ProjetoService.Proxima> proxima = projetos.proximaSubtarefa();
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
            proximoCiclo = LocalTime.now().plus(INTERVALO);
            avisarSituacao();
        }
    }

    private void executar(ProjetoService.Proxima proxima) throws InterruptedException {
        Projeto projeto = proxima.projeto();
        Task task = proxima.task();
        Subtarefa subtarefa = proxima.subtarefa();
        String projetoId = projeto.id();
        String taskId = task.id();

        Optional<Agente> agente = agentes.para(task.provedor());
        if (agente.isEmpty()) {
            String motivo = task.provedor() == null || task.provedor() == Provedor.AUTOMATICO
                    ? "Nenhum agente disponível: instale o Claude Code."
                    : task.provedor() + " ainda não é suportado ou não está instalado.";
            registrar(projetoId, taskId, hora() + " " + task.titulo() + " › " + subtarefa.titulo() + " · " + motivo);
            projetos.marcar(projetoId, taskId, subtarefa.id(), Estado.ERRO, motivo);
            return;
        }
        if (projetos.marcar(projetoId, taskId, subtarefa.id(), Estado.EM_ANDAMENTO, null).isEmpty()) {
            return;
        }
        emExecucao = agente.get().provedor() + " em " + subtarefa.titulo();
        avisarSituacao();
        registrar(projetoId, taskId,
                hora() + " " + task.titulo() + " › " + subtarefa.titulo() + " · " + agente.get().provedor());

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
        Estado estado = !sucesso ? Estado.ERRO : subtarefa.requerVerificacao() ? Estado.ATENCAO : Estado.CONCLUIDO;
        registrar(projetoId, taskId, hora() + " " + switch (estado) {
            case CONCLUIDO -> "Concluída";
            case ATENCAO -> "Aguardando verificação";
            default -> "Falhou: " + primeiraLinha(resultado.resposta());
        });
        projetos.marcar(projetoId, taskId, subtarefa.id(), estado, resultado.resposta());
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
