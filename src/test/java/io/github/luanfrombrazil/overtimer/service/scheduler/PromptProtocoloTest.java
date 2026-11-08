package io.github.luanfrombrazil.overtimer.service.scheduler;

import io.github.luanfrombrazil.overtimer.model.Acessos;
import io.github.luanfrombrazil.overtimer.model.Agenda;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.ItemFila;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptProtocoloTest {

    private static final Function<String, String> NOMES =
            id -> Map.of("id-email", "E-mail", "id-projeto", "Overtimer").getOrDefault(id, id);

    private static Protocolo protocolo(Acessos acessos, List<ItemFila> fila) {
        return Protocolo.novo("Faculdade", "FA", "1. Entrar no portal\n2. Listar as entregas da semana",
                Agenda.aCada(60), Provedor.CLAUDE_CODE, Esforco.MEDIUM, acessos, List.of())
                .comFilas(fila, List.of());
    }

    @Test
    void levaOPassoAPasso() {
        String prompt = PromptProtocolo.montar(protocolo(Acessos.nenhum(), List.of()), NOMES);

        assertTrue(prompt.contains("\"Faculdade\""));
        assertTrue(prompt.contains("## Passo a passo\n1. Entrar no portal\n2. Listar as entregas da semana"));
        assertTrue(prompt.contains(Prompt.MARCA_DE_FALHA));
    }

    @Test
    void levaAFilaDeEntradaComAOrigem() {
        List<ItemFila> fila = List.of(
                new ItemFila("1", "Marcar a prova", "Prova de cálculo\nsexta às 10h", "id-email"),
                new ItemFila("2", "Revisar notas", null, "id-projeto"));
        String prompt = PromptProtocolo.montar(protocolo(Acessos.nenhum(), fila), NOMES);

        assertTrue(prompt.contains("## Fila de entrada"));
        assertTrue(prompt.contains("- Marcar a prova (de E-mail): Prova de cálculo\n  sexta às 10h\n"));
        assertTrue(prompt.contains("- Revisar notas (de Overtimer)\n"));
    }

    @Test
    void semFilaNaoTemASecao() {
        assertFalse(PromptProtocolo.montar(protocolo(Acessos.nenhum(), List.of()), NOMES).contains("Fila de entrada"));
    }

    @Test
    void semAcessoProibeTudoForaDaPasta() {
        String regras = PromptProtocolo.regras(Acessos.nenhum());

        assertTrue(regras.contains("só dentro dela"));
        assertTrue(regras.contains("Não acesse sites."));
        assertTrue(regras.contains("Não acesse e-mail."));
        assertTrue(regras.contains("Não acesse o calendário."));
    }

    @Test
    void sitesSoLeitura() {
        String regras = PromptProtocolo.regras(new Acessos(List.of("https://portal.exemplo.edu"), false, false));

        assertTrue(regras.contains("Sites, só leitura"));
        assertTrue(regras.contains("https://portal.exemplo.edu"));
        assertTrue(regras.contains("não faça login"));
        assertFalse(regras.contains("Não acesse sites."));
    }

    @Test
    void emailSoLeituraERascunho() {
        String regras = PromptProtocolo.regras(new Acessos(List.of(), true, false, "claude_ai_Gmail", null));

        assertTrue(regras.contains("E-mail, só leitura e rascunho"));
        assertTrue(regras.contains("`claude_ai_Gmail`"));
        assertTrue(regras.contains("Nunca envie"));
        assertTrue(regras.contains("Não acesse o calendário."));
    }

    @Test
    void calendarioSoAAgendaDoAgente() {
        String regras = PromptProtocolo.regras(
                new Acessos(List.of(), false, true, null, "claude_ai_Google_Calendar"));

        assertTrue(regras.contains("Calendário, só a agenda do agente"));
        assertTrue(regras.contains("\"" + PromptProtocolo.AGENDA_DO_AGENTE + "\""));
        assertTrue(regras.contains("outras agendas"));
        assertTrue(regras.contains("Não acesse e-mail."));
    }

    @Test
    void acessoMarcadoSemConectorContaComoDesligado() {
        String regras = PromptProtocolo.regras(new Acessos(List.of(), true, true));

        assertTrue(regras.contains("Não acesse e-mail."));
        assertTrue(regras.contains("Não acesse o calendário."));
    }
}
