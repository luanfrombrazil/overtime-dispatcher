package io.github.luanfrombrazil.overtimer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de direcionamento do agente: as perguntas que ele deixou ao parar e, depois que você responde, as
 * respostas, que vão para ele na próxima execução, na mesma sessão.
 *
 * @param resumo o que o agente já tinha feito ao parar; null se ele não disse
 */
public record Direcionamento(String resumo, List<Pergunta> perguntas) {

    /**
     * @param opcoes   respostas sugeridas pelo agente; além delas, sempre dá para escrever outra
     * @param resposta a sua resposta, ou null enquanto não respondida
     */
    public record Pergunta(String texto, List<String> opcoes, String resposta) {

        public Pergunta {
            opcoes = opcoes == null ? List.of() : List.copyOf(opcoes);
        }
    }

    public Direcionamento {
        perguntas = perguntas == null ? List.of() : List.copyOf(perguntas);
    }

    /** Se todas as perguntas já têm resposta. */
    public boolean respondido() {
        return !perguntas.isEmpty() && perguntas.stream().allMatch(p -> p.resposta() != null && !p.resposta().isBlank());
    }

    /**
     * Guarda as respostas, uma por pergunta e na mesma ordem.
     *
     * @throws IllegalArgumentException se faltar alguma resposta
     */
    public Direcionamento comRespostas(List<String> respostas) {
        if (respostas == null || respostas.size() != perguntas.size()
                || respostas.stream().anyMatch(r -> r == null || r.isBlank())) {
            throw new IllegalArgumentException("Responda todas as perguntas.");
        }
        List<Pergunta> respondidas = new ArrayList<>();
        for (int i = 0; i < perguntas.size(); i++) {
            Pergunta p = perguntas.get(i);
            respondidas.add(new Pergunta(p.texto(), p.opcoes(), respostas.get(i).strip()));
        }
        return new Direcionamento(resumo, respondidas);
    }
}
