package io.github.luanfrombrazil.overtimer.model;

/**
 * Número da Home com a sua série para o mini-gráfico.
 */
public record Estatistica(String valor, String rotulo, Estado cor, int[] serie) {
}
