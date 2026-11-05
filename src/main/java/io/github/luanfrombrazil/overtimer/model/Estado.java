package io.github.luanfrombrazil.overtimer.model;

import java.util.List;

/**
 * Estado de um item, mostrado na tela pela cor da borda.
 */
public enum Estado {
    CONCLUIDO,
    EM_ANDAMENTO,
    ATENCAO,
    ERRO,
    PENDENTE,
    ORACULO,
    /** Rascunho guardado ao fechar o pop-up clicando fora; não vai para a fila até ser criado de fato. */
    RASCUNHO;

    /**
     * Se um item (subtarefa ou Task executada inteira) espera a sua resposta a perguntas do agente. Amarelo como a
     * verificação, mas é outra espera: a verificação aprova ou manda refazer; o direcionamento responde e retoma.
     */
    public static boolean aguardaDirecionamento(Estado estado, Direcionamento direcionamento) {
        return estado == ATENCAO && direcionamento != null && !direcionamento.respondido();
    }

    /**
     * Estado de um conjunto a partir dos estados das partes: falha e pedido de atenção têm prioridade,
     * depois o que está em andamento; só fica concluído quando todas as partes estão concluídas.
     */
    public static Estado resumir(List<Estado> partes) {
        for (Estado prioritario : List.of(ERRO, ATENCAO, EM_ANDAMENTO)) {
            if (partes.contains(prioritario)) {
                return prioritario;
            }
        }
        if (!partes.isEmpty() && partes.stream().allMatch(e -> e == CONCLUIDO)) {
            return CONCLUIDO;
        }
        return PENDENTE;
    }
}
