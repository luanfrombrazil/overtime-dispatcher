package io.github.luanfrombrazil.overtimer.service.consumo;

import io.github.luanfrombrazil.overtimer.model.Consumo;

/**
 * Lê quanto da janela de uso de um provedor já foi gasto.
 */
public interface Medidor {

    /** Nome curto mostrado na barra superior (ex.: "Claude"). */
    String provedor();

    /** Nunca lança exceção: quando não dá para medir, devolve {@link Consumo#desconhecido}. */
    Consumo medir();

    /** Provedor que ainda não tem de onde ler o uso: aparece na barra sem porcentagem. */
    static Medidor semLeitura(String provedor, String motivo) {
        return new Medidor() {
            @Override
            public String provedor() {
                return provedor;
            }

            @Override
            public Consumo medir() {
                return Consumo.desconhecido(provedor, motivo);
            }
        };
    }
}
