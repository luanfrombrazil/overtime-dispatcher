package io.github.luanfrombrazil.overtimedispatcher.service.persistence;

import io.github.luanfrombrazil.overtimedispatcher.model.Configuracoes;
import io.github.luanfrombrazil.overtimedispatcher.model.HorarioFuncionamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

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
    void permiteRespeitaOsSegmentos() {
        HorarioFuncionamento horario = HorarioFuncionamento.vazio();
        horario.definir(0, 45, true); // segunda, 22:30-23:00

        assertTrue(horario.permite(LocalDateTime.of(2026, 11, 2, 22, 45)));
        assertFalse(horario.permite(LocalDateTime.of(2026, 11, 2, 22, 15)));
        assertFalse(horario.permite(LocalDateTime.of(2026, 11, 3, 22, 45)));
    }
}
