package io.github.luanfrombrazil.overtimedispatcher.model;

import java.util.List;

/**
 * Trabalho para um agente, dividido em subtarefas.
 */
public record Task(String titulo, String detalhe, Estado estado, List<Subtarefa> subtarefas) {
}
