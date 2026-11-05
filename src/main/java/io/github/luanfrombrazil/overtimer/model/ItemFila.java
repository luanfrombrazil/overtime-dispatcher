package io.github.luanfrombrazil.overtimer.model;

import java.util.UUID;

/**
 * Task injetada num protocolo por outro protocolo ou projeto, direto na fila de entrada ou como proposta.
 *
 * @param origem id do protocolo ou projeto que injetou a task
 */
public record ItemFila(String id, String titulo, String descricao, String origem) {

    public static ItemFila novo(String titulo, String descricao, String origem) {
        return new ItemFila(UUID.randomUUID().toString(), titulo, descricao, origem);
    }
}
