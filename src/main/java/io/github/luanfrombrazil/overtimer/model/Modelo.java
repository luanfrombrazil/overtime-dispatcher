package io.github.luanfrombrazil.overtimer.model;

/**
 * Modelo de linguagem que o agente usa numa subtarefa. Padrão deixa a escolha para o próprio agente.
 */
public enum Modelo {
    PADRAO("Padrão", null),
    OPUS("Opus", "opus"),
    SONNET("Sonnet", "sonnet"),
    HAIKU("Haiku", "haiku");

    private final String rotulo;
    private final String apelido;

    Modelo(String rotulo, String apelido) {
        this.rotulo = rotulo;
        this.apelido = apelido;
    }

    /** Nome passado ao CLI do agente ({@code --model}), ou null para não passar nenhum. */
    public String apelido() {
        return apelido;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
