package io.github.luanfrombrazil.overtimer.model;

import java.time.LocalDateTime;

/**
 * Aviso que a app mostra no sino e, conforme as configurações, no desktop e por som.
 *
 * @param projetoId projeto a que o aviso se refere; null se não é de um projeto (ex.: limite de uso)
 * @param taskId    Task a que o aviso se refere; null se não é de uma Task
 */
public record Notificacao(String id, Configuracoes.Aviso tipo, String titulo, String texto, String projetoId,
                          String taskId, LocalDateTime instante, boolean lida) {

    /** A mesma notificação, já lida. */
    public Notificacao comoLida() {
        return lida ? this : new Notificacao(id, tipo, titulo, texto, projetoId, taskId, instante, true);
    }
}
