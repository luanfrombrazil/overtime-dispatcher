package io.github.luanfrombrazil.overtimer.service;

import io.github.luanfrombrazil.overtimer.model.Acessos;
import io.github.luanfrombrazil.overtimer.model.Agenda;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProtocolos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocoloServiceTest {

    @TempDir
    Path dados;

    private ProtocoloService servico;
    private final List<Protocolo> avisos = new ArrayList<>();

    @BeforeEach
    void criarServico() {
        servico = new ProtocoloService(new RepositorioProtocolos(dados));
        servico.aoMudar(avisos::add);
    }

    private static Protocolo novo(String nome) {
        return Protocolo.novo(nome, nome.substring(0, 2).toUpperCase(), "Passos", Agenda.aCada(60),
                Provedor.CLAUDE_CODE, Esforco.MEDIUM, Acessos.nenhum(), List.of());
    }

    private ProtocoloService reaberto() {
        return new ProtocoloService(new RepositorioProtocolos(dados));
    }

    @Test
    void listaEmOrdemAlfabetica() {
        servico.salvar(novo("faculdade"));
        servico.salvar(novo("Agenda"));
        servico.salvar(novo("E-mail"));
        assertEquals(List.of("Agenda", "E-mail", "faculdade"),
                servico.protocolos().stream().map(Protocolo::nome).toList());
    }

    @Test
    void salvosAparecemAoReabrir() {
        Protocolo protocolo = servico.salvar(novo("Agenda"));
        assertEquals(List.of(protocolo), reaberto().protocolos());
    }

    @Test
    void salvarDeNovoSubstitui() {
        Protocolo protocolo = servico.salvar(novo("Agenda"));
        Protocolo editado = protocolo.comDefinicao("Calendário", "CA", "Outros passos", Agenda.aCada(30),
                Provedor.CODEX, Esforco.LOW, Acessos.nenhum(), List.of());
        servico.salvar(editado);
        assertEquals(List.of(editado), servico.protocolos());
        assertEquals(List.of(editado), reaberto().protocolos());
    }

    @Test
    void exigeNome() {
        assertThrows(IllegalArgumentException.class, () -> servico.salvar(novo("  ")));
        assertTrue(servico.protocolos().isEmpty());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void desligaELigaSalvando() {
        Protocolo protocolo = servico.salvar(novo("Agenda"));
        assertTrue(protocolo.ativo());

        assertFalse(servico.definirAtivo(protocolo.id(), false).orElseThrow().ativo());
        assertFalse(reaberto().buscar(protocolo.id()).orElseThrow().ativo());

        assertTrue(servico.definirAtivo(protocolo.id(), true).orElseThrow().ativo());
        assertTrue(reaberto().buscar(protocolo.id()).orElseThrow().ativo());
    }

    @Test
    void ligarProtocoloInexistenteNaoFazNada() {
        assertTrue(servico.definirAtivo("nao-existe", true).isEmpty());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void excluirTiraDaListaEDoDisco() {
        Protocolo protocolo = servico.salvar(novo("Agenda"));
        servico.excluir(protocolo.id());
        assertTrue(servico.protocolos().isEmpty());
        assertTrue(reaberto().protocolos().isEmpty());
    }

    @Test
    void avisaCadaMudanca() {
        Protocolo protocolo = servico.salvar(novo("Agenda"));
        Protocolo desligado = servico.definirAtivo(protocolo.id(), false).orElseThrow();
        servico.definirAtivo(protocolo.id(), false);
        servico.excluir(protocolo.id());
        servico.excluir(protocolo.id());
        assertEquals(List.of(protocolo, desligado, desligado), avisos);
    }
}
