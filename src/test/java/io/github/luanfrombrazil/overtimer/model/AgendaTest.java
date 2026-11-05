package io.github.luanfrombrazil.overtimer.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaTest {

    private static final LocalDateTime DIA_2_10H15 = LocalDateTime.of(2026, 11, 2, 10, 15);

    private static Agenda nosHorarios(int... horas) {
        return Agenda.nosHorarios(Arrays.stream(horas).mapToObj(h -> LocalTime.of(h, 0)).toList());
    }

    @Test
    void horarioFixoSeguinteNoMesmoDia() {
        assertEquals(Optional.of(DIA_2_10H15.withHour(18).withMinute(0)),
                nosHorarios(8, 18).proxima(DIA_2_10H15));
    }

    @Test
    void horariosFixosForaDeOrdemSaoOrdenados() {
        assertEquals(Optional.of(DIA_2_10H15.withHour(12).withMinute(0)),
                nosHorarios(18, 12, 8).proxima(DIA_2_10H15));
    }

    @Test
    void execucaoNoProprioHorarioPassaParaOSeguinte() {
        LocalDateTime rodouAs8 = DIA_2_10H15.withHour(8).withMinute(0).withSecond(40);
        assertEquals(Optional.of(DIA_2_10H15.withHour(18).withMinute(0)), nosHorarios(8, 18).proxima(rodouAs8));
    }

    @Test
    void horarioComMinutos() {
        Agenda agenda = Agenda.nosHorarios(List.of(LocalTime.of(10, 30)));
        assertEquals(Optional.of(DIA_2_10H15.withMinute(30)), agenda.proxima(DIA_2_10H15));
    }

    @Test
    void depoisDoUltimoHorarioViraODia() {
        LocalDateTime noite = LocalDateTime.of(2026, 11, 2, 22, 0);
        assertEquals(Optional.of(LocalDateTime.of(2026, 11, 3, 8, 0)), nosHorarios(8, 18).proxima(noite));
    }

    @Test
    void viradaDoMesEDoAno() {
        LocalDateTime reveillon = LocalDateTime.of(2026, 12, 31, 23, 30);
        assertEquals(Optional.of(LocalDateTime.of(2027, 1, 1, 0, 0)), nosHorarios(0).proxima(reveillon));
    }

    @Test
    void unicoHorarioJaPassadoVaiParaAmanha() {
        assertEquals(Optional.of(LocalDateTime.of(2026, 11, 3, 9, 0)), nosHorarios(9).proxima(DIA_2_10H15));
    }

    @Test
    void semHorariosNaoTemProxima() {
        assertTrue(Agenda.vazia().proxima(DIA_2_10H15).isEmpty());
    }

    @Test
    void intervaloSomaAoInstante() {
        assertEquals(Optional.of(DIA_2_10H15.plusMinutes(90)), Agenda.aCada(90).proxima(DIA_2_10H15));
    }

    @Test
    void intervaloAtravessaAMeiaNoite() {
        LocalDateTime noite = LocalDateTime.of(2026, 11, 2, 23, 0);
        assertEquals(Optional.of(LocalDateTime.of(2026, 11, 3, 1, 0)), Agenda.aCada(120).proxima(noite));
    }

    @Test
    void intervaloPrecisaSerPositivo() {
        assertThrows(IllegalArgumentException.class, () -> Agenda.aCada(0));
    }

    @Test
    void descricao() {
        assertEquals("Todo dia às 08:00, 12:00 e 18:00", nosHorarios(8, 12, 18).descricao());
        assertEquals("A cada 1h30min", Agenda.aCada(90).descricao());
        assertEquals("Sem horário", Agenda.vazia().descricao());
    }
}
