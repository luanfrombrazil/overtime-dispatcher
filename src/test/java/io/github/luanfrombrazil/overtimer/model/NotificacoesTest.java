package io.github.luanfrombrazil.overtimer.model;

import io.github.luanfrombrazil.overtimer.model.Configuracoes.Aviso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Notificacoes;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacoesTest {

    private static final LocalDateTime MEIO_DIA = LocalDateTime.of(2026, 11, 5, 12, 0);

    private static Notificacoes com(boolean agora, boolean automaticamente, String inicio, String fim,
                                    boolean falhasFuram) {
        return new Notificacoes(agora, automaticamente, LocalTime.parse(inicio), LocalTime.parse(fim), falhasFuram,
                true, false, false, EnumSet.allOf(Aviso.class));
    }

    private static LocalDateTime as(int hora, int minuto) {
        return LocalDateTime.of(2026, 11, 5, hora, minuto);
    }

    @Test
    void silenciarAgoraSilenciaForaDaFaixa() {
        Notificacoes notificacoes = com(true, false, "23:00", "07:00", false);

        assertTrue(notificacoes.silenciado(MEIO_DIA, Aviso.VERIFICACAO));
        assertTrue(notificacoes.silenciado(MEIO_DIA, Aviso.FALHAS));
    }

    @Test
    void semSilencioNadaFicaSilenciado() {
        Notificacoes notificacoes = com(false, false, "23:00", "07:00", false);

        assertFalse(notificacoes.silenciado(as(23, 30), Aviso.VERIFICACAO));
    }

    @Test
    void faixaQueViraODia() {
        Notificacoes notificacoes = com(false, true, "23:00", "07:00", false);

        assertFalse(notificacoes.silenciado(as(22, 59), Aviso.DIRECIONAMENTO));
        assertTrue(notificacoes.silenciado(as(23, 0), Aviso.DIRECIONAMENTO));
        assertTrue(notificacoes.silenciado(as(0, 0), Aviso.DIRECIONAMENTO));
        assertTrue(notificacoes.silenciado(as(6, 59), Aviso.DIRECIONAMENTO));
        assertFalse(notificacoes.silenciado(as(7, 0), Aviso.DIRECIONAMENTO));
        assertFalse(notificacoes.silenciado(MEIO_DIA, Aviso.DIRECIONAMENTO));
    }

    @Test
    void faixaNoMesmoDia() {
        Notificacoes notificacoes = com(false, true, "12:00", "14:00", false);

        assertTrue(notificacoes.silenciado(as(13, 0), Aviso.VERIFICACAO));
        assertFalse(notificacoes.silenciado(as(14, 0), Aviso.VERIFICACAO));
        assertFalse(notificacoes.silenciado(as(11, 59), Aviso.VERIFICACAO));
    }

    @Test
    void faixaIgnoradaSemSilenciarAutomaticamente() {
        Notificacoes notificacoes = com(false, false, "23:00", "07:00", false);

        assertFalse(notificacoes.silenciado(as(2, 0), Aviso.VERIFICACAO));
    }

    @Test
    void faixaVaziaNaoSilencia() {
        Notificacoes notificacoes = com(false, true, "07:00", "07:00", false);

        assertFalse(notificacoes.silenciado(as(7, 0), Aviso.VERIFICACAO));
    }

    @Test
    void falhaFuraOSilencio() {
        Notificacoes naFaixa = com(false, true, "23:00", "07:00", true);
        Notificacoes agora = com(true, false, "23:00", "07:00", true);

        assertFalse(naFaixa.silenciado(as(2, 0), Aviso.FALHAS));
        assertFalse(agora.silenciado(MEIO_DIA, Aviso.FALHAS));
        assertTrue(naFaixa.silenciado(as(2, 0), Aviso.VERIFICACAO));
        assertTrue(agora.silenciado(MEIO_DIA, Aviso.LIMITE_DE_USO));
    }

    @Test
    void falhaNaoFuraQuandoDesligado() {
        Notificacoes notificacoes = com(false, true, "23:00", "07:00", false);

        assertTrue(notificacoes.silenciado(as(2, 0), Aviso.FALHAS));
    }
}
