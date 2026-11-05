package io.github.luanfrombrazil.overtimer.model;

import java.util.List;
import java.util.UUID;

/**
 * Agrupa Tasks e aponta para uma pasta do sistema.
 *
 * @param branch    branch principal do git, ou null quando o projeto não usa git
 * @param provedor  provedor padrão das novas Tasks
 * @param esforco   esforço padrão das novas Tasks
 * @param imagem    caminho da imagem (.png ou .jpg) usada como ícone, ou null para mostrar as iniciais
 */
public record Projeto(String id, String nome, String sigla, String descricao, String pasta, String branch,
                      Provedor provedor, Esforco esforco, Permissoes permissoes, List<Task> tasks, String imagem) {

    public Projeto {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    /** Projeto sem imagem. */
    public Projeto(String id, String nome, String sigla, String descricao, String pasta, String branch,
                   Provedor provedor, Esforco esforco, Permissoes permissoes, List<Task> tasks) {
        this(id, nome, sigla, descricao, pasta, branch, provedor, esforco, permissoes, tasks, null);
    }

    public static Projeto novo(String nome, String sigla, String descricao, String pasta, String branch,
                               Provedor provedor, Esforco esforco, Permissoes permissoes) {
        return new Projeto(UUID.randomUUID().toString(), nome, sigla, descricao, pasta, branch,
                provedor, esforco, permissoes, List.of());
    }

    public Projeto comTasks(List<Task> tasks) {
        return new Projeto(id, nome, sigla, descricao, pasta, branch, provedor, esforco, permissoes, tasks, imagem);
    }

    public Projeto comNome(String nome, String sigla) {
        return new Projeto(id, nome, sigla, descricao, pasta, branch, provedor, esforco, permissoes, tasks, imagem);
    }

    public Projeto comImagem(String imagem) {
        return new Projeto(id, nome, sigla, descricao, pasta, branch, provedor, esforco, permissoes, tasks, imagem);
    }

    public boolean usaGit() {
        return branch != null;
    }

    /** Estado resumido das Tasks; as sugestões do Oráculo e os rascunhos não contam. */
    public Estado estado() {
        return Estado.resumir(tasks.stream().map(Task::estado)
                .filter(e -> e != Estado.ORACULO && e != Estado.RASCUNHO).toList());
    }

    /** Texto curto sob o nome, na barra lateral. */
    public String detalhe() {
        return switch (estado()) {
            case ATENCAO -> Textos.pendencias(tasks.stream().mapToLong(Task::verificacoes).sum(),
                    tasks.stream().mapToLong(Task::direcionamentos).sum());
            case ERRO -> "Falhou";
            default -> null;
        };
    }
}
