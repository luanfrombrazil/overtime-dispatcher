package io.github.luanfrombrazil.overtimer.service.consumo;

import io.github.luanfrombrazil.overtimer.model.Consumo;
import io.github.luanfrombrazil.overtimer.model.Provedor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Mede o uso de cada provedor e avisa quem mostra as barras de consumo.
 * <p>
 * Mede só em três momentos, para não estourar o limite de consultas do provedor: ao abrir o programa
 * ({@link #iniciar()}), quando uma Task começa ({@link #atualizar()}) e quando o programa fica
 * {@link #OCIOSO} sem nenhum agente rodando nem nenhuma medição.
 */
public class ConsumoService {

    /** Tempo sem Tasks e sem medições depois do qual o uso é medido de novo. */
    public static final Duration OCIOSO = Duration.ofMinutes(30);
    /** De quanto em quanto tempo verifica se o programa está ocioso (não mede, só olha o relógio). */
    static final Duration VERIFICACAO = Duration.ofMinutes(1);

    private final List<Medidor> medidores;
    private final BooleanSupplier ocupado;
    private final Clock relogio;
    private final List<Consumer<List<Consumo>>> ouvintes = new CopyOnWriteArrayList<>();
    private final AtomicBoolean pendente = new AtomicBoolean();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(tarefa -> {
        Thread thread = new Thread(tarefa, "consumo");
        thread.setDaemon(true);
        return thread;
    });
    private volatile List<Consumo> atual;
    /** Última medição ou último momento em que um agente estava rodando, o que for mais recente. */
    private volatile Instant ultimaAtividade;

    /** @param ocupado se um agente está rodando agora; enquanto estiver, o programa não conta como ocioso */
    public ConsumoService(List<Medidor> medidores, BooleanSupplier ocupado, Clock relogio) {
        this.medidores = List.copyOf(medidores);
        this.ocupado = ocupado;
        this.relogio = relogio;
        this.atual = medidores.stream().map(m -> Consumo.desconhecido(m.provedor(), "Medindo…")).toList();
        this.ultimaAtividade = relogio.instant();
    }

    /** Claude (Claude Code), Codex e Gemini (Antigravity). */
    public static ConsumoService padrao(BooleanSupplier ocupado) {
        return new ConsumoService(List.of(new MedidorClaude(), new MedidorCodex(), new MedidorAntigravity()),
                ocupado, Clock.systemDefaultZone());
    }

    /** Avisado na thread do consumo; quem atualiza a tela deve passar para a thread do Swing. */
    public void aoMudar(Consumer<List<Consumo>> ouvinte) {
        ouvintes.add(ouvinte);
    }

    /** Última medição (antes da primeira, todos os provedores aparecem sem porcentagem). */
    public List<Consumo> consumo() {
        return atual;
    }

    /**
     * Maior uso medido do provedor (sessão ou semanal), de 0 a 100, ou {@link Consumo#DESCONHECIDO} se ele não é
     * medido ou a última medição falhou.
     */
    public int uso(Provedor provedor) {
        String nome = nomeDoMedidor(provedor);
        return atual.stream()
                .filter(c -> c.provedor().equals(nome))
                .mapToInt(c -> Math.max(c.sessao(), c.semanal()))
                .findFirst()
                .orElse(Consumo.DESCONHECIDO);
    }

    /** Nome com que o medidor do provedor aparece na barra superior. */
    static String nomeDoMedidor(Provedor provedor) {
        return switch (provedor) {
            case CLAUDE_CODE -> "Claude";
            case CODEX -> "Codex";
            case GEMINI -> "Gemini";
            case AUTOMATICO -> "";
        };
    }

    /** Mede já (abertura do programa) e passa a medir de novo quando ficar ocioso. */
    public void iniciar() {
        atualizar();
        executor.scheduleWithFixedDelay(this::verificarOciosidade, VERIFICACAO.toMillis(), VERIFICACAO.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    /** Mede de novo já (ex.: uma Task começou). Pedidos seguidos viram uma medição só. */
    public void atualizar() {
        if (pendente.compareAndSet(false, true)) {
            executor.execute(this::medir);
        }
    }

    public void encerrar() {
        executor.shutdownNow();
    }

    /** Mede se já se passou {@link #OCIOSO} desde a última atividade sem nenhum agente rodando. */
    void verificarOciosidade() {
        Instant agora = relogio.instant();
        if (ocupado.getAsBoolean()) {
            ultimaAtividade = agora;
            return;
        }
        if (Duration.between(ultimaAtividade, agora).compareTo(OCIOSO) >= 0) {
            medir();
        }
    }

    void medir() {
        pendente.set(false);
        ultimaAtividade = relogio.instant();
        List<Consumo> medido = new ArrayList<>();
        for (Medidor medidor : medidores) {
            try {
                medido.add(medidor.medir());
            } catch (RuntimeException e) {
                medido.add(Consumo.desconhecido(medidor.provedor(), "Erro ao medir: " + e.getMessage()));
            }
        }
        atual = List.copyOf(medido);
        ouvintes.forEach(o -> o.accept(atual));
    }
}
