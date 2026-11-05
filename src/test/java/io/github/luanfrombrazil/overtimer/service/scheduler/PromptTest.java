package io.github.luanfrombrazil.overtimer.service.scheduler;

import io.github.luanfrombrazil.overtimer.model.Direcionamento;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptTest {

    @Test
    void leOPedidoComResumoPerguntasEOpcoes() {
        String resposta = """
                Migrei 14 dos 20 testes.
                Parei nos de integração.

                **DIRECIONAMENTO:**
                Pergunta: Os testes de integração usam um runner próprio.
                O que faço com eles?
                - Migrar o runner para uma extensão do JUnit 5
                - Remover esses testes
                  desta Task
                **Pergunta 2:** Atualizo o Surefire?
                1. Sim
                2) Não
                """;

        Direcionamento d = Prompt.pedido(resposta).orElseThrow().direcionamento();

        assertEquals("Migrei 14 dos 20 testes.\nParei nos de integração.", d.resumo());
        assertEquals(2, d.perguntas().size());
        Direcionamento.Pergunta primeira = d.perguntas().getFirst();
        assertEquals("Os testes de integração usam um runner próprio. O que faço com eles?", primeira.texto());
        assertEquals(List.of("Migrar o runner para uma extensão do JUnit 5", "Remover esses testes desta Task"),
                primeira.opcoes());
        assertNull(primeira.resposta());
        assertEquals("Atualizo o Surefire?", d.perguntas().get(1).texto());
        assertEquals(List.of("Sim", "Não"), d.perguntas().get(1).opcoes());
        assertFalse(d.respondido());
    }

    @Test
    void perguntaSemOpcoesSoTemOOutro() {
        Direcionamento d = Prompt.pedido("Feito em parte.\nDIRECIONAMENTO:\nQual o nome do pacote?").orElseThrow()
                .direcionamento();

        assertEquals("Qual o nome do pacote?", d.perguntas().getFirst().texto());
        assertTrue(d.perguntas().getFirst().opcoes().isEmpty());
    }

    @Test
    void semBlocoOuSemPerguntaNaoEPedido() {
        assertTrue(Prompt.pedido("Pronto. Fiz tudo.").isEmpty());
        assertTrue(Prompt.pedido("Pronto.\nDIRECIONAMENTO:\n").isEmpty());
        assertTrue(Prompt.pedido(null).isEmpty());
    }

    @Test
    void pedidoEnsinaQuandoEComoPerguntar() {
        Projeto projeto = Projeto.novo("P", "P", "", "", null, Provedor.AUTOMATICO, Esforco.LOW, Permissoes.padrao());
        Task task = Task.nova("T", "", Provedor.AUTOMATICO, Esforco.LOW);

        String prompt = Prompt.montar(projeto, task, null, "b");

        assertFalse(prompt.contains("Ninguém vai responder"));
        assertTrue(prompt.contains(Prompt.MARCA_DE_DIRECIONAMENTO + "\nPergunta:"));
        assertFalse(prompt.contains("Direcionamento já dado"));
    }

    @Test
    void respostasVaoNaRetomadaOuNoPedidoCompletoSemSessao() {
        Projeto projeto = Projeto.novo("P", "P", "", "", null, Provedor.AUTOMATICO, Esforco.LOW, Permissoes.padrao());
        Direcionamento respondido = new Direcionamento("Parei.", List.of(
                new Direcionamento.Pergunta("Runner?", List.of("Migrar", "Remover"), null)))
                .comRespostas(List.of("Manter o runner"));
        Subtarefa s = Subtarefa.nova("Integração", "Migre", "", false, List.of()).comDirecionamento(respondido);
        Task task = Task.nova("Migração", "", Provedor.AUTOMATICO, Esforco.LOW).comSubtarefas(List.of(s));

        String retomada = Prompt.continuar(task, s, respondido);
        String completo = Prompt.montar(projeto, task, s, "b");

        assertTrue(retomada.startsWith("Resposta ao seu pedido de direcionamento:"));
        assertTrue(retomada.contains("Pergunta: Runner?\nResposta: Manter o runner"));
        assertTrue(retomada.contains("a subtarefa \"Integração\""));
        assertTrue(retomada.contains(Prompt.MARCA_DE_FALHA));
        assertTrue(completo.contains("## Direcionamento já dado\nPergunta: Runner?\nResposta: Manter o runner"));
    }
}
