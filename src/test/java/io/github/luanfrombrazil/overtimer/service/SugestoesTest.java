package io.github.luanfrombrazil.overtimer.service;

import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Origem;
import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProjetos;
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

/** Tasks e subtarefas sugeridas por agentes: só rodam depois de aceitas. */
class SugestoesTest {

    @TempDir
    Path dados;

    @TempDir
    Path pastaDoProjeto;

    private ProjetoService servico;
    private Projeto projeto;

    @BeforeEach
    void criar() {
        servico = new ProjetoService(new RepositorioProjetos(dados));
        projeto = servico.salvar(Projeto.novo("App", "AP", "", pastaDoProjeto.toString(), null,
                Provedor.CLAUDE_CODE, Esforco.HIGH, Permissoes.padrao()));
    }

    private static Subtarefa sub(String titulo) {
        return Subtarefa.nova(titulo, "Faça " + titulo, "", false, List.of());
    }

    private Task sugerir(Subtarefa... subtarefas) {
        Task task = Task.sugerida("Planejada", "Porque sim", Provedor.CLAUDE_CODE, Esforco.HIGH, List.of(subtarefas));
        servico.sugerirTask(projeto.id(), task);
        return task;
    }

    private Task atual(Task task) {
        return servico.buscar(projeto.id()).orElseThrow().tasks().stream()
                .filter(t -> t.id().equals(task.id())).findFirst().orElse(null);
    }

    private Projeto projetoAtual() {
        return servico.buscar(projeto.id()).orElseThrow();
    }

    @Test
    void taskSugeridaNaoRodaAteSerAceitaEAvisaATela() {
        List<Projeto> avisados = new ArrayList<>();
        servico.aoMudar(avisados::add);
        Task task = sugerir(sub("A"), sub("B"));

        assertEquals(1, avisados.size(), "a sugestão chega de fora da tela");
        Task sugerida = atual(task);
        assertEquals(Origem.AGENTE, sugerida.origem());
        assertTrue(sugerida.sugerida());
        assertEquals(Estado.SUGERIDA, sugerida.estado());
        assertEquals("Sugerida", sugerida.detalhe());
        assertTrue(sugerida.subtarefas().stream().allMatch(s -> s.estado() == Estado.SUGERIDA));
        assertTrue(servico.proxima().isEmpty());
        assertEquals(Estado.PENDENTE, projetoAtual().estado(), "a sugestão não conta no estado do projeto");

        servico.aceitar(projetoAtual(), task);
        Task aceita = atual(task);
        assertFalse(aceita.sugerida());
        assertEquals(Estado.PENDENTE, aceita.execucao());
        assertEquals("A", servico.proxima().orElseThrow().subtarefa().titulo());
    }

    @Test
    void taskSoContaComoAceitaQuandoTodasAsSubtarefasForemAceitas() {
        Task task = sugerir(sub("A"), sub("B"));
        Subtarefa a = atual(task).subtarefas().get(0);
        Subtarefa b = atual(task).subtarefas().get(1);

        servico.aceitar(projetoAtual(), task, a);
        assertTrue(atual(task).sugerida(), "B ainda é sugestão");
        assertTrue(servico.proxima().isEmpty(), "nenhuma subtarefa roda antes de a Task inteira ser aceita");

        servico.aceitar(projetoAtual(), task, b);
        assertFalse(atual(task).sugerida());
        assertEquals(Estado.PENDENTE, atual(task).execucao());
        assertEquals("A", servico.proxima().orElseThrow().subtarefa().titulo());
    }

    @Test
    void recusarAUltimaSugestaoComAsOutrasAceitasAceitaATask() {
        Task task = sugerir(sub("A"), sub("B"));
        servico.aceitar(projetoAtual(), task, atual(task).subtarefas().get(0));
        servico.recusar(projetoAtual(), task, atual(task).subtarefas().get(1));

        Task aceita = atual(task);
        assertEquals(List.of("A"), aceita.subtarefas().stream().map(Subtarefa::titulo).toList());
        assertFalse(aceita.sugerida());
        assertTrue(servico.proxima().isPresent());
    }

