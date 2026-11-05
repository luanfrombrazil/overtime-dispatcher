package io.github.luanfrombrazil.overtimer.service.scheduler;

import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.ProjetoService;
import io.github.luanfrombrazil.overtimer.service.agente.Agente;
import io.github.luanfrombrazil.overtimer.service.agente.Agentes;
import io.github.luanfrombrazil.overtimer.service.agente.AreaDeTrabalho;
import io.github.luanfrombrazil.overtimer.service.agente.Execucao;
import io.github.luanfrombrazil.overtimer.service.agente.ResultadoExecucao;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProjetos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendadorTest {

    /** Responde na hora, sem processo: "FALHOU" ou um pedido de direcionamento quando a instrução pede. */
    static class AgenteFalso implements Agente {
        final List<Execucao> execucoes = new CopyOnWriteArrayList<>();

        @Override
        public Provedor provedor() {
            return Provedor.CLAUDE_CODE;
        }

        @Override
        public boolean disponivel() {
            return true;
        }

        @Override
        public ResultadoExecucao executar(Execucao execucao, Consumer<String> log) {
            execucoes.add(execucao);
            log.accept("› Edit arquivo.txt");
            boolean falhar = execucao.prompt().contains("deve falhar");
            if (execucao.prompt().contains("deve perguntar")) {
                return new ResultadoExecucao(true, """
                        Migrei 14 testes.

                        DIRECIONAMENTO:
                        Pergunta: O que faço com o runner?
                        - Migrar o runner
                        - Remover os testes
                        """, execucao.sessao());
            }
            return new ResultadoExecucao(true, falhar ? "FALHOU: não deu" : "Pronto", execucao.sessao());
        }

        @Override
        public void cancelar() {
        }
    }

    @TempDir
    Path dados;

    @TempDir
    Path pastaDoProjeto;

    private ProjetoService projetos;
    private AgenteFalso agente;
    private Agendador agendador;

    @BeforeEach
    void preparar() {
        projetos = new ProjetoService(new RepositorioProjetos(dados));
        agente = new AgenteFalso();
        agendador = new Agendador(projetos, new Agentes(List.of(agente)), new AreaDeTrabalho(dados));
    }

    @AfterEach
    void encerrar() {
        agendador.encerrar();
    }

    private Projeto projeto() {
        return projetos.salvar(Projeto.novo("P", "P", "", pastaDoProjeto.toString(), null,
                Provedor.AUTOMATICO, Esforco.LOW, Permissoes.padrao()));
    }

    private Subtarefa subtarefa(Projeto projeto, Task task, int indice) {
        return projetos.buscar(projeto.id()).orElseThrow().tasks().stream()
                .filter(t -> t.id().equals(task.id())).findFirst().orElseThrow().subtarefas().get(indice);
    }

    private void esperarFilaVazia() throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        while ((projetos.proxima().isPresent() || agendador.situacao().texto().contains(" em "))
                && System.currentTimeMillis() < limite) {
            Thread.sleep(20);
        }
        Thread.sleep(50);
    }

    @Test
    void executaAsSubtarefasNaOrdemCompartilhandoASessao() throws InterruptedException {
        Task task = Task.nova("Corrigir login", "", Provedor.AUTOMATICO, Esforco.HIGH);
        Projeto projeto = projetos.salvarTask(projeto(), task);
        Subtarefa a = Subtarefa.nova("Reproduzir", "Escreva o teste", "Falha", false, List.of());
        projetos.salvarSubtarefa(projeto, task, a);
        projetos.salvarSubtarefa(projeto, task, Subtarefa.nova("Corrigir", "", "", false, List.of(a.id())));

        agendador.iniciar();
        esperarFilaVazia();

        assertEquals(2, agente.execucoes.size());
        Execucao primeira = agente.execucoes.get(0);
        Execucao segunda = agente.execucoes.get(1);
        assertTrue(primeira.sessaoNova());
        assertFalse(segunda.sessaoNova());
        assertEquals(primeira.sessao(), segunda.sessao());
        assertEquals(Esforco.HIGH, primeira.esforco());
        assertEquals(pastaDoProjeto, primeira.pasta());
        assertTrue(primeira.prompt().contains("Escreva o teste"));
        assertTrue(segunda.prompt().contains("- Reproduzir"), "a segunda sabe o que já foi feito");
        assertEquals(Estado.CONCLUIDO, subtarefa(projeto, task, 1).estado());
        assertEquals(primeira.sessao(), projetos.buscar(projeto.id()).orElseThrow().tasks().getFirst().sessao());
        assertTrue(agendador.log(projeto.id()).stream().anyMatch(l -> l.contains("› Edit arquivo.txt")));
    }

    @Test
    void verificacaoSeguraAsDependentesEFalhaViraErro() throws InterruptedException {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = projetos.salvarTask(projeto(), task);
        Subtarefa verificar = Subtarefa.nova("Verificar", "", "", true, List.of());
        projetos.salvarSubtarefa(projeto, task, verificar);
        projetos.salvarSubtarefa(projeto, task, Subtarefa.nova("Depois", "", "", false, List.of(verificar.id())));
        projetos.salvarSubtarefa(projeto, task, Subtarefa.nova("Quebrada", "Esta deve falhar", "", false, List.of()));

        agendador.iniciar();
        esperarFilaVazia();

        assertEquals(Estado.ATENCAO, subtarefa(projeto, task, 0).estado());
        assertEquals(Estado.PENDENTE, subtarefa(projeto, task, 1).estado());
        assertEquals(Estado.ERRO, subtarefa(projeto, task, 2).estado());
        assertEquals("FALHOU: não deu", subtarefa(projeto, task, 2).resultado());

        // Aprovar a verificação libera a dependente.
        projetos.definirEstado(projeto, task, subtarefa(projeto, task, 0), Estado.CONCLUIDO);
        agendador.agora();
        esperarFilaVazia();
        assertEquals(Estado.CONCLUIDO, subtarefa(projeto, task, 1).estado());
    }

    @Test
    void provedorSemAgenteViraErroSemTravarAFila() throws InterruptedException {
        Projeto projeto = projeto();
        Task codex = Task.nova("Codex", "", Provedor.CODEX, Esforco.MEDIUM);
        Task claude = Task.nova("Claude", "", Provedor.CLAUDE_CODE, Esforco.MEDIUM);
        projeto = projetos.salvarTask(projeto, codex);
        projeto = projetos.salvarTask(projeto, claude);
        projetos.salvarSubtarefa(projeto, codex, Subtarefa.nova("X", "", "", false, List.of()));
        projetos.salvarSubtarefa(projeto, claude, Subtarefa.nova("Y", "", "", false, List.of()));

        agendador.iniciar();
        esperarFilaVazia();

        assertEquals(Estado.ERRO, subtarefa(projeto, codex, 0).estado());
        assertTrue(subtarefa(projeto, codex, 0).resultado().contains("Codex"));
        assertEquals(Estado.CONCLUIDO, subtarefa(projeto, claude, 0).estado());
    }

    @Test
    void taskSemSubtarefasRodaInteira() throws InterruptedException {
        Projeto projeto = projeto();
        Task pronta = Task.nova("Atualizar README", "Explique a instalação", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Task quebrada = Task.nova("Quebrada", "Esta deve falhar", Provedor.AUTOMATICO, Esforco.MEDIUM);
        projeto = projetos.salvarTask(projeto, pronta);
        projetos.salvarTask(projeto, quebrada);

        agendador.iniciar();
        esperarFilaVazia();

        assertEquals(2, agente.execucoes.size());
        String prompt = agente.execucoes.getFirst().prompt();
        assertTrue(prompt.contains("Explique a instalação"));
        assertTrue(prompt.contains("Faça a task inteira"));
        assertFalse(prompt.contains("Subtarefa atual"));
        List<Task> tasks = projetos.buscar(projeto.id()).orElseThrow().tasks();
        assertEquals(Estado.CONCLUIDO, tasks.get(0).estado());
        assertEquals("Pronto", tasks.get(0).resultado());
        assertEquals(Estado.ERRO, tasks.get(1).estado());
        assertEquals("FALHOU: não deu", tasks.get(1).resultado());
    }

    @Test
    void avisaOComecoDeCadaTaskUmaVezSo() throws InterruptedException {
        Projeto projeto = projeto();
        Task comSubtarefas = Task.nova("Com subtarefas", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Task inteira = Task.nova("Inteira", "Faça", Provedor.AUTOMATICO, Esforco.MEDIUM);
        projeto = projetos.salvarTask(projeto, comSubtarefas);
        projeto = projetos.salvarTask(projeto, inteira);
        projetos.salvarSubtarefa(projeto, comSubtarefas, Subtarefa.nova("A", "", "", false, List.of()));
        projetos.salvarSubtarefa(projeto, comSubtarefas, Subtarefa.nova("B", "", "", false, List.of()));
        List<String> iniciadas = new CopyOnWriteArrayList<>();
        agendador.aoMudar(new Agendador.Ouvinte() {
            @Override
            public void situacao(Agendador.Situacao situacao) {
            }

            @Override
            public void log(String projetoId, String linha) {
            }

            @Override
            public void taskIniciada(String projetoId, String taskId) {
                iniciadas.add(taskId);
            }
        });

        agendador.iniciar();
        esperarFilaVazia();

        assertEquals(3, agente.execucoes.size());
        assertEquals(List.of(comSubtarefas.id(), inteira.id()), iniciadas);
        assertFalse(agendador.executando());
    }

    @Test
    void perguntaParaATaskEmAmareloERetomaNaMesmaSessaoComAsRespostas() throws InterruptedException {
        Task task = Task.nova("Migração", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = projetos.salvarTask(projeto(), task);
        projetos.salvarSubtarefa(projeto, task, Subtarefa.nova("Integração", "Esta deve perguntar", "", false,
                List.of()));
        projetos.salvarSubtarefa(projeto, task, Subtarefa.nova("Sem dependência", "", "", false, List.of()));
        projetos.salvarTask(projetos.buscar(projeto.id()).orElseThrow(),
                Task.nova("Outra task", "", Provedor.AUTOMATICO, Esforco.MEDIUM));

        agendador.iniciar();
        esperarFilaVazia();

        Subtarefa parada = subtarefa(projeto, task, 0);
        assertEquals(Estado.ATENCAO, parada.estado());
        assertTrue(parada.aguardaDirecionamento());
        assertEquals("Direcionamento", parada.detalhe());
        assertEquals("Migrei 14 testes.", parada.resultado());
        assertEquals(List.of("Migrar o runner", "Remover os testes"),
                parada.direcionamento().perguntas().getFirst().opcoes());
        assertEquals(Estado.PENDENTE, subtarefa(projeto, task, 1).estado(), "a Task inteira espera a resposta");
        assertEquals(2, agente.execucoes.size(), "a fila seguiu para a outra Task");
        assertTrue(agendador.log(projeto.id()).stream().anyMatch(l -> l.contains("Aguardando direcionamento")));

        projetos.responder(projetos.buscar(projeto.id()).orElseThrow(), task, parada, List.of("Mantenha o runner"));
        agendador.agora();
        esperarFilaVazia();

        Execucao retomada = agente.execucoes.get(2);
        assertFalse(retomada.sessaoNova());
        assertEquals(agente.execucoes.getFirst().sessao(), retomada.sessao());
        assertTrue(retomada.prompt().startsWith("Resposta ao seu pedido de direcionamento:"));
        assertTrue(retomada.prompt().contains("Resposta: Mantenha o runner"));
        assertEquals(Estado.CONCLUIDO, subtarefa(projeto, task, 0).estado());
        assertNull(subtarefa(projeto, task, 0).direcionamento(), "as respostas já foram entregues");
        assertEquals(Estado.CONCLUIDO, subtarefa(projeto, task, 1).estado());
    }

    @Test
    void pausadoNaoExecuta() throws InterruptedException {
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Projeto projeto = projetos.salvarTask(projeto(), task);
        projetos.salvarSubtarefa(projeto, task, Subtarefa.nova("A", "", "", false, List.of()));

        agendador.agora();
        Thread.sleep(200);

        assertTrue(agente.execucoes.isEmpty());
        assertFalse(agendador.situacao().rodando());
    }
}
