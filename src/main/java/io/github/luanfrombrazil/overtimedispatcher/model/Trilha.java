package io.github.luanfrombrazil.overtimedispatcher.model;

import java.util.List;

/**
 * Conjunto de tarefas para o usuário, guardado no vault do Obsidian.
 */
public record Trilha(String nome, String sigla, String detalhe, Estado estado,
                     String arquivo, List<TarefaPessoal> tarefas) {
}
