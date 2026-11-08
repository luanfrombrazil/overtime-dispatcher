package io.github.luanfrombrazil.overtimer.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorarioFuncionamentoTest {

    // 2026-11-02 é uma segunda-feira.
    private static final LocalDateTime SEGUNDA_10H15 = LocalDateTime.of(2026, 11, 2, 10, 15);

    @Test
    void permiteSoOsSegmentosMarcados() {
        HorarioFuncionamento horario = HorarioFuncionamento.noitesEFinsDeSemana();
        assertFalse(horario.permite(SEGUNDA_10H15));
        assertTrue(horario.permite(SEGUNDA_10H15.withHour(22)));
        assertTrue(horario.permite(LocalDateTime.of(2026, 11, 7, 12, 0)));
    }

    @Test
    void proximaLiberacaoNoMesmoDia() {
        assertEquals(Optional.of(SEGUNDA_10H15.withHour(22).withMinute(0)),
                HorarioFuncionamento.noitesEFinsDeSemana().proximaLiberacao(SEGUNDA_10H15));
    }

    @Test
    void proximaLiberacaoNaMeiaHora() {
        HorarioFuncionamento horario = HorarioFuncionamento.vazio();
        horario.definir(0, 21, true);
        assertEquals(Optional.of(SEGUNDA_10H15.withMinute(30)), horario.proximaLiberacao(SEGUNDA_10H15));
    }

    @Test
    void proximaLiberacaoDaVoltaNaSemana() {
        HorarioFuncionamento horario = HorarioFuncionamento.vazio();
        horario.definirHoras(0, 8, 9);
        assertEquals(Optional.of(LocalDateTime.of(2026, 11, 9, 8, 0)), horario.proximaLiberacao(SEGUNDA_10H15));
    }

    @Test
    void semHorarioNaoHaProximaLiberacao() {
        assertEquals(Optional.empty(), HorarioFuncionamento.vazio().proximaLiberacao(SEGUNDA_10H15));
    }
}
