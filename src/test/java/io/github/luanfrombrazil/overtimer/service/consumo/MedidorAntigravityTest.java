package io.github.luanfrombrazil.overtimer.service.consumo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luanfrombrazil.overtimer.model.Consumo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedidorAntigravityTest {

    private static final String RESPOSTA = "{\"userStatus\":{\"cascadeModelConfigData\":{\"clientModelConfigs\":["
            + "{\"label\":\"Gemini 3.8 Flash (High)\",\"quotaInfo\":{\"remainingFraction\":0.8}},"
            + "{\"label\":\"Gemini 3.8 Pro\",\"quotaInfo\":{\"remainingFraction\":0.25}},"
            + "{\"label\":\"Sem cota\"}]}}}";

    @Test
    void usaOModeloSelecionado() throws Exception {
        Consumo c = MedidorAntigravity.interpretar("Gemini", new ObjectMapper().readTree(RESPOSTA),
                "Gemini 3.8 Flash (High)");

        assertEquals(20, c.sessao());
        assertEquals(Consumo.DESCONHECIDO, c.semanal());
        assertTrue(c.detalhe().startsWith("Gemini 3.8 Flash (High): 20%"), c.detalhe());
    }

    @Test
    void semSelecionadoUsaOMaisGasto() throws Exception {
        Consumo c = MedidorAntigravity.interpretar("Gemini", new ObjectMapper().readTree(RESPOSTA), null);

        assertEquals(75, c.sessao());
    }

    @Test
    void semRemainingFractionACotaAcabou() throws Exception {
        Consumo c = MedidorAntigravity.interpretar("Gemini", new ObjectMapper().readTree(
                "{\"userStatus\":{\"cascadeModelConfigData\":{\"clientModelConfigs\":["
                        + "{\"label\":\"Gemini\",\"quotaInfo\":{\"resetTime\":\"2026-11-04T20:00:00Z\"}}]}}}"), null);

        assertEquals(100, c.sessao());
    }

    @Test
    void semModelosFicaSemPorcentagem() throws Exception {
        Consumo c = MedidorAntigravity.interpretar("Gemini", new ObjectMapper().readTree("{}"), null);

        assertFalse(c.conhecido());
    }

    @Test
    void antigravityFechadoMantemAUltimaMedicao() {
        MedidorAntigravity medidor = new MedidorAntigravity(Path.of("nao-existe"));
        medidor.lembrar(new Consumo("Gemini", 30, Consumo.DESCONHECIDO, "Gemini: 30%"), Instant.now());

        Consumo c = medidor.medir();

        assertEquals(30, c.sessao());
        assertTrue(c.detalhe().contains("Antigravity fechado"), c.detalhe());
    }

    @Test
    void antigravityFechadoSemMedicaoAnteriorFicaSemPorcentagem() {
        Consumo c = new MedidorAntigravity(Path.of("nao-existe")).medir();

        assertFalse(c.conhecido());
    }
}
