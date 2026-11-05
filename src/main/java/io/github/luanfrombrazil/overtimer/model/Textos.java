package io.github.luanfrombrazil.overtimer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Textos curtos compartilhados pelas entidades.
 */
final class Textos {

    private Textos() {
    }

    /** Ex.: "2 verificações", "1 direcionamento", "1 verificação · 1 direcionamento". */
    static String pendencias(long verificacoes, long direcionamentos) {
        List<String> partes = new ArrayList<>();
        if (verificacoes > 0) {
            partes.add(verificacoes + (verificacoes == 1 ? " verificação" : " verificações"));
        }
        if (direcionamentos > 0) {
            partes.add(direcionamentos + (direcionamentos == 1 ? " direcionamento" : " direcionamentos"));
        }
        return partes.isEmpty() ? null : String.join(" · ", partes);
    }
}
