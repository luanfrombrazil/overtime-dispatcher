package io.github.luanfrombrazil.overtimedispatcher.model;

/**
 * Passo pequeno e opcional de uma Task.
 *
 * @param dependeDe índice da subtarefa da qual esta depende, ou -1
 * @param commit    se o agente deve fazer commit ao terminar esta subtarefa
 * @param push      se o agente deve fazer push depois do commit
 */
public record Subtarefa(String titulo, String detalhe, Estado estado, int dependeDe, boolean commit, boolean push) {

    /** Subtarefa com os padrões: commit ligado e push desligado. */
    public Subtarefa(String titulo, String detalhe, Estado estado, int dependeDe) {
        this(titulo, detalhe, estado, dependeDe, true, false);
    }
}
