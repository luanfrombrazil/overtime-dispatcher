package io.github.luanfrombrazil.overtimedispatcher.model;

/**
 * Quanto o agente deve se esforçar numa Task.
 */
public enum Esforco {
    LOW("Baixo"),
    MEDIUM("Médio"),
    HIGH("Alto"),
    XHIGH("Muito alto"),
    MAX("Máximo");

    private final String rotulo;

    Esforco(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
