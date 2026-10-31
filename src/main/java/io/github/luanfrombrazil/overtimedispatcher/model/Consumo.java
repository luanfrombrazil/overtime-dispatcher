package io.github.luanfrombrazil.overtimedispatcher.model;

import java.time.LocalDateTime;

/**
 * Uso da janela de um provedor, em porcentagem, e quando os créditos dessa janela voltam (null se não se sabe).
 */
public record Consumo(String provedor, int porcentagem, LocalDateTime reset) {

    public Consumo(String provedor, int porcentagem) {
        this(provedor, porcentagem, null);
    }
}
