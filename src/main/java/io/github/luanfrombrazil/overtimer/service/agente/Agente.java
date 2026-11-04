package io.github.luanfrombrazil.overtimer.service.agente;

import io.github.luanfrombrazil.overtimer.model.Provedor;

import java.util.function.Consumer;

/**
 * Um provedor de IA executado pelo seu CLI. O resto da app só conhece esta interface.
 */
public interface Agente {

    Provedor provedor();

    /** Se o CLI do provedor foi encontrado neste computador. */
    boolean disponivel();

    /**
     * Executa o pedido e espera o agente terminar.
     *
     * @param log recebe, enquanto o agente trabalha, linhas curtas do que ele está fazendo
     */
    ResultadoExecucao executar(Execucao execucao, Consumer<String> log) throws InterruptedException;

    /** Interrompe a execução em andamento, se houver. */
    void cancelar();
}
