package io.github.luanfrombrazil.overtimer.service.consumo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Texto de quando uma janela de uso renova, no fuso local.
 */
final class Renovacao {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_E_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private Renovacao() {
    }

    /** Ex.: " (renova às 14:30)" ou " (renova às 03/11 09:00)". */
    static String texto(Instant instante) {
        ZonedDateTime local = instante.atZone(ZoneId.systemDefault());
        boolean hoje = local.toLocalDate().equals(LocalDate.now());
        return " (renova às " + local.format(hoje ? HORA : DIA_E_HORA) + ")";
    }
}
