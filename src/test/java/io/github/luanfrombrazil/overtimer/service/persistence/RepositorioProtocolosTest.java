package io.github.luanfrombrazil.overtimer.service.persistence;

import io.github.luanfrombrazil.overtimer.model.Acessos;
import io.github.luanfrombrazil.overtimer.model.Agenda;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.ItemFila;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioProtocolosTest {

    @TempDir
    Path dados;

    private static Protocolo completo() {
        return Protocolo.novo("Faculdade", "FA", "1. Entrar no portal\n2. Listar as entregas",
                        Agenda.nosHorarios(List.of(LocalTime.of(8, 0), LocalTime.of(18, 30))),
                        Provedor.CLAUDE_CODE, Esforco.HIGH,
                        new Acessos(List.of("https://portal.exemplo.edu"), true, false), List.of("id-da-agenda"))
                .comFilas(List.of(ItemFila.novo("Ler o e-mail", "detalhes", "id-do-email")),
                        List.of(ItemFila.novo("Marcar prova", null, "id-da-faculdade")));
    }

    @Test
    void semPastaNaoHaProtocolos() {
        assertTrue(new RepositorioProtocolos(dados).carregar().isEmpty());
    }

    @Test
    void gravaNaPastaDoProtocolo() {
        Protocolo protocolo = completo();
        new RepositorioProtocolos(dados).salvar(protocolo);
        assertTrue(Files.isRegularFile(dados.resolve("protocolos-rotina").resolve(protocolo.id())
                .resolve("protocolo.json")));
    }

    @Test
    void releTodosOsCampos() {
        Protocolo horarios = completo();
        Protocolo intervalo = Protocolo.novo("E-mail", "EM", "Ler", Agenda.aCada(45), Provedor.CODEX, Esforco.LOW,
                Acessos.nenhum(), List.of()).comAtivo(false);
        RepositorioProtocolos repositorio = new RepositorioProtocolos(dados);
        repositorio.salvar(horarios);
        repositorio.salvar(intervalo);

        List<Protocolo> relidos = new RepositorioProtocolos(dados).carregar();
        assertEquals(2, relidos.size());
        assertTrue(relidos.contains(horarios));
        assertTrue(relidos.contains(intervalo));
    }

    @Test
    void salvarDeNovoSubstitui() {
        RepositorioProtocolos repositorio = new RepositorioProtocolos(dados);
        Protocolo protocolo = completo();
        repositorio.salvar(protocolo);
        repositorio.salvar(protocolo.comAtivo(false));
        assertEquals(List.of(protocolo.comAtivo(false)), repositorio.carregar());
    }

    @Test
    void excluirApagaAPasta() {
        RepositorioProtocolos repositorio = new RepositorioProtocolos(dados);
        Protocolo protocolo = completo();
        repositorio.salvar(protocolo);
        repositorio.excluir(protocolo.id());
        assertFalse(Files.exists(dados.resolve("protocolos-rotina").resolve(protocolo.id())));
        assertTrue(repositorio.carregar().isEmpty());
    }

    @Test
    void idForaDaPastaEhRecusado() {
        assertThrows(IllegalArgumentException.class, () -> new RepositorioProtocolos(dados).excluir("../projetos"));
    }
}
