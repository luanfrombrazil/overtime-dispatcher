package io.github.luanfrombrazil.overtimer.model;

/**
 * Uso de um provedor, em porcentagem: a janela da sessão atual e o limite semanal.
 *
 * @param sessao  uso da janela da sessão atual, de 0 a 100, ou {@link #DESCONHECIDO} quando não deu para medir
 * @param semanal uso do limite semanal, de 0 a 100, ou {@link #DESCONHECIDO}
 * @param detalhe texto extra para a dica da barra (ex.: horários de renovação ou o motivo de não medir), ou null
 */
public record Consumo(String provedor, int sessao, int semanal, String detalhe) {

    public static final int DESCONHECIDO = -1;

    public Consumo {
        sessao = limitar(sessao);
        semanal = limitar(semanal);
    }

    public Consumo(String provedor, int sessao, int semanal) {
        this(provedor, sessao, semanal, null);
    }

    public static Consumo desconhecido(String provedor, String motivo) {
        return new Consumo(provedor, DESCONHECIDO, DESCONHECIDO, motivo);
    }

    /** Se ao menos um dos dois usos foi medido. */
    public boolean conhecido() {
        return sessao != DESCONHECIDO || semanal != DESCONHECIDO;
    }

    /** Ex.: "12% · 40%", "12% · —" ou "—". */
    public String rotulo() {
        return conhecido() ? texto(sessao) + " · " + texto(semanal) : "—";
    }

    private static String texto(int porcentagem) {
        return porcentagem == DESCONHECIDO ? "—" : porcentagem + "%";
    }

    private static int limitar(int porcentagem) {
        return porcentagem == DESCONHECIDO ? DESCONHECIDO : Math.clamp(porcentagem, 0, 100);
    }
}