    @Test
    void recusarTodasAsSubtarefasDeixaATaskSugeridaInteira() {
        Task task = sugerir(sub("A"));
        servico.recusar(projetoAtual(), task, atual(task).subtarefas().getFirst());

        assertTrue(atual(task).inteira());
        assertTrue(atual(task).sugerida());
        assertTrue(servico.proxima().isEmpty());
        servico.recusar(projetoAtual(), atual(task));
        assertEquals(null, atual(task), "recusar a Task sugerida a remove");
    }

    @Test
    void editarATaskSugeridaMantemASugestaoEAceitaComOQueFoiEditado() {
        Task task = sugerir();
        Task editada = new Task(task.id(), "Renomeada", "Nova descrição", Provedor.CLAUDE_CODE, Esforco.LOW,
                task.origem(), List.of(), null, Estado.PENDENTE, null, null);
        servico.salvarTask(projetoAtual(), editada);
        assertTrue(atual(task).sugerida(), "editar não aceita");

        servico.aceitar(projetoAtual(), task);
        Task aceita = atual(task);
        assertEquals("Renomeada", aceita.titulo());
        assertEquals(Esforco.LOW, aceita.esforco());
        assertEquals(task.id(), servico.proxima().orElseThrow().task().id());
    }

    @Test
    void subtarefaSugeridaNumaTaskExistenteSeguraATaskInteira() {
        Task task = Task.nova("Existente", "", Provedor.CLAUDE_CODE, Esforco.LOW);
        servico.salvarTask(projetoAtual(), task);
        servico.salvarSubtarefa(projetoAtual(), task, sub("A"));
        Subtarefa a = atual(task).subtarefas().getFirst();
        assertTrue(servico.proxima().isPresent());

        Subtarefa nova = sub("B").comDependencias(List.of(a.id()));
        servico.sugerirSubtarefas(projeto.id(), task.id(), List.of(nova));
        Task comSugestao = atual(task);
        assertTrue(comSugestao.sugerida());
        assertEquals("1 subtarefa sugerida", comSugestao.detalhe());
        assertTrue(servico.proxima().isEmpty(), "A também espera B ser aceita");

        servico.recusar(projetoAtual(), comSugestao);
        assertEquals(List.of("A"), atual(task).subtarefas().stream().map(Subtarefa::titulo).toList());
        assertTrue(servico.proxima().isPresent(), "sem a sugestão, a Task volta para a fila");
    }

    @Test
    void taskFinalizadaOuEmAndamentoNaoRecebeSugestoes() {
        Task task = Task.nova("Existente", "", Provedor.CLAUDE_CODE, Esforco.LOW);
        servico.salvarTask(projetoAtual(), task);
        servico.marcar(projeto.id(), task.id(), null, Estado.EM_ANDAMENTO, null);
        assertThrows(IllegalArgumentException.class,
                () -> servico.sugerirSubtarefas(projeto.id(), task.id(), List.of(sub("B"))));

        servico.marcar(projeto.id(), task.id(), null, Estado.CONCLUIDO, "ok");
        assertThrows(IllegalArgumentException.class,
                () -> servico.sugerirSubtarefas(projeto.id(), task.id(), List.of(sub("B"))));
    }

    @Test
    void aceitarOQueNaoESugestaoFalha() {
        Task task = Task.nova("Normal", "", Provedor.CLAUDE_CODE, Esforco.LOW);
        servico.salvarTask(projetoAtual(), task);
        assertThrows(IllegalArgumentException.class, () -> servico.aceitar(projetoAtual(), task));
        assertThrows(IllegalArgumentException.class, () -> servico.recusar(projetoAtual(), task));
    }

    @Test
    void sugestaoSobreviveAReabrirAApp() {
        Task task = sugerir(sub("A"));
        Task reaberta = new ProjetoService(new RepositorioProjetos(dados)).buscar(projeto.id()).orElseThrow()
                .tasks().getFirst();
        assertEquals(atual(task), reaberta);
        assertTrue(reaberta.sugerida());
    }
}
