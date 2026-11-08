package io.github.luanfrombrazil.overtimer.service;

import io.github.luanfrombrazil.overtimer.model.Direcionamento;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProjetos;
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

    private static Direcionamento perguntas(String... textos) {
        return new Direcionamento("Parei.", java.util.Arrays.stream(textos)
                .map(t -> new Direcionamento.Pergunta(t, List.of("Sim", "Não"), null)).toList());
    }

    @Test
    void direcionamentoSeguraATaskEARespostaDevolveAFila() {
        Projeto projeto = novoProjeto("Meu App");
        Task task = Task.nova("Migração", "", Provedor.CLAUDE_CODE, Esforco.LOW);
        projeto = servico.salvarTask(projeto, task);
        projeto = servico.salvarSubtarefa(projeto, task, sub("A"));
        projeto = servico.salvarSubtarefa(projeto, task, sub("B"));
        Subtarefa a = projeto.tasks().getFirst().subtarefas().getFirst();
        servico.marcar(projeto.id(), task.id(), a.id(), Estado.ATENCAO, "Parei.", perguntas("Runner?", "Surefire?"));

        assertTrue(servico.proxima().isEmpty(), "B não roda enquanto A espera");
        Task parada = servico.buscar(projeto.id()).orElseThrow().tasks().getFirst();
        assertEquals("1 direcionamento", parada.detalhe());
        assertEquals(0, parada.verificacoes());
        // O que foi perguntado sobrevive a reabrir a app.
        assertEquals(parada, new ProjetoService(new RepositorioProjetos(dados)).buscar(projeto.id()).orElseThrow()
                .tasks().getFirst());

        Projeto atual = servico.buscar(projeto.id()).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> servico.responder(atual, task, a, List.of("Sim")));
        assertThrows(IllegalArgumentException.class, () -> servico.responder(atual, task, a, List.of("Sim", " ")));
        Subtarefa b = parada.subtarefas().get(1);
        assertThrows(IllegalArgumentException.class, () -> servico.responder(atual, task, b, List.of("Sim")));

        projeto = servico.responder(atual, task, a, List.of("Sim", "Só no Java 25"));
        Subtarefa respondida = projeto.tasks().getFirst().subtarefas().getFirst();
        assertEquals(Estado.PENDENTE, respondida.estado());
        assertEquals("Só no Java 25", respondida.direcionamento().perguntas().get(1).resposta());
        assertEquals(a.id(), servico.proxima().orElseThrow().subtarefa().id());
    }

    @Test
    void taskInteiraTambemPedeDirecionamento() {
        Projeto projeto = novoProjeto("Meu App");
        Task task = Task.nova("Inteira", "", Provedor.CLAUDE_CODE, Esforco.LOW);
        projeto = servico.salvarTask(projeto, task);
        servico.marcar(projeto.id(), task.id(), null, Estado.ATENCAO, "Parei.", perguntas("Runner?"));

        Task parada = servico.buscar(projeto.id()).orElseThrow().tasks().getFirst();
        assertTrue(parada.aguardaDirecionamento());
        assertEquals("1 direcionamento", parada.detalhe());
        assertEquals("1 direcionamento", servico.buscar(projeto.id()).orElseThrow().detalhe());
        // Editar a Task não apaga as perguntas.
        projeto = servico.salvarTask(servico.buscar(projeto.id()).orElseThrow(), new Task(task.id(), "Renomeada", "",
                Provedor.CLAUDE_CODE, Esforco.LOW, task.origem(), List.of(), null, null, null, null));
        assertTrue(projeto.tasks().getFirst().aguardaDirecionamento());

        projeto = servico.responder(projeto, task, null, List.of("Não"));
        assertEquals(Estado.PENDENTE, projeto.tasks().getFirst().execucao());
        assertTrue(projeto.tasks().getFirst().direcionamento().respondido());
        assertEquals(task.id(), servico.proxima().orElseThrow().task().id());
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
    void conectoresLiberadosSobrevivemAoReinicio() {
        Projeto projeto = servico.salvar(Projeto.novo("Com Figma", "CF", "", pastaDoProjeto.toString(), null,
                Provedor.CLAUDE_CODE, Esforco.MEDIUM, new Permissoes(true, true, false, false, List.of("claude_ai_Figma"))));

        Projeto relido = new ProjetoService(new RepositorioProjetos(dados)).buscar(projeto.id()).orElseThrow();

        assertEquals(List.of("claude_ai_Figma"), relido.permissoes().conectores());
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
    void alternarDependenciaFazODestinoDependerDaOrigemESolta() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        projeto = servico.salvarSubtarefa(projeto, task, sub("A"));
        projeto = servico.salvarSubtarefa(projeto, task, sub("B"));
        List<Subtarefa> subs = projeto.tasks().getFirst().subtarefas();

        // Arrastar de cima para baixo: a de baixo (destino) passa a depender da de cima (origem).
        projeto = servico.alternarDependencia(projeto, task, subs.get(0), subs.get(1));
        assertEquals(List.of(subs.get(0).id()), projeto.tasks().getFirst().subtarefas().get(1).dependeDe());

        // Arrastar de novo, em qualquer sentido, solta.
        projeto = servico.alternarDependencia(projeto, task, subs.get(1), subs.get(0));
        assertTrue(projeto.tasks().getFirst().subtarefas().get(1).dependeDe().isEmpty());
    }

    @Test
    void origemAbaixoLevaODestinoParaLogoAbaixoDelaComQuemEsperaPorEle() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        Subtarefa a = sub("A");
        Subtarefa b = Subtarefa.nova("B", "", "", false, List.of(a.id()));
        Subtarefa c = sub("C");
        Subtarefa d = sub("D");
        Subtarefa e = sub("E");
        for (Subtarefa s : List.of(a, b, c, d, e)) {
            projeto = servico.salvarSubtarefa(projeto, task, s);
        }

        // D (origem) até A (destino): A passa a esperar D e desce para logo abaixo dela, levando B.
        projeto = servico.alternarDependencia(projeto, task, d, a);
        List<Subtarefa> subs = projeto.tasks().getFirst().subtarefas();
        assertEquals(List.of("C", "D", "A", "B", "E"), subs.stream().map(Subtarefa::titulo).toList());
        assertEquals(List.of(d.id()), subs.get(2).dependeDe());
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
        Subtarefa feita = new Subtarefa("1", "A", "", "", false, Estado.CONCLUIDO, List.of(), null, true, false, null, null, null);
        Subtarefa verificar = new Subtarefa("2", "B", "", "", true, Estado.ATENCAO, List.of(), null, true, false, null, null, null);
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM).comSubtarefas(List.of(feita, verificar));
        Projeto projeto = Projeto.novo("P", "P", "", "", null, Provedor.AUTOMATICO, Esforco.MEDIUM, Permissoes.padrao())
                .comTasks(List.of(task));

        assertEquals(Estado.ATENCAO, projeto.estado());
        assertEquals("1 verificação", projeto.detalhe());
        assertEquals(Estado.PENDENTE, Task.nova("Vazia", "", Provedor.AUTOMATICO, Esforco.MEDIUM).estado());
    }

    @Test
    void commitEPushDaSubtarefaSaoSalvos() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        projeto = servico.salvarSubtarefa(projeto, task, sub("Padrão"));
        projeto = servico.salvarSubtarefa(projeto, task, sub("Com push").comGit(true, true));
        projeto = servico.salvarSubtarefa(projeto, task, sub("Sem commit").comGit(false, true));

        List<Subtarefa> relidas = new ProjetoService(new RepositorioProjetos(dados)).buscar(projeto.id())
                .orElseThrow().tasks().getFirst().subtarefas();

        assertTrue(relidas.get(0).commit());
        assertFalse(relidas.get(0).push());
        assertTrue(relidas.get(1).push());
        // Push sem commit não vale.
        assertFalse(relidas.get(2).commit());
        assertFalse(relidas.get(2).push());
        // Arquivos de antes desses campos: commit ligado, push desligado.
        Subtarefa antiga = new Subtarefa("1", "A", "", "", false, Estado.PENDENTE, List.of(), null, null, null, null, null, null);
        assertTrue(antiga.commit());
        assertFalse(antiga.push());
    }

    @Test
    void moverTaskMudaAOrdemDeExecucaoESalva() {
        Task a = Task.nova("A", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Task b = Task.nova("B", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(servico.salvarTask(novoProjeto("P"), a), b);
        projeto = servico.salvarSubtarefa(projeto, a, sub("da A"));
        projeto = servico.salvarSubtarefa(projeto, b, sub("da B"));
        assertEquals("da A", servico.proxima().orElseThrow().subtarefa().titulo());

        projeto = servico.moverTask(projeto, b, -1);
        // Mover além das pontas não faz nada.
        projeto = servico.moverTask(projeto, b, -1);

        assertEquals(List.of("B", "A"), projeto.tasks().stream().map(Task::titulo).toList());
        assertEquals("da B", servico.proxima().orElseThrow().subtarefa().titulo());
        Projeto relido = new ProjetoService(new RepositorioProjetos(dados)).buscar(projeto.id()).orElseThrow();
        assertEquals(List.of("B", "A"), relido.tasks().stream().map(Task::titulo).toList());
    }

    @Test
    void moverSubtarefaMudaAOrdemSemPassarDaQueElaEspera() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        Subtarefa a = sub("A");
        Subtarefa b = Subtarefa.nova("B", "", "", false, List.of(a.id()));
        Subtarefa c = sub("C");
        projeto = servico.salvarSubtarefa(projeto, task, a);
        projeto = servico.salvarSubtarefa(projeto, task, b);
        projeto = servico.salvarSubtarefa(projeto, task, c);

        projeto = servico.moverSubtarefa(projeto, task, c, -2);
        assertEquals(List.of("C", "A", "B"),
                projeto.tasks().getFirst().subtarefas().stream().map(Subtarefa::titulo).toList());
        assertEquals("C", servico.proxima().orElseThrow().subtarefa().titulo());

        // B espera A: não pode subir acima dela.
        Projeto atual = projeto;
        assertThrows(IllegalArgumentException.class, () -> servico.moverSubtarefa(atual, task, b, -1));
        assertEquals(List.of("C", "A", "B"), servico.buscar(projeto.id()).orElseThrow().tasks().getFirst()
                .subtarefas().stream().map(Subtarefa::titulo).toList());
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

        assertEquals("A", servico.proxima().orElseThrow().subtarefa().titulo());
        servico.marcar(projeto.id(), task.id(), a.id(), Estado.EM_ANDAMENTO, null);
        // B espera A; C não depende de ninguém.
        assertEquals("C", servico.proxima().orElseThrow().subtarefa().titulo());
        servico.marcar(projeto.id(), task.id(), c.id(), Estado.CONCLUIDO, "ok");
        assertTrue(servico.proxima().isEmpty());
        servico.marcar(projeto.id(), task.id(), a.id(), Estado.CONCLUIDO, "ok");
        assertEquals("B", servico.proxima().orElseThrow().subtarefa().titulo());
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

    @Test
    void taskSemSubtarefasEntraNaFilaInteira() {
        Task inteira = Task.nova("Inteira", "Faça tudo", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Task dividida = Task.nova("Dividida", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(servico.salvarTask(novoProjeto("P"), inteira), dividida);
        projeto = servico.salvarSubtarefa(projeto, dividida, sub("A"));

        ProjetoService.Proxima proxima = servico.proxima().orElseThrow();
        assertEquals("Inteira", proxima.task().titulo());
        assertEquals(null, proxima.subtarefa());

        servico.marcar(projeto.id(), inteira.id(), null, Estado.EM_ANDAMENTO, null);
        assertEquals("A", servico.proxima().orElseThrow().subtarefa().titulo());
        // Não dá para quebrar a Task enquanto o agente a executa inteira.
        Projeto atual = projeto;
        assertThrows(IllegalArgumentException.class, () -> servico.salvarSubtarefa(atual, inteira, sub("B")));

        projeto = servico.marcar(projeto.id(), inteira.id(), null, Estado.CONCLUIDO, "Feito").orElseThrow();
        Task concluida = projeto.tasks().getFirst();
        assertEquals(Estado.CONCLUIDO, concluida.estado());
        assertEquals("Feito", concluida.resultado());
        // A Task finalizada fica travada.
        assertTrue(concluida.finalizada());
        Projeto finalizado = projeto;
        assertThrows(IllegalArgumentException.class, () -> servico.salvarTask(finalizado, new Task(inteira.id(),
                "Renomeada", "", Provedor.AUTOMATICO, Esforco.MEDIUM, inteira.origem(), List.of(), null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> servico.salvarSubtarefa(finalizado, inteira, sub("B")));
        assertEquals("Inteira", servico.buscar(projeto.id()).orElseThrow().tasks().getFirst().titulo());
    }

    @Test
    void editarTaskPendenteNaoApagaOResultado() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        projeto = servico.marcar(projeto.id(), task.id(), null, Estado.ERRO, "FALHOU: x").orElseThrow();

        projeto = servico.salvarTask(projeto, new Task(task.id(), "Renomeada", "", Provedor.AUTOMATICO,
                Esforco.MEDIUM, task.origem(), List.of(), null, null, null, null));
        assertEquals("Renomeada", projeto.tasks().getFirst().titulo());
        assertEquals(Estado.ERRO, projeto.tasks().getFirst().estado());
        assertEquals("FALHOU: x", projeto.tasks().getFirst().resultado());
    }

    @Test
    void subtarefaFinalizadaTravaEdicaoExclusaoEOrdem() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        Subtarefa a = sub("A");
        Subtarefa b = sub("B");
        projeto = servico.salvarSubtarefa(projeto, task, a);
        projeto = servico.salvarSubtarefa(projeto, task, b);
        projeto = servico.marcar(projeto.id(), task.id(), a.id(), Estado.CONCLUIDO, "ok").orElseThrow();

        Projeto atual = projeto;
        assertThrows(IllegalArgumentException.class, () -> servico.salvarSubtarefa(atual, task, a));
        assertThrows(IllegalArgumentException.class, () -> servico.excluirSubtarefa(atual, task, a));
        assertThrows(IllegalArgumentException.class, () -> servico.moverSubtarefa(atual, task, a, 1));
        // A que ainda está na fila continua editável e pode esperar a finalizada.
        projeto = servico.salvarSubtarefa(projeto, task, b.comDependencias(List.of(a.id())));
        assertEquals(List.of(a.id()), projeto.tasks().getFirst().subtarefas().get(1).dependeDe());
        assertFalse(projeto.tasks().getFirst().finalizada());

        // Refazer devolve a subtarefa à fila e destrava.
        projeto = servico.definirEstado(projeto, task, a, Estado.PENDENTE);
        projeto = servico.salvarSubtarefa(projeto, task, new Subtarefa(a.id(), "A editada", "", "", false,
                Estado.PENDENTE, List.of(), null, true, false, null, null, null));
        assertEquals("A editada", projeto.tasks().getFirst().subtarefas().getFirst().titulo());
    }

    @Test
    void taskFinalizadaNaoMudaDeOrdemEAsAbertasPassamPorEla() {
        Task a = Task.nova("A", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Task b = Task.nova("B", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Task c = Task.nova("C", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(servico.salvarTask(servico.salvarTask(novoProjeto("P"), a), b), c);
        projeto = servico.marcar(projeto.id(), b.id(), null, Estado.CONCLUIDO, "ok").orElseThrow();

        Projeto atual = projeto;
        assertThrows(IllegalArgumentException.class, () -> servico.moverTask(atual, b, -1));
        // Entre as abertas, C sobe uma posição e passa à frente de A; B fica onde estava.
        projeto = servico.moverTask(projeto, c, -1);
        assertEquals(List.of("C", "B", "A"), projeto.tasks().stream().map(Task::titulo).toList());
    }

    @Test
    void taskInteiraFalhaVoltaParaAFilaERecuperaAoAbrir() {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = servico.salvarTask(novoProjeto("P"), task);
        servico.marcar(projeto.id(), task.id(), null, Estado.ERRO, "FALHOU: x");

        projeto = servico.tentarDeNovo(projeto, task);
        assertEquals(Estado.PENDENTE, projeto.tasks().getFirst().estado());

        servico.marcar(projeto.id(), task.id(), null, Estado.EM_ANDAMENTO, null);
        Projeto reaberto = new ProjetoService(new RepositorioProjetos(dados)).buscar(projeto.id()).orElseThrow();
        assertEquals(Estado.PENDENTE, reaberto.tasks().getFirst().estado());

        projeto = servico.marcar(projeto.id(), task.id(), null, Estado.CONCLUIDO, "ok").orElseThrow();
        projeto = servico.definirEstado(projeto, task, Estado.PENDENTE);
        assertEquals(Estado.PENDENTE, projeto.tasks().getFirst().estado());
        assertEquals("ok", projeto.tasks().getFirst().resultado());
    }
}
