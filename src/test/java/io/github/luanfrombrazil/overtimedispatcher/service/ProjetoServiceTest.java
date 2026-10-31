package io.github.luanfrombrazil.overtimedispatcher.service;

import io.github.luanfrombrazil.overtimedispatcher.model.Esforco;
import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.Permissoes;
import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Provedor;
import io.github.luanfrombrazil.overtimedispatcher.model.Subtarefa;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import io.github.luanfrombrazil.overtimedispatcher.service.persistence.RepositorioProjetos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjetoServiceTest {

    @TempDir
    Path dados;

    @TempDir
    Path pastaDoProjeto;

    private ProjetoService servico;

    @BeforeEach
    void criarServico() {
        servico = new ProjetoService(new RepositorioProjetos(dados));
    }

    private Projeto novoProjeto(String nome) {
        return servico.salvar(Projeto.novo(nome, "MA", "Descrição", pastaDoProjeto.toString(), "main",
                Provedor.CLAUDE_CODE, Esforco.HIGH, Permissoes.padrao()));
    }

    private static Subtarefa sub(String titulo) {
        return Subtarefa.nova(titulo, "Faça " + titulo, "Pronto", false, List.of());
    }

    @Test
    void salvaERecarregaProjetoComTasksESubtarefas() {
        Projeto projeto = novoProjeto("Meu App");
        Task task = Task.nova("Corrigir login", "Detalhes", Provedor.CODEX, Esforco.LOW);
        projeto = servico.salvarTask(projeto, task);
        projeto = servico.salvarSubtarefa(projeto, task, sub("Reproduzir"));
        Subtarefa primeira = projeto.tasks().getFirst().subtarefas().getFirst();
        projeto = servico.salvarSubtarefa(projeto, task,
                Subtarefa.nova("Corrigir", "", "", true, List.of(primeira.id())));

        Projeto relido = new ProjetoService(new RepositorioProjetos(dados)).buscar(projeto.id()).orElseThrow();

        assertEquals(projeto, relido);
        assertEquals("main", relido.branch());
        assertEquals(Provedor.CLAUDE_CODE, relido.provedor());
        assertEquals(List.of(primeira.id()), relido.tasks().getFirst().subtarefas().get(1).dependeDe());
        assertTrue(Files.isRegularFile(dados.resolve("projetos").resolve(projeto.id())
                .resolve("tasks").resolve(task.id()).resolve("task.json")));
    }

    @Test
    void editarProjetoMantemAsTasks() {
        Projeto projeto = servico.salvarTask(novoProjeto("Meu App"), Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM));
        Projeto editado = new Projeto(projeto.id(), "Outro nome", "ON", "", projeto.pasta(), null,
                Provedor.GEMINI, Esforco.MAX, Permissoes.padrao(), List.of());

        Projeto salvo = servico.salvar(editado);

        assertEquals("Outro nome", salvo.nome());
        assertFalse(salvo.usaGit());
        assertEquals(1, salvo.tasks().size());
    }

    @Test
    void recusaProjetoSemNomeOuComPastaInexistente() {
        assertThrows(IllegalArgumentException.class, () -> servico.salvar(Projeto.novo(" ", "", "",
                pastaDoProjeto.toString(), null, Provedor.AUTOMATICO, Esforco.MEDIUM, Permissoes.padrao())));
        assertThrows(IllegalArgumentException.class, () -> servico.salvar(Projeto.novo("X", "X", "",
                pastaDoProjeto.resolve("nao-existe").toString(), null, Provedor.AUTOMATICO, Esforco.MEDIUM,
                Permissoes.padrao())));
        assertTrue(servico.projetos().isEmpty());
    }

    @Test
    void alternarDependenciaAmarraAPosteriorNaAnteriorESolta() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        projeto = servico.salvarSubtarefa(projeto, task, sub("A"));
        projeto = servico.salvarSubtarefa(projeto, task, sub("B"));
        List<Subtarefa> subs = projeto.tasks().getFirst().subtarefas();

        // Arrastar de baixo para cima também faz a de baixo depender da de cima.
        projeto = servico.alternarDependencia(projeto, task, subs.get(1), subs.get(0));
        assertEquals(List.of(subs.get(0).id()), projeto.tasks().getFirst().subtarefas().get(1).dependeDe());

        projeto = servico.alternarDependencia(projeto, task, subs.get(0), subs.get(1));
        assertTrue(projeto.tasks().getFirst().subtarefas().get(1).dependeDe().isEmpty());
    }

    @Test
    void recusaCicloDeDependencias() {
        Subtarefa a = sub("A");
        Subtarefa b = Subtarefa.nova("B", "", "", false, List.of(a.id()));
        Subtarefa aComCiclo = a.comDependencias(List.of(b.id()));

        assertThrows(IllegalArgumentException.class, () -> ProjetoService.validarDependencias(List.of(aComCiclo, b)));
    }

    @Test
    void excluirSubtarefaRemoveAsDependenciasParaEla() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        Subtarefa a = sub("A");
        projeto = servico.salvarSubtarefa(projeto, task, a);
        projeto = servico.salvarSubtarefa(projeto, task, Subtarefa.nova("B", "", "", false, List.of(a.id())));

        projeto = servico.excluirSubtarefa(projeto, task, a);

        List<Subtarefa> restantes = projeto.tasks().getFirst().subtarefas();
        assertEquals(1, restantes.size());
        assertTrue(restantes.getFirst().dependeDe().isEmpty());
    }

    @Test
    void excluirTaskEProjetoApagaOsArquivos() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        Path pastaProjeto = dados.resolve("projetos").resolve(projeto.id());

        projeto = servico.excluirTask(projeto, task);
        assertFalse(Files.exists(pastaProjeto.resolve("tasks").resolve(task.id())));

        servico.excluir(projeto);
        assertFalse(Files.exists(pastaProjeto));
        assertTrue(Files.isDirectory(pastaDoProjeto));
        assertTrue(new ProjetoService(new RepositorioProjetos(dados)).projetos().isEmpty());
    }

    @Test
    void estadoDoProjetoVemDasSubtarefas() {
        Subtarefa feita = new Subtarefa("1", "A", "", "", false, Estado.CONCLUIDO, List.of(), null);
        Subtarefa verificar = new Subtarefa("2", "B", "", "", true, Estado.ATENCAO, List.of(), null);
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM).comSubtarefas(List.of(feita, verificar));
        Projeto projeto = Projeto.novo("P", "P", "", "", null, Provedor.AUTOMATICO, Esforco.MEDIUM, Permissoes.padrao())
                .comTasks(List.of(task));

        assertEquals(Estado.ATENCAO, projeto.estado());
        assertEquals("1 verificação", projeto.detalhe());
        assertEquals(Estado.PENDENTE, Task.nova("Vazia", "", Provedor.AUTOMATICO, Esforco.MEDIUM).estado());
    }

    @Test
    void proximaSubtarefaRespeitaOrdemEDependencias() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        Subtarefa a = sub("A");
        Subtarefa b = Subtarefa.nova("B", "", "", false, List.of(a.id()));
        Subtarefa c = sub("C");
        projeto = servico.salvarSubtarefa(projeto, task, a);
        projeto = servico.salvarSubtarefa(projeto, task, b);
        projeto = servico.salvarSubtarefa(projeto, task, c);

        assertEquals("A", servico.proximaSubtarefa().orElseThrow().subtarefa().titulo());
        servico.marcar(projeto.id(), task.id(), a.id(), Estado.EM_ANDAMENTO, null);
        // B espera A; C não depende de ninguém.
        assertEquals("C", servico.proximaSubtarefa().orElseThrow().subtarefa().titulo());
        servico.marcar(projeto.id(), task.id(), c.id(), Estado.CONCLUIDO, "ok");
        assertTrue(servico.proximaSubtarefa().isEmpty());
        servico.marcar(projeto.id(), task.id(), a.id(), Estado.CONCLUIDO, "ok");
        assertEquals("B", servico.proximaSubtarefa().orElseThrow().subtarefa().titulo());
    }

    @Test
    void editarSubtarefaNaoMudaOEstadoEDecisaoDoUsuarioMuda() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        Subtarefa a = sub("A");
        projeto = servico.salvarSubtarefa(projeto, task, a);
        projeto = servico.marcar(projeto.id(), task.id(), a.id(), Estado.ATENCAO, "Feito").orElseThrow();

        projeto = servico.salvarSubtarefa(projeto, task, a.comEstado(Estado.PENDENTE, null));
        Subtarefa editada = projeto.tasks().getFirst().subtarefas().getFirst();
        assertEquals(Estado.ATENCAO, editada.estado());
        assertEquals("Feito", editada.resultado());

        projeto = servico.definirEstado(projeto, task, editada, Estado.CONCLUIDO);
        assertEquals(Estado.CONCLUIDO, projeto.tasks().getFirst().subtarefas().getFirst().estado());
    }

    @Test
    void tentarDeNovoDevolveSoAsQueFalharamParaAFila() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        Subtarefa a = sub("A");
        Subtarefa b = sub("B");
        Subtarefa c = sub("C");
        projeto = servico.salvarSubtarefa(projeto, task, a);
        projeto = servico.salvarSubtarefa(projeto, task, b);
        projeto = servico.salvarSubtarefa(projeto, task, c);
        servico.marcar(projeto.id(), task.id(), a.id(), Estado.ERRO, "invalid reference: master");
        servico.marcar(projeto.id(), task.id(), b.id(), Estado.CONCLUIDO, "ok");
        servico.marcar(projeto.id(), task.id(), c.id(), Estado.ERRO, "FALHOU: x");

        projeto = servico.tentarDeNovo(projeto, task);

        List<Estado> estados = projeto.tasks().getFirst().subtarefas().stream().map(Subtarefa::estado).toList();
        assertEquals(List.of(Estado.PENDENTE, Estado.CONCLUIDO, Estado.PENDENTE), estados);
    }

    @Test
    void execucaoInterrompidaVoltaParaAFilaAoAbrir() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        Subtarefa a = sub("A");
        projeto = servico.salvarSubtarefa(projeto, task, a);
        servico.marcar(projeto.id(), task.id(), a.id(), Estado.EM_ANDAMENTO, null);
        servico.definirSessao(projeto.id(), task.id(), "sessao-1");

        Projeto reaberto = new ProjetoService(new RepositorioProjetos(dados)).buscar(projeto.id()).orElseThrow();

        assertEquals(Estado.PENDENTE, reaberto.tasks().getFirst().subtarefas().getFirst().estado());
        assertEquals("sessao-1", reaberto.tasks().getFirst().sessao());
    }
}
