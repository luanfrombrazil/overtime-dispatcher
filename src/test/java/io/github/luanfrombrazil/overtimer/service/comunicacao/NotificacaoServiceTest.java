package io.github.luanfrombrazil.overtimer.service.comunicacao;

import io.github.luanfrombrazil.overtimer.model.Configuracoes.Aviso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Notificacoes;
import io.github.luanfrombrazil.overtimer.model.Notificacao;
import io.github.luanfrombrazil.overtimer.service.persistence.NotificacoesRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacaoServiceTest {

    private static final ZoneId ZONA = ZoneOffset.UTC;
    private static final Notificacoes SO_FALHAS_E_VERIFICACAO = new Notificacoes(false, false, LocalTime.of(23, 0),
            LocalTime.of(7, 0), true, true, false, false, EnumSet.of(Aviso.FALHAS, Aviso.VERIFICACAO));

    @TempDir
    Path pasta;

    private static Clock as(int hora) {
        return Clock.fixed(LocalDateTime.of(2026, 11, 5, hora, 0).toInstant(ZoneOffset.UTC), ZONA);
    }

    private NotificacaoService servico(Clock relogio) {
        return new NotificacaoService(new NotificacoesRepositorio(pasta), SO_FALHAS_E_VERIFICACAO, relogio);
    }

    @Test
    void registraAvisoMarcado() {
        NotificacaoService servico = servico(as(10));

        Optional<Notificacao> registrada = servico.registrar(Aviso.FALHAS, "Task falhou", "Timeout.", "p1", "t1");

        assertTrue(registrada.isPresent());
        Notificacao n = registrada.get();
        assertEquals(Aviso.FALHAS, n.tipo());
        assertEquals("p1", n.projetoId());
        assertEquals("t1", n.taskId());
        assertEquals(LocalDateTime.of(2026, 11, 5, 10, 0), n.instante());
        assertFalse(n.lida());
        assertEquals(List.of(n), servico.listar());
        assertEquals(1, servico.naoLidas());
    }

    @Test
    void avisoDesmarcadoNaoERegistrado() {
        NotificacaoService servico = servico(as(10));
        List<Notificacao> avisadas = new ArrayList<>();
        servico.aoMudar(new NotificacaoService.Ouvinte() {
            @Override
            public void registrada(Notificacao notificacao) {
                avisadas.add(notificacao);
            }
        });

        Optional<Notificacao> registrada = servico.registrar(Aviso.SESSAO_CONCLUIDA, "Sessão", "Concluída.", null, null);

        assertTrue(registrada.isEmpty());
        assertEquals(List.of(), servico.listar());
        assertEquals(0, servico.naoLidas());
        assertEquals(List.of(), avisadas);
        assertEquals(List.of(), new NotificacoesRepositorio(pasta).carregar());
    }

    @Test
    void aplicarTrocaOsAvisosMarcados() {
        NotificacaoService servico = servico(as(10));
        servico.aplicar(Notificacoes.padrao());

        assertTrue(servico.registrar(Aviso.DIRECIONAMENTO, "Pergunta", "", "p1", "t1").isPresent());
        assertTrue(servico.registrar(Aviso.LIMITE_DE_USO, "Limite", "", null, null).isEmpty());
    }

    @Test
    void listaDaMaisNovaParaAMaisAntiga() {
        NotificacaoService antiga = servico(as(9));
        Notificacao primeira = antiga.registrar(Aviso.FALHAS, "1", "", null, null).orElseThrow();
        NotificacaoService nova = servico(as(11));
        Notificacao segunda = nova.registrar(Aviso.VERIFICACAO, "2", "", null, null).orElseThrow();

        assertEquals(List.of(segunda, primeira), nova.listar());
    }

    @Test
    void marcaComoLidas() {
        NotificacaoService servico = servico(as(10));
        Notificacao a = servico.registrar(Aviso.FALHAS, "a", "", null, null).orElseThrow();
        servico.registrar(Aviso.VERIFICACAO, "b", "", null, null);
        servico.registrar(Aviso.VERIFICACAO, "c", "", null, null);

        servico.marcarComoLida(a.id());
        assertEquals(2, servico.naoLidas());

        servico.marcarComoLidas();
        assertEquals(0, servico.naoLidas());
        assertTrue(servico.listar().stream().allMatch(Notificacao::lida));
    }

    @Test
    void avisaOsOuvintes() {
        NotificacaoService servico = servico(as(10));
        List<Notificacao> registradas = new ArrayList<>();
        List<Integer> contagens = new ArrayList<>();
        servico.aoMudar(new NotificacaoService.Ouvinte() {
            @Override
            public void registrada(Notificacao notificacao) {
                registradas.add(notificacao);
            }

            @Override
            public void mudou(int naoLidas) {
                contagens.add(naoLidas);
            }
        });

        Notificacao n = servico.registrar(Aviso.FALHAS, "a", "", null, null).orElseThrow();
        servico.marcarComoLidas();
        // Nada mais a marcar: não avisa de novo.
        servico.marcarComoLidas();

        assertEquals(List.of(n), registradas);
        assertEquals(List.of(1, 0), contagens);
    }

    @Test
    void notificacoesSobrevivemAReabrir() {
        NotificacaoService servico = servico(as(10));
        Notificacao n = servico.registrar(Aviso.FALHAS, "a", "texto", "p1", null).orElseThrow();
        servico.marcarComoLidas();

        NotificacaoService reaberto = servico(Clock.fixed(Instant.EPOCH, ZONA));

        assertEquals(List.of(n.comoLida()), reaberto.listar());
        assertEquals(0, reaberto.naoLidas());
    }
}
