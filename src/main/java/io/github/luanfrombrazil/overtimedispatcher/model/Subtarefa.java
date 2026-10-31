package io.github.luanfrombrazil.overtimedispatcher.model;

/**
 * Passo pequeno de uma Task.
 *
 * @param dependeDe índice da subtarefa da qual esta depende, ou -1
 */
public record Subtarefa(String titulo, String detalhe, Estado estado, int dependeDe) {
}
