package io.github.luanfrombrazil.overtimedispatcher.model;

import java.util.List;

/**
 * Trabalho para um agente. A própria Task já traz as instruções e pode ser executada sozinha;
 * as subtarefas são opcionais e servem apenas para quebrar o trabalho em passos menores.
 *
 * @param subtarefas passos da Task, na ordem de execução; vazia quando a Task é executada inteira
 */
public record Task(String titulo, String detalhe, Estado estado, List<Subtarefa> subtarefas) {
}
