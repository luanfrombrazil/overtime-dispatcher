package io.github.luanfrombrazil.overtimedispatcher.service.agente;

import io.github.luanfrombrazil.overtimedispatcher.model.Esforco;
import io.github.luanfrombrazil.overtimedispatcher.model.Permissoes;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import io.github.luanfrombrazil.overtimedispatcher.model.Provedor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaudeCodeAgenteTest {

    private static Execucao execucao(boolean sessaoNova, Permissoes permissoes, boolean usaGit) {
        return new Execucao(Path.of("."), "prompt", "abc", sessaoNova, Esforco.XHIGH, permissoes, usaGit,
                Duration.ofMinutes(1));
    }

    @Test
    void primeiraExecucaoCriaASessaoEAsSeguintesRetomam() {
        List<String> nova = ClaudeCodeAgente.linhaDeComando(List.of("claude"), execucao(true, Permissoes.padrao(), false));
        List<String> seguinte = ClaudeCodeAgente.linhaDeComando(List.of("claude"), execucao(false, Permissoes.padrao(), false));

        assertEquals("claude", nova.getFirst());
        assertTrue(nova.containsAll(List.of("-p", "--output-format", "stream-json", "--verbose", "dontAsk")));
        assertEquals("abc", nova.get(nova.indexOf("--session-id") + 1));
        assertEquals("abc", seguinte.get(seguinte.indexOf("--resume") + 1));
        assertEquals("xhigh", nova.get(nova.indexOf("--effort") + 1));
    }

    @Test
    void permissoesViramFerramentasLiberadas() {
        Permissoes soLeitura = new Permissoes(false, false, false, false);
        List<String> semGit = ClaudeCodeAgente.ferramentas(soLeitura, false);
        List<String> comGit = ClaudeCodeAgente.ferramentas(soLeitura, true);
        List<String> tudo = ClaudeCodeAgente.ferramentas(new Permissoes(true, true, true, true), true);

        assertFalse(semGit.contains("Edit"));
        assertFalse(semGit.stream().anyMatch(f -> f.startsWith("Bash")));
        assertTrue(comGit.contains("Bash(git *)"), "o agente precisa fazer commit na branch da task");
        assertTrue(tudo.containsAll(List.of("Edit", "Write", "Bash", "WebFetch")));
    }

    @Test
    void leitorTransformaEventosEmLogEGuardaOResultado() {
        List<String> log = new ArrayList<>();
        LeitorClaudeCode leitor = new LeitorClaudeCode(log::add);
        leitor.processar("{\"type\":\"system\",\"subtype\":\"init\",\"session_id\":\"s-1\"}");
        leitor.processar("{\"type\":\"assistant\",\"message\":{\"content\":[{\"type\":\"text\",\"text\":\"Vou ler.\\nDepois editar.\"},"
                + "{\"type\":\"tool_use\",\"name\":\"Edit\",\"input\":{\"file_path\":\"C:\\\\code\\\\Login.java\"}},"
                + "{\"type\":\"tool_use\",\"name\":\"Bash\",\"input\":{\"command\":\"mvn test\"}}]}}");
        leitor.processar("não é json");
        leitor.processar("{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":false,\"result\":\"Feito.\","
                + "\"session_id\":\"s-1\",\"permission_denials\":[{\"tool_name\":\"WebFetch\"}]}");

        assertEquals(List.of("Vou ler.", "› Edit Login.java", "› Bash mvn test", "não é json", "Negado: WebFetch"), log);
        assertTrue(leitor.terminou());
        assertFalse(leitor.erro());
        assertEquals("Feito.", leitor.resposta());
        assertEquals("s-1", leitor.sessao());
    }

    @Test
    void leitorReconheceErro() {
        LeitorClaudeCode leitor = new LeitorClaudeCode(l -> {
        });
        leitor.processar("{\"type\":\"result\",\"subtype\":\"error_max_turns\",\"is_error\":true}");

        assertTrue(leitor.erro());
        assertEquals("error_max_turns", leitor.resposta());
    }

    @Test
    void branchDaTaskSemAcentosComIdCurto() {
        Task task = new Task("1a2b3c4d-0000-0000-0000-000000000000", "Corrigir Ação de LOGIN!", "", Provedor.AUTOMATICO,
                Esforco.MEDIUM, null, List.of(), null, null, null);

        assertEquals("overtime/corrigir-acao-de-login-1a2b3c4d", AreaDeTrabalho.branch(task));
    }
}
