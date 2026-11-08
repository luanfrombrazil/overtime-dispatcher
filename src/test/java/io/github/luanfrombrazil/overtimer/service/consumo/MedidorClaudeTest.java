package io.github.luanfrombrazil.overtimer.service.consumo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luanfrombrazil.overtimer.model.Consumo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedidorClaudeTest {

    private final MedidorClaude medidor = new MedidorClaude(Path.of("nao-existe.json"));

    @Test
    void interpretaSessaoESemana() throws Exception {
        Consumo c = MedidorClaude.interpretar("Claude", new ObjectMapper().readTree(
                "{\"five_hour\":{\"utilization\":39.4},\"seven_day\":{\"utilization\":53.0}}"));

        assertEquals(39, c.sessao());
        assertEquals(53, c.semanal());
    }

    @Test
    void limiteAtingidoMantemAUltimaMedicao() {
        Instant quando = Instant.parse("2026-11-04T15:02:00Z");
        medidor.lembrar(new Consumo("Claude", 39, 53, "Janela de 5 h: 39%"), quando);

        Consumo c = medidor.limiteAtingido();

        assertEquals(39, c.sessao());
        assertEquals(53, c.semanal());
        String hora = LocalTime.ofInstant(quando, ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"));
        assertTrue(c.detalhe().contains("medido às " + hora), c.detalhe());
        assertTrue(c.detalhe().contains("limite de consultas"), c.detalhe());
    }

    @Test
    void limiteAtingidoSemMedicaoAnteriorFicaSemPorcentagem() {
        Consumo c = medidor.limiteAtingido();

        assertFalse(c.conhecido());
        assertTrue(c.detalhe().contains("Limite de consultas"), c.detalhe());
    }
}
