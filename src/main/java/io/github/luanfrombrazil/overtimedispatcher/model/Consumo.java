package io.github.luanfrombrazil.overtimedispatcher.model;

/**
 * Uso da janela de um provedor, em porcentagem.
 *
 * @param porcentagem de 0 a 100, ou {@link #DESCONHECIDO} quando não deu para medir
 * @param detalhe     texto extra para a dica da barra (ex.: semana e horário de renovação), ou null
 */
public record Consumo(String provedor, int porcentagem, String detalhe) {

    public static final int DESCONHECIDO = -1;

    public Consumo {
        if (porcentagem != DESCONHECIDO) {
            porcentagem = Math.clamp(porcentagem, 0, 100);
        }
    }

    public Consumo(String provedor, int porcentagem) {
        this(provedor, porcentagem, null);
    }

    public static Consumo desconhecido(String provedor, String motivo) {
        return new Consumo(provedor, DESCONHECIDO, motivo);
    }

    public boolean conhecido() {
        return porcentagem != DESCONHECIDO;
    }

    /** Ex.: "42%" ou "—". */
    public String rotulo() {
        return conhecido() ? porcentagem + "%" : "—";
    }
}
