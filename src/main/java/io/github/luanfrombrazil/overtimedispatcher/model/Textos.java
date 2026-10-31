package io.github.luanfrombrazil.overtimedispatcher.model;

/**
 * Textos curtos compartilhados pelas entidades.
 */
final class Textos {

    private Textos() {
    }

    static String verificacoes(long quantidade) {
        return quantidade + (quantidade == 1 ? " verificação" : " verificações");
    }
}
