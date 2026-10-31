package io.github.luanfrombrazil.overtimedispatcher.model;

import java.util.List;

/**
 * Rotina contínua que o agente segue de tempos em tempos.
 */
public record Protocolo(String nome, String sigla, String detalhe, Estado estado,
                        String horarios, String passos, String provedor, String esforco,
                        List<String> acessos, List<String> aceitaDiretoDe,
                        List<ItemFila> filaDeEntrada, List<ItemFila> propostas,
                        List<String> ultimaExecucao) {
}
