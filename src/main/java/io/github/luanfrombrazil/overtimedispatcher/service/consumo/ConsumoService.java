package io.github.luanfrombrazil.overtimedispatcher.service.consumo;

import io.github.luanfrombrazil.overtimedispatcher.model.Consumo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Mede o uso de cada provedor a cada {@link #INTERVALO} (e logo depois de {@link #atualizar()}) e avisa quem
 * mostra as barras de consumo.
 */
public class ConsumoService {

    public static final Duration INTERVALO = Duration.ofMinutes(2);

    private final List<Medidor> medidores;
    private final List<Consumer<List<Consumo>>> ouvintes = new CopyOnWriteArrayList<>();
    private final AtomicBoolean pendente = new AtomicBoolean();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(tarefa -> {
        Thread thread = new Thread(tarefa, "consumo");
        thread.setDaemon(true);
        return thread;
    });
    private volatile List<Consumo> atual;

    public ConsumoService(List<Medidor> medidores) {
        this.medidores = List.copyOf(medidores);
        this.atual = medidores.stream().map(m -> Consumo.desconhecido(m.provedor(), "Medindo…")).toList();
    }

    /** Claude, Codex e Gemini (este ainda sem leitura). */
    public static ConsumoService padrao() {
        return new ConsumoService(List.of(new MedidorClaude(), new MedidorCodex(),
                Medidor.semLeitura("Gemini", "O uso do Gemini ainda não é medido.")));
    }

    /** Avisado na thread do consumo; quem atualiza a tela deve passar para a thread do Swing. */
    public void aoMudar(Consumer<List<Consumo>> ouvinte) {
        ouvintes.add(ouvinte);
    }

    /** Última medição (antes da primeira, todos os provedores aparecem sem porcentagem). */
    public List<Consumo> consumo() {
        return atual;
    }

    public void iniciar() {
        executor.scheduleWithFixedDelay(this::medir, 0, INTERVALO.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Mede de novo já (ex.: um agente acabou de rodar). Pedidos seguidos viram uma medição só. */
    public void atualizar() {
        if (pendente.compareAndSet(false, true)) {
            executor.execute(this::medir);
        }
    }

    public void encerrar() {
        executor.shutdownNow();
    }

    void medir() {
        pendente.set(false);
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
