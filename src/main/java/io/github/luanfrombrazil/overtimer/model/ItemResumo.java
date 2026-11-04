package io.github.luanfrombrazil.overtimer.model;

/**
 * Linha resumida usada na Home (última sessão, pendências).
 */
public record ItemResumo(String titulo, String detalhe, Estado estado) {
}
