package io.github.luanfrombrazil.overtimedispatcher.model;

import java.util.List;

/**
 * Agrupa Tasks e aponta para uma pasta do sistema.
 */
public record Projeto(String nome, String sigla, String detalhe, Estado estado,
                      String pasta, String branch, String provedor,
                      List<Task> tasks, List<String> console) {
}
