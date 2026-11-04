package io.github.luanfrombrazil.overtimer.service.persistence;

import io.github.luanfrombrazil.overtimer.model.Configuracoes;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Aviso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Execucao;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Notificacoes;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Reserva;
import io.github.luanfrombrazil.overtimer.model.HorarioFuncionamento;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracoesRepositorioTest {

    @TempDir
    Path pasta;

    @Test
    void semArquivoUsaOPadrao() {
        assertEquals(Configuracoes.padrao(), new ConfiguracoesRepositorio(pasta).carregar());
    }

    @Test
    void salvaECarregaOHorario() {
        HorarioFuncionamento horario = HorarioFuncionamento.vazio();
        horario.definirHoras(0, 22, 24);
        horario.definir(2, 15, true);
        horario.definirHoras(6, 0, 24);
        ConfiguracoesRepositorio repositorio = new ConfiguracoesRepositorio(pasta);

        repositorio.salvar(new Configuracoes(horario));

        assertEquals(horario, new ConfiguracoesRepositorio(pasta).carregar().horarioFuncionamento());
    }

    @Test
    void gravaFaixasLegiveis() throws Exception {
        HorarioFuncionamento horario = HorarioFuncionamento.vazio();
        horario.definirHoras(0, 0, 7);
        horario.definirHoras(0, 22, 24);
        new ConfiguracoesRepositorio(pasta).salvar(new Configuracoes(horario));

        String conteudo = Files.readString(pasta.resolve("configuracoes.json"));

        assertTrue(conteudo.contains("\"00:00-07:00\""), conteudo);
        assertTrue(conteudo.contains("\"22:00-24:00\""), conteudo);
    }

    @Test
    void arquivoInvalidoUsaOPadrao() throws Exception {
        Files.writeString(pasta.resolve("configuracoes.json"),
                "{\"horarioFuncionamento\": {\"seg\": [\"25:00-26:00\"]}}");

        assertEquals(Configuracoes.padrao(), new ConfiguracoesRepositorio(pasta).carregar());
    }

    @Test
    void salvaECarregaTodasAsConfiguracoes() {
        HorarioFuncionamento horario = HorarioFuncionamento.vazio();
        horario.definirHoras(3, 8, 18);
        Configuracoes configuracoes = new Configuracoes(horario,
                Map.of(Provedor.CLAUDE_CODE, new Reserva(60, 10), Provedor.GEMINI, new Reserva(0, 0)),
                new Execucao(15, 2, 60),
                new Notificacoes(true, false, LocalTime.of(22, 0), LocalTime.of(6, 30), false, false, true, true,
                        EnumSet.of(Aviso.MERGE, Aviso.SESSAO_CONCLUIDA)));

        new ConfiguracoesRepositorio(pasta).salvar(configuracoes);
        Configuracoes carregadas = new ConfiguracoesRepositorio(pasta).carregar();

        assertEquals(configuracoes, carregadas);
        assertEquals(Reserva.padrao(), carregadas.reserva(Provedor.CODEX));
    }

    @Test
    void arquivoAntigoSoComHorarioCompletaComOsPadroes() throws Exception {
        Files.writeString(pasta.resolve("configuracoes.json"),
                "{\"horarioFuncionamento\": {\"seg\": [\"22:00-24:00\"]}}");

        Configuracoes carregadas = new ConfiguracoesRepositorio(pasta).carregar();

        assertTrue(carregadas.horarioFuncionamento().permite(LocalDateTime.of(2026, 11, 2, 23, 0)));
        assertEquals(Execucao.padrao(), carregadas.execucao());
        assertEquals(Notificacoes.padrao(), carregadas.notificacoes());
        assertEquals(Reserva.padrao(), carregadas.reserva(Provedor.CLAUDE_CODE));
    }

    @Test
    void reservaInvalidaUsaOPadrao() throws Exception {
        Files.writeString(pasta.resolve("configuracoes.json"),
                "{\"reservas\": {\"CODEX\": {\"automacao\": 90, \"oraculo\": 20}}}");

        assertEquals(Configuracoes.padrao(), new ConfiguracoesRepositorio(pasta).carregar());
    }

    @Test
    void permiteRespeitaOsSegmentos() {
        HorarioFuncionamento horario = HorarioFuncionamento.vazio();
        horario.definir(0, 45, true); // segunda, 22:30-23:00

        assertTrue(horario.permite(LocalDateTime.of(2026, 11, 2, 22, 45)));
        assertFalse(horario.permite(LocalDateTime.of(2026, 11, 2, 22, 15)));
        assertFalse(horario.permite(LocalDateTime.of(2026, 11, 3, 22, 45)));
    }
}
