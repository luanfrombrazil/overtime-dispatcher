package io.github.luanfrombrazil.overtimer.service.consumo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luanfrombrazil.overtimer.model.Consumo;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedidorCodexTest {

    private static final Instant AGORA = Instant.parse("2026-11-04T18:00:00Z");

    @Test
    void interpretaAApi() throws Exception {
        Consumo c = MedidorCodex.interpretarApi("Codex", new ObjectMapper().readTree(
                "{\"rate_limit\":{\"primary_window\":{\"used_percent\":12,\"limit_window_seconds\":18000,"
                        + "\"reset_after_seconds\":3600},\"secondary_window\":{\"used_percent\":40.4,"
                        + "\"limit_window_seconds\":604800}}}"), AGORA);

        assertEquals(12, c.sessao());
        assertEquals(40, c.semanal());
        assertTrue(c.detalhe().startsWith("Janela de 5 h: 12%"), c.detalhe());
        assertTrue(c.detalhe().contains("Janela de 7 dias: 40%"), c.detalhe());
    }

    @Test
    void apiSemJanelasFicaSemPorcentagem() throws Exception {
        Consumo c = MedidorCodex.interpretarApi("Codex", new ObjectMapper().readTree("{\"rate_limit\":null}"), AGORA);

        assertFalse(c.conhecido());
    }

    @Test
    void sessaoComJanelaJaRenovadaVoltaAZero() throws Exception {
        Consumo c = MedidorCodex.interpretar("Codex", new ObjectMapper().readTree(
                "{\"timestamp\":\"2026-11-01T10:00:00Z\",\"payload\":{\"rate_limits\":{\"primary\":"
                        + "{\"used_percent\":55.0,\"window_minutes\":300,\"resets_at\":1793800000},"
                        + "\"secondary\":null}}}"), AGORA);

        assertEquals(0, c.sessao());
        assertEquals(Consumo.DESCONHECIDO, c.semanal());
    }
}
