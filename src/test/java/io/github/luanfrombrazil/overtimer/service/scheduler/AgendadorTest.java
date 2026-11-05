package io.github.luanfrombrazil.overtimer.service.scheduler;

import io.github.luanfrombrazil.overtimer.model.Configuracoes;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Aviso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Notificacoes;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Modelo;
import io.github.luanfrombrazil.overtimer.model.Notificacao;
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
import io.github.luanfrombrazil.overtimer.service.comunicacao.NotificacaoService;
import io.github.luanfrombrazil.overtimer.service.persistence.NotificacoesRepositorio;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProjetos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
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
        Task task = Task.nova("Corrigir login", "", Provedor.AUTOMATICO, Esforco.LOW);
        Projeto projeto = projetos.salvarTask(projeto(), task);
        // O esforço da subtarefa vale sobre o da Task.
        Subtarefa a = Subtarefa.nova("Reproduzir", "Escreva o teste", "Falha", false, List.of())
                .comModelo(Esforco.HIGH, Modelo.OPUS);
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
        assertEquals(Modelo.OPUS, primeira.modelo());
        assertEquals(pastaDoProjeto, primeira.pasta());
        assertTrue(primeira.prompt().contains("Escreva o teste"));
        assertTrue(segunda.prompt().contains("- Reproduzir"), "a segunda sabe o que já foi feito");
        assertEquals(Estado.CONCLUIDO, subtarefa(projeto, task, 1).estado());
        assertEquals(primeira.sessao(), projetos.buscar(projeto.id()).orElseThrow().tasks().getFirst().sessao());
        assertTrue(agendador.log(projeto.id()).stream().anyMatch(l -> l.contains("› Edit arquivo.txt")));
    }

    @Test
    void naoComecaTaskComOProvedorNoLimiteDeUso() throws InterruptedException {
        AtomicInteger uso = new AtomicInteger(95);
        agendador.usarConsumo(p -> uso.get());
        agendador.aplicar(Configuracoes.padrao()); // automação até 90%
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        projetos.salvarTask(projeto(), task);

        agendador.iniciar();
        Thread.sleep(300);

        assertEquals(0, agente.execucoes.size());
        assertTrue(agendador.situacao().texto().contains("limite de uso"), agendador.situacao().texto());

        uso.set(40);
        agendador.agora();
        esperarFilaVazia();

        assertEquals(1, agente.execucoes.size());
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
    void avisaVerificacaoDirecionamentoEFalhaComProjetoETask() throws InterruptedException {
        NotificacaoService notificacoes = new NotificacaoService(new NotificacoesRepositorio(dados),
                new Notificacoes(false, false, LocalTime.of(23, 0), LocalTime.of(7, 0), true, true, false, false,
                        EnumSet.of(Aviso.VERIFICACAO, Aviso.DIRECIONAMENTO, Aviso.FALHAS)));
        agendador.usarNotificacoes(notificacoes);
        Projeto projeto = projeto();
        Task verificar = Task.nova("Verificar", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Task perguntar = Task.nova("Perguntar", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Task falhar = Task.nova("Falhar", "Esta deve falhar", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Task concluir = Task.nova("Concluir", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        projeto = projetos.salvarTask(projeto, verificar);
        projeto = projetos.salvarTask(projeto, perguntar);
        projeto = projetos.salvarTask(projeto, falhar);
        projeto = projetos.salvarTask(projeto, concluir);
        projetos.salvarSubtarefa(projeto, verificar, Subtarefa.nova("Olhar", "", "", true, List.of()));
        projetos.salvarSubtarefa(projetos.buscar(projeto.id()).orElseThrow(), perguntar,
                Subtarefa.nova("Integração", "Esta deve perguntar", "", false, List.of()));

        agendador.iniciar();
        esperarFilaVazia();

        List<Notificacao> registradas = notificacoes.listar();
        assertEquals(3, registradas.size(), registradas.toString());
        assertNotificacao(registradas, Aviso.VERIFICACAO, projeto, verificar);
        Notificacao direcionamento = assertNotificacao(registradas, Aviso.DIRECIONAMENTO, projeto, perguntar);
        assertTrue(direcionamento.texto().contains("O que faço com o runner?"), direcionamento.texto());
        // A Task executada inteira também avisa.
        Notificacao falha = assertNotificacao(registradas, Aviso.FALHAS, projeto, falhar);
        assertTrue(falha.texto().contains("FALHOU: não deu"), falha.texto());
    }

    @Test
    void avisaOLimiteDeUsoUmaVezPorEntrada() throws InterruptedException {
        NotificacaoService notificacoes = new NotificacaoService(new NotificacoesRepositorio(dados),
                new Notificacoes(false, false, LocalTime.of(23, 0), LocalTime.of(7, 0), true, true, false, false,
                        EnumSet.of(Aviso.LIMITE_DE_USO)));
        AtomicInteger uso = new AtomicInteger(95);
        agendador.usarConsumo(p -> uso.get());
        agendador.aplicar(Configuracoes.padrao()); // automação até 90%
        // Depois do aplicar, que troca os avisos marcados pelos das configurações.
        agendador.usarNotificacoes(notificacoes);
        Projeto projeto = projetos.salvarTask(projeto(), Task.nova("T1", "", Provedor.CLAUDE_CODE, Esforco.MEDIUM));

        agendador.iniciar();
        Thread.sleep(200);
        // Vários ciclos com o provedor ainda no limite: não repete o aviso.
        agendador.agora();
        agendador.agora();
        Thread.sleep(300);
        assertEquals(1, limitesAvisados(notificacoes));
        Notificacao aviso = notificacoes.listar().getFirst();
        assertTrue(aviso.titulo().contains(Provedor.CLAUDE_CODE.toString()), aviso.titulo());
        assertNull(aviso.projetoId());

        // Sai do limite e roda; depois entra de novo com outra Task esperando: avisa outra vez.
        uso.set(40);
        agendador.agora();
        esperarFilaVazia();
        assertEquals(1, agente.execucoes.size());
        projetos.salvarTask(projetos.buscar(projeto.id()).orElseThrow(),
                Task.nova("T2", "", Provedor.CLAUDE_CODE, Esforco.MEDIUM));
        uso.set(95);
        agendador.pausar();
        agendador.iniciar();
        Thread.sleep(300);
        agendador.agora();
        Thread.sleep(200);

        assertEquals(2, limitesAvisados(notificacoes));
        assertEquals(1, agente.execucoes.size());
    }

    private static long limitesAvisados(NotificacaoService notificacoes) {
        return notificacoes.listar().stream().filter(n -> n.tipo() == Aviso.LIMITE_DE_USO).count();
    }

    @Test
    void avisaUmaVezPorTaskFinalizada() throws InterruptedException {
        NotificacaoService notificacoes = new NotificacaoService(new NotificacoesRepositorio(dados),
                new Notificacoes(false, false, LocalTime.of(23, 0), LocalTime.of(7, 0), true, true, false, false,
                        EnumSet.of(Aviso.SESSAO_CONCLUIDA)));
        agendador.usarNotificacoes(notificacoes);
        Projeto projeto = projeto();
        Task duas = Task.nova("Duas subtarefas", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Task inteira = Task.nova("Inteira", "Faça", Provedor.AUTOMATICO, Esforco.MEDIUM);
        Task verificar = Task.nova("Com verificação", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        projeto = projetos.salvarTask(projeto, duas);
        projeto = projetos.salvarTask(projeto, inteira);
        projeto = projetos.salvarTask(projeto, verificar);
        projeto = projetos.salvarSubtarefa(projeto, duas, Subtarefa.nova("A", "", "", false, List.of()));
        projeto = projetos.salvarSubtarefa(projeto, duas, Subtarefa.nova("B", "", "", false, List.of()));
        projetos.salvarSubtarefa(projeto, verificar, Subtarefa.nova("Olhar", "", "", true, List.of()));

        agendador.iniciar();
        esperarFilaVazia();

        List<Notificacao> concluidas = notificacoes.listar().stream()
                .filter(n -> n.tipo() == Aviso.SESSAO_CONCLUIDA).toList();
        assertEquals(2, concluidas.size(), concluidas.toString());
        assertEquals(1, concluidas.stream().filter(n -> n.taskId().equals(duas.id())).count());
        assertEquals(1, concluidas.stream().filter(n -> n.taskId().equals(inteira.id())).count());
        String projetoId = projeto.id();
        assertTrue(concluidas.stream().allMatch(n -> projetoId.equals(n.projetoId())));
    }

    @Test
    void semAgenteAvisaFalha() throws InterruptedException {
        NotificacaoService notificacoes = new NotificacaoService(new NotificacoesRepositorio(dados),
                Notificacoes.padrao());
        agendador.usarNotificacoes(notificacoes);
        Task codex = Task.nova("Codex", "", Provedor.CODEX, Esforco.MEDIUM);
        Projeto projeto = projetos.salvarTask(projeto(), codex);

        agendador.iniciar();
        esperarFilaVazia();

        assertNotificacao(notificacoes.listar(), Aviso.FALHAS, projeto, codex);
    }

    private static Notificacao assertNotificacao(List<Notificacao> notificacoes, Aviso tipo, Projeto projeto,
                                                 Task task) {
        Notificacao encontrada = notificacoes.stream().filter(n -> n.tipo() == tipo).findFirst()
                .orElseThrow(() -> new AssertionError("sem notificação " + tipo + " em " + notificacoes));
        assertEquals(projeto.id(), encontrada.projetoId());
        assertEquals(task.id(), encontrada.taskId());
        assertTrue(encontrada.texto().startsWith(projeto.nome() + " · " + task.titulo()), encontrada.texto());
        assertFalse(encontrada.lida());
        return encontrada;
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
