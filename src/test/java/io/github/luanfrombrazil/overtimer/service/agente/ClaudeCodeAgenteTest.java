package io.github.luanfrombrazil.overtimer.service.agente;

import io.github.luanfrombrazil.overtimer.model.Acessos;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Modelo;
import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaudeCodeAgenteTest {

    private static Execucao execucao(boolean sessaoNova, Permissoes permissoes, boolean usaGit) {
        return new Execucao(Path.of("."), "prompt", "abc", sessaoNova, Esforco.XHIGH, Modelo.PADRAO, permissoes, usaGit,
                false, Duration.ofMinutes(1));
    }

    @Test
    void primeiraExecucaoCriaASessaoEAsSeguintesRetomam() {
        List<String> nova = ClaudeCodeAgente.linhaDeComando(List.of("claude"), execucao(true, Permissoes.padrao(), false));
        List<String> seguinte = ClaudeCodeAgente.linhaDeComando(List.of("claude"), execucao(false, Permissoes.padrao(), false));

        assertEquals("claude", nova.getFirst());
        assertTrue(nova.containsAll(List.of("-p", "--output-format", "stream-json", "--verbose")));
        assertFalse(nova.contains("--permission-mode"), "o modo de permissão fica o padrão do CLI");
        assertEquals("abc", nova.get(nova.indexOf("--session-id") + 1));
        assertEquals("abc", seguinte.get(seguinte.indexOf("--resume") + 1));
        assertEquals("xhigh", nova.get(nova.indexOf("--effort") + 1));
        assertFalse(nova.contains("--model"), "o modelo padrão fica a cargo do agente");
    }

    @Test
    void modeloEscolhidoVaiParaOAgente() {
        Execucao comModelo = new Execucao(Path.of("."), "prompt", "abc", true, Esforco.MEDIUM, Modelo.OPUS,
                Permissoes.padrao(), false, false, Duration.ofMinutes(1));
        List<String> linha = ClaudeCodeAgente.linhaDeComando(List.of("claude"), comModelo);

        assertEquals("opus", linha.get(linha.indexOf("--model") + 1));
    }

    @Test
    void permissoesViramFerramentasLiberadas() {
        Permissoes soLeitura = new Permissoes(false, false, false, false);
        List<String> semGit = ClaudeCodeAgente.ferramentas(soLeitura, false, false);
        List<String> comGit = ClaudeCodeAgente.ferramentas(soLeitura, true, false);
        List<String> comPush = ClaudeCodeAgente.ferramentas(soLeitura, true, true);
        List<String> tudo = ClaudeCodeAgente.ferramentas(new Permissoes(true, true, true, true), true, false);

        assertFalse(semGit.stream().anyMatch(f -> f.startsWith("Edit")));
        assertFalse(semGit.stream().anyMatch(f -> f.startsWith("Bash")));
        assertTrue(comGit.contains("Bash(git commit *)"), "o agente precisa fazer commit na branch da task");
        assertFalse(comGit.contains("Bash(git *)"), "git genérico deixaria passar git config/-c");
        assertFalse(comGit.stream().anyMatch(f -> f.contains("git push")));
        assertTrue(comPush.contains("Bash(git push *)"));
        assertTrue(tudo.containsAll(List.of("Edit(./**)", "Write(./**)", "Bash", "WebFetch")));
        assertFalse(tudo.contains("Edit"), "a edição fica presa à pasta de trabalho");
        assertTrue(tudo.stream().noneMatch(f -> f.startsWith("mcp__")), "nenhum conector sem liberar");
    }

    @Test
    void conectoresLiberadosViramRegrasDeServidorMcp() {
        Permissoes comFigma = new Permissoes(false, false, false, false, List.of("claude_ai_Figma"));

        assertTrue(ClaudeCodeAgente.ferramentas(comFigma, false, false).contains("mcp__claude_ai_Figma"));
    }

    @Test
    void protocoloSemAcessoSoLeEEditaAPastaDeTrabalho() {
        List<String> ferramentas = ClaudeCodeAgente.ferramentas(Acessos.nenhum());

        assertEquals(List.of("Read(./**)", "Glob", "Grep", "TodoWrite", "Edit(./**)", "Write(./**)"), ferramentas);
        assertFalse(ferramentas.contains("Read"), "a leitura fica presa à pasta de trabalho");
        assertTrue(ferramentas.stream().noneMatch(f -> f.startsWith("Bash") || f.startsWith("PowerShell")));
        assertTrue(ferramentas.stream().noneMatch(f -> f.startsWith("Web") || f.startsWith("mcp__")));
    }

    @Test
    void sitesLiberamOWebFetchSoNosDominiosDeles() {
        Acessos sites = new Acessos(List.of("https://Portal.Exemplo.edu/aluno", "portal.exemplo.edu",
                "outro.com.br", "não é site"), false, false);
        List<String> ferramentas = ClaudeCodeAgente.ferramentas(sites);

        assertTrue(ferramentas.containsAll(List.of("WebFetch(domain:portal.exemplo.edu)",
                "WebFetch(domain:outro.com.br)")));
        assertEquals(2, ferramentas.stream().filter(f -> f.startsWith("WebFetch")).count());
        assertFalse(ferramentas.contains("WebFetch"), "sem WebFetch genérico");
        assertFalse(ferramentas.contains("WebSearch"));
    }

    @Test
    void emailECalendarioLiberamOConectorEscolhido() {
        Acessos acessos = new Acessos(List.of(), true, true, "mcp__claude_ai_Gmail", "claude_ai_Google_Calendar");
        List<String> ferramentas = ClaudeCodeAgente.ferramentas(acessos);

        assertTrue(ferramentas.containsAll(List.of("mcp__claude_ai_Gmail", "mcp__claude_ai_Google_Calendar")));
        assertTrue(ferramentas.stream().noneMatch(f -> f.startsWith("Bash")));
    }

    @Test
    void conectorDeAcessoDesmarcadoFicaDeFora() {
        Acessos desmarcado = new Acessos(List.of(), false, false, "claude_ai_Gmail", "claude_ai_Google_Calendar");
        Acessos semConector = new Acessos(List.of(), true, true);

        assertTrue(ClaudeCodeAgente.ferramentas(desmarcado).stream().noneMatch(f -> f.startsWith("mcp__")));
        assertTrue(ClaudeCodeAgente.ferramentas(semConector).stream().noneMatch(f -> f.startsWith("mcp__")));
        assertThrows(IllegalArgumentException.class,
                () -> new Acessos(List.of(), true, false, "Bash(rm *)", null));
    }

    @Test
    void execucaoDoProtocoloUsaOsAcessosESemGit() {
        Execucao execucao = Execucao.doProtocolo(Path.of("."), "prompt", "abc", true, Esforco.MEDIUM,
                new Acessos(List.of("exemplo.com"), true, false, "claude_ai_Gmail", null), Duration.ofMinutes(1));
        List<String> linha = ClaudeCodeAgente.linhaDeComando(List.of("claude"), execucao);
        String liberadas = linha.get(linha.indexOf("--allowedTools") + 1);
        String bloqueadas = linha.get(linha.indexOf("--disallowedTools") + 1);

        assertTrue(liberadas.contains("WebFetch(domain:exemplo.com)"));
        assertTrue(liberadas.contains("mcp__claude_ai_Gmail"));
        assertFalse(liberadas.contains("git "), "a pasta de trabalho não é um repositório");
        assertTrue(bloqueadas.contains("Read(~/.ssh/**)"), "credenciais ficam fora também no protocolo");
        assertTrue(bloqueadas.contains("mcp__claude_ai_Gmail__send_message"), "o e-mail não envia");
    }

    @Test
    void emailSoLeECriaRascunhos() {
        Acessos acessos = new Acessos(List.of(), true, true, "claude_ai_Gmail", "claude_ai_Google_Calendar");
        List<String> bloqueadas = ClaudeCodeAgente.bloqueadas(acessos);

        assertTrue(bloqueadas.containsAll(List.of("mcp__claude_ai_Gmail__send_message",
                "mcp__claude_ai_Gmail__reply", "mcp__claude_ai_Gmail__forward",
                "mcp__claude_ai_Gmail__trash_thread", "mcp__claude_ai_Gmail__update_draft",
                "mcp__claude_ai_Gmail__delete_draft", "mcp__claude_ai_Gmail__create_filter")));
        assertFalse(bloqueadas.contains("mcp__claude_ai_Gmail__create_draft"), "rascunho novo é permitido");
        assertFalse(bloqueadas.contains("mcp__claude_ai_Gmail__get_message"), "ler é permitido");
        assertTrue(bloqueadas.stream().allMatch(r -> r.startsWith("mcp__claude_ai_Gmail__")),
                "o calendário não é afetado");
        assertEquals(List.of(), ClaudeCodeAgente.bloqueadas(
                new Acessos(List.of(), false, false, "claude_ai_Gmail", null)), "sem e-mail, nada a bloquear");
    }

    @Test
    void conectoresDigitadosSaoLidosESoAceitamNomesDeServidor() {
        assertEquals(List.of("claude_ai_Figma", "github"),
                Permissoes.conectoresDe(" mcp__claude_ai_Figma, github\nclaude_ai_Figma "));
        assertEquals(List.of(), Permissoes.conectoresDe("  "));
        assertThrows(IllegalArgumentException.class,
                () -> new Permissoes(true, true, false, false, List.of("Bash(rm *)")));
        assertEquals(List.of(), new Permissoes(true, true, false, false, null).conectores(),
                "projetos salvos antes dos conectores");
    }

    @Test
    void bloqueiosProtegemCredenciaisEABranchDaTask() {
        List<String> semPush = ClaudeCodeAgente.bloqueadas(true, false);
        List<String> comPush = ClaudeCodeAgente.bloqueadas(true, true);
        List<String> semGit = ClaudeCodeAgente.bloqueadas(false, false);

        assertTrue(semPush.containsAll(List.of("Read(~/.ssh/**)", "Bash(git push*)", "Bash(git checkout*)")));
        assertTrue(comPush.contains("Read(~/.ssh/**)"), "credenciais ficam fora mesmo com tudo liberado");
        assertTrue(comPush.contains("Bash(git push --force*)"));
        assertFalse(comPush.contains("Bash(git push*)"));
        assertTrue(semGit.stream().noneMatch(r -> r.contains("(git ")), "sem git, só as credenciais");
    }

    @Test
    void linhaDeComandoCabeNoLimiteDoCmd() {
        List<String> linha = ClaudeCodeAgente.linhaDeComando(List.of("cmd.exe", "/c", "claude.cmd"),
                execucao(true, new Permissoes(true, false, false, false), true));

        assertTrue(String.join(" ", linha).length() < 8000);
        assertTrue(linha.stream().noneMatch(a -> a.matches(".*[\"%&|<>^].*")),
                "nada que o cmd.exe interprete");
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
                Esforco.MEDIUM, null, List.of(), null, null, null, null);

        assertEquals("overtime/corrigir-acao-de-login-1a2b3c4d", AreaDeTrabalho.branch(task));
    }
}
