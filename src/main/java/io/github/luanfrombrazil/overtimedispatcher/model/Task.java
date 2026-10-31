package io.github.luanfrombrazil.overtimedispatcher.model;

import java.util.List;
import java.util.UUID;

/**
 * Trabalho para um agente, dividido em subtarefas.
 *
 * @param sessao id da sessão do agente compartilhada pelas subtarefas, ou null antes da primeira execução
 */
public record Task(String id, String titulo, String descricao, Provedor provedor, Esforco esforco,
                   Origem origem, List<Subtarefa> subtarefas, String sessao) {

    public Task {
        subtarefas = subtarefas == null ? List.of() : List.copyOf(subtarefas);
    }

    public static Task nova(String titulo, String descricao, Provedor provedor, Esforco esforco) {
        return new Task(UUID.randomUUID().toString(), titulo, descricao, provedor, esforco, Origem.USUARIO,
                List.of(), null);
    }

    public Task comSubtarefas(List<Subtarefa> subtarefas) {
        return new Task(id, titulo, descricao, provedor, esforco, origem, subtarefas, sessao);
    }

    public Task comSessao(String sessao) {
        return new Task(id, titulo, descricao, provedor, esforco, origem, subtarefas, sessao);
    }

    /** Tasks do Oráculo aparecem em roxo; as demais herdam o estado das subtarefas. */
    public Estado estado() {
        if (origem == Origem.ORACULO) {
            return Estado.ORACULO;
        }
        return Estado.resumir(subtarefas.stream().map(Subtarefa::estado).toList());
    }

    public long verificacoes() {
        return subtarefas.stream().filter(s -> s.estado() == Estado.ATENCAO).count();
    }

    /** Texto curto sob o título. */
    public String detalhe() {
        if (subtarefas.isEmpty()) {
            return "Sem subtarefas";
        }
        return switch (estado()) {
            case ATENCAO -> Textos.verificacoes(verificacoes());
            case ERRO -> "Falhou";
            default -> null;
        };
    }
}
