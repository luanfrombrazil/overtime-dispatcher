package io.github.luanfrombrazil.overtimer.service.persistence;

import io.github.luanfrombrazil.overtimer.model.Configuracoes.Aviso;
import io.github.luanfrombrazil.overtimer.model.Notificacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacoesRepositorioTest {

    @TempDir
    Path pasta;

    @Test
    void semArquivoNaoHaNotificacoes() {
        assertEquals(List.of(), new NotificacoesRepositorio(pasta).carregar());
    }

    @Test
    void salvaECarregaAsNotificacoes() {
        List<Notificacao> notificacoes = List.of(
                new Notificacao("n1", Aviso.FALHAS, "Task falhou", "Timeout depois de 3 tentativas.", "p1", "t1",
                        LocalDateTime.of(2026, 11, 5, 14, 30, 12), false),
                new Notificacao("n2", Aviso.LIMITE_DE_USO, "Limite de uso", "Claude Code chegou a 90%.", null, null,
                        LocalDateTime.of(2026, 11, 5, 15, 0), true));

        new NotificacoesRepositorio(pasta).salvar(notificacoes);

        assertEquals(notificacoes, new NotificacoesRepositorio(pasta).carregar());
    }

    @Test
    void omiteProjetoETaskAusentes() throws Exception {
        new NotificacoesRepositorio(pasta).salvar(List.of(new Notificacao("n1", Aviso.SESSAO_CONCLUIDA, "Sessão",
                "Concluída.", null, null, LocalDateTime.of(2026, 11, 5, 8, 0), false)));

        String conteudo = Files.readString(pasta.resolve("notificacoes.json"));

        assertFalse(conteudo.contains("projetoId"), conteudo);
        assertFalse(conteudo.contains("taskId"), conteudo);
        assertTrue(conteudo.contains("\"SESSAO_CONCLUIDA\""), conteudo);
    }

    @Test
    void ignoraNotificacaoComTipoDesconhecido() throws Exception {
        Files.writeString(pasta.resolve("notificacoes.json"), """
                {"notificacoes": [
                  {"id": "n1", "tipo": "INEXISTENTE", "titulo": "a", "texto": "", "instante": "2026-11-05T10:00"},
                  {"id": "n2", "tipo": "VERIFICACAO", "titulo": "b", "texto": "", "instante": "2026-11-05T11:00"}
                ]}""");

        List<Notificacao> carregadas = new NotificacoesRepositorio(pasta).carregar();

        assertEquals(1, carregadas.size());
        assertEquals("n2", carregadas.get(0).id());
        assertFalse(carregadas.get(0).lida());
    }

    @Test
    void arquivoInvalidoComecaSemNotificacoes() throws Exception {
        Files.writeString(pasta.resolve("notificacoes.json"), "[1, 2");

        assertEquals(List.of(), new NotificacoesRepositorio(pasta).carregar());
    }
}
