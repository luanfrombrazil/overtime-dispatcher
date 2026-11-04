package io.github.luanfrombrazil.overtimer.service.consumo;

import io.github.luanfrombrazil.overtimer.model.Consumo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsumoServiceTest {

    /** Relógio que só anda quando o teste manda. */
    static class RelogioFalso extends Clock {
        Instant agora = Instant.parse("2026-11-04T12:00:00Z");

        void avancar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private final AtomicInteger medicoes = new AtomicInteger();
    private final AtomicBoolean ocupado = new AtomicBoolean();
    private final RelogioFalso relogio = new RelogioFalso();
    private final ConsumoService servico = new ConsumoService(List.of(new Medidor() {
        @Override
        public String provedor() {
            return "Teste";
        }

        @Override
        public Consumo medir() {
            medicoes.incrementAndGet();
            return new Consumo("Teste", 10, 20);
        }
    }), ocupado::get, relogio);

    @Test
    void naoMedeAntesDeTrintaMinutosOcioso() {
        servico.medir();
        relogio.avancar(Duration.ofMinutes(29));
        servico.verificarOciosidade();

        assertEquals(1, medicoes.get());
        assertEquals(List.of(new Consumo("Teste", 10, 20)), servico.consumo());
    }

    @Test
    void medeDeNovoAposTrintaMinutosOcioso() {
        servico.medir();
        relogio.avancar(Duration.ofMinutes(30));
        servico.verificarOciosidade();
        relogio.avancar(Duration.ofMinutes(1));
        servico.verificarOciosidade();

        assertEquals(2, medicoes.get(), "mede uma vez ao completar 30 min e volta a contar");
    }

    @Test
    void agenteRodandoNaoContaComoOcioso() {
        servico.medir();
        ocupado.set(true);
        relogio.avancar(Duration.ofMinutes(45));
        servico.verificarOciosidade();
        assertEquals(1, medicoes.get(), "com um agente rodando não mede");

        // A contagem dos 30 min recomeça quando o agente para.
        ocupado.set(false);
        relogio.avancar(Duration.ofMinutes(29));
        servico.verificarOciosidade();
        assertEquals(1, medicoes.get());
        relogio.avancar(Duration.ofMinutes(1));
        servico.verificarOciosidade();
        assertEquals(2, medicoes.get());
    }
}
