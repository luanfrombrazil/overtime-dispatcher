package io.github.luanfrombrazil.overtimer.model;

/**
 * Agente que executa as Tasks. Automático deixa a escolha para o agendador.
 */
public enum Provedor {
    AUTOMATICO("Automático"),
    CLAUDE_CODE("Claude Code"),
    CODEX("Codex"),
    GEMINI("Gemini");

    private final String rotulo;

    Provedor(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
