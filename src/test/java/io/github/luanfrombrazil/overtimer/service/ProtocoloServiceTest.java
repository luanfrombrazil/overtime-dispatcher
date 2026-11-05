package io.github.luanfrombrazil.overtimer.service;

import io.github.luanfrombrazil.overtimer.model.Acessos;
import io.github.luanfrombrazil.overtimer.model.Agenda;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.ExecucaoProtocolo;
import io.github.luanfrombrazil.overtimer.model.ItemFila;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.service.agente.AreaDeTrabalho;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProtocolos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    void exigePassos() {
        Protocolo semPassos = Protocolo.novo("Agenda", "AG", " ", Agenda.aCada(60), Provedor.CLAUDE_CODE,
                Esforco.MEDIUM, Acessos.nenhum(), List.of());
        assertThrows(IllegalArgumentException.class, () -> servico.salvar(semPassos));
        assertTrue(servico.protocolos().isEmpty());
        assertTrue(reaberto().protocolos().isEmpty());
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
    void tirarDaFilaTiraSoOsItensPedidos() {
        ItemFila a = ItemFila.novo("A", null, null);
        ItemFila b = ItemFila.novo("B", null, null);
        ItemFila proposta = ItemFila.novo("P", null, null);
        Protocolo protocolo = servico.salvar(novo("Agenda").comFilas(List.of(a, b), List.of(proposta)));
        avisos.clear();

        Protocolo depois = servico.tirarDaFila(protocolo.id(), Set.of(a.id(), "outro")).orElseThrow();

        assertEquals(List.of(b), depois.filaDeEntrada());
        assertEquals(List.of(proposta), depois.propostas(), "as propostas não mudam");
        assertEquals(List.of(b), reaberto().buscar(protocolo.id()).orElseThrow().filaDeEntrada());
        assertEquals(List.of(depois), avisos);
    }

    @Test
    void tirarDaFilaSemNadaParaTirarNaoGrava() {
        Protocolo protocolo = servico.salvar(novo("Agenda"));
        avisos.clear();
        assertEquals(protocolo, servico.tirarDaFila(protocolo.id(), Set.of("x")).orElseThrow());
        assertTrue(avisos.isEmpty());
        assertTrue(servico.tirarDaFila("nao-existe", Set.of("x")).isEmpty());
    }

    @Test
    void excluirTiraDaListaEDoDisco() throws Exception {
        Protocolo protocolo = servico.salvar(novo("Agenda"));
        Path pasta = dados.resolve("protocolos-rotina").resolve(protocolo.id());
        new AreaDeTrabalho(dados).preparar(protocolo);
        Files.writeString(pasta.resolve("trabalho").resolve("notas.md"), "anotações");

        servico.excluir(protocolo.id());

        assertTrue(servico.protocolos().isEmpty());
        assertTrue(reaberto().protocolos().isEmpty());
        assertFalse(Files.exists(pasta), "sai junto a pasta de trabalho");
    }

    @Test
    void editarTrocaADefinicaoEMantemOQueMudouComOPopUpAberto() {
        Protocolo aberto = servico.salvar(novo("Agenda"));
        // Com o pop-up aberto, o protocolo é desligado e recebe um item na fila.
        ItemFila chegou = ItemFila.novo("Marcar a prova", null, null);
        servico.definirAtivo(aberto.id(), false);
        Protocolo atual = servico.buscar(aberto.id()).orElseThrow();
        servico.salvar(atual.comFilas(List.of(chegou), List.of()));

        Protocolo editado = servico.editar(aberto.comDefinicao("Calendário", "CA", "Novos passos",
                Agenda.aCada(15), Provedor.CODEX, Esforco.HIGH,
                new Acessos(List.of(), false, true, null, "claude_ai_Google_Calendar"), List.of()));

        assertEquals("Calendário", editado.nome());
        assertEquals("Novos passos", editado.passos());
        assertEquals(Agenda.aCada(15), editado.agenda());
        assertEquals(Provedor.CODEX, editado.provedor());
        assertEquals("claude_ai_Google_Calendar", editado.acessos().conectorCalendario());
        assertFalse(editado.ativo(), "o desligamento feito com o pop-up aberto fica");
        assertEquals(List.of(chegou), editado.filaDeEntrada(), "o item que chegou fica");
        assertEquals(editado, reaberto().buscar(aberto.id()).orElseThrow());
    }

    private static ExecucaoProtocolo execucao(int minuto, boolean sucesso) {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 5, 8, minuto);
        return ExecucaoProtocolo.de(inicio, inicio.plusSeconds(40), sucesso, "Resultado " + minuto,
                List.of("08:" + minuto + " Agenda · Claude Code", "  › Read notas.md"));
    }

    @Test
    void historicoGuardaAsUltimasExecucoesESobreviveAReabrir() {
        Protocolo protocolo = servico.salvar(novo("Agenda"));
        avisos.clear();
        for (int minuto = 0; minuto < ProtocoloService.EXECUCOES_NO_HISTORICO + 5; minuto++) {
            servico.registrarExecucao(protocolo.id(), execucao(minuto, minuto % 2 == 0));
        }

        List<ExecucaoProtocolo> historico = servico.historico(protocolo.id());
        assertEquals(ProtocoloService.EXECUCOES_NO_HISTORICO, historico.size());
        assertEquals(execucao(24, true), historico.getFirst(), "a mais recente primeiro");
        assertEquals(execucao(5, false), historico.getLast());
        assertEquals(historico, reaberto().historico(protocolo.id()));
        assertEquals(execucao(24, true), reaberto().ultimaExecucao(protocolo.id()).orElseThrow());
        assertEquals(ProtocoloService.EXECUCOES_NO_HISTORICO + 5, avisos.size(), "a tela é avisada a cada uma");
    }

    @Test
    void historicoDeProtocoloExcluidoNaoVolta() {
        Protocolo protocolo = servico.salvar(novo("Agenda"));
        servico.registrarExecucao(protocolo.id(), execucao(0, true));
        servico.excluir(protocolo.id());

        servico.registrarExecucao(protocolo.id(), execucao(1, true));

        assertTrue(servico.historico(protocolo.id()).isEmpty());
        assertFalse(Files.exists(dados.resolve("protocolos-rotina").resolve(protocolo.id())),
                "a execução que termina depois da exclusão não recria a pasta");
    }

    @Test
    void resumoDaExecucao() {
        ExecucaoProtocolo ok = execucao(3, true);
        assertEquals("05/11 08:03 → 08:03 · Concluída", ok.resumo());
        assertEquals("Resultado 3", ok.primeiraLinha());
        assertEquals("05/11 08:03 → 08:03 · Falhou", execucao(3, false).resumo());
    }

    @Test
    void editarProtocoloExcluidoAvisa() {
        Protocolo protocolo = servico.salvar(novo("Agenda"));
        servico.excluir(protocolo.id());
        assertThrows(IllegalArgumentException.class, () -> servico.editar(protocolo));
        assertTrue(reaberto().protocolos().isEmpty(), "não volta a existir");
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
