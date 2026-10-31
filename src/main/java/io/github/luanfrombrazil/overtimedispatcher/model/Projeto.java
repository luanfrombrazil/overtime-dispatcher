package io.github.luanfrombrazil.overtimedispatcher.model;

import java.util.List;
import java.util.UUID;

/**
 * Agrupa Tasks e aponta para uma pasta do sistema.
 *
 * @param branch    branch principal do git, ou null quando o projeto não usa git
 * @param provedor  provedor padrão das novas Tasks
 * @param esforco   esforço padrão das novas Tasks
 */
public record Projeto(String id, String nome, String sigla, String descricao, String pasta, String branch,
                      Provedor provedor, Esforco esforco, Permissoes permissoes, List<Task> tasks) {

    public Projeto {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    public static Projeto novo(String nome, String sigla, String descricao, String pasta, String branch,
                               Provedor provedor, Esforco esforco, Permissoes permissoes) {
        return new Projeto(UUID.randomUUID().toString(), nome, sigla, descricao, pasta, branch,
                provedor, esforco, permissoes, List.of());
    }

    public Projeto comTasks(List<Task> tasks) {
        return new Projeto(id, nome, sigla, descricao, pasta, branch, provedor, esforco, permissoes, tasks);
    }

    public boolean usaGit() {
        return branch != null;
    }

    /** Estado resumido das Tasks; as sugestões do Oráculo não contam. */
    public Estado estado() {
        return Estado.resumir(tasks.stream().map(Task::estado).filter(e -> e != Estado.ORACULO).toList());
    }

    /** Texto curto sob o nome, na barra lateral. */
    public String detalhe() {
        return switch (estado()) {
            case ATENCAO -> Textos.verificacoes(tasks.stream().mapToLong(Task::verificacoes).sum());
            case ERRO -> "Falhou";
            default -> null;
        };
    }
}
