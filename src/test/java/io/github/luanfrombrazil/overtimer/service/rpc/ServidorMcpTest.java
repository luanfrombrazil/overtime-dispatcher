package io.github.luanfrombrazil.overtimer.service.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.ProjetoService;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProjetos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServidorMcpTest {

    @TempDir
    Path dados;

    @TempDir
    Path pastaDoProjeto;

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private ProjetoService projetos;
    private ServidorMcp servidor;
    private Projeto projeto;
    private String tokenExterno;

    @BeforeEach
    void iniciar() throws IOException {
        int porta;
        try (ServerSocket livre = new ServerSocket(0)) {
            porta = livre.getLocalPort();
        }
        Files.writeString(dados.resolve("servidor-mcp.json"), "{\"porta\": " + porta + ", \"token\": \"segredo\"}");
        projetos = new ProjetoService(new RepositorioProjetos(dados));
        projeto = projetos.salvar(Projeto.novo("Meu App", "MA", "Um app", pastaDoProjeto.toString(), null,
                Provedor.CLAUDE_CODE, Esforco.HIGH, Permissoes.padrao()));
        servidor = new ServidorMcp(projetos, dados);
        servidor.iniciar();
        assertTrue(servidor.ativo(), () -> servidor.erro().orElse(""));
        tokenExterno = "segredo";
    }

    @AfterEach
    void parar() {
        servidor.close();
    }

    private HttpResponse<String> post(String token, String corpo) throws Exception {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(servidor.endereco()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(corpo));
        if (token != null) {
            pedido.header("Authorization", "Bearer " + token);
        }
        return http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode chamar(String token, String ferramenta, String argumentos) throws Exception {
        HttpResponse<String> resposta = post(token, """
                {"jsonrpc": "2.0", "id": 7, "method": "tools/call",
                 "params": {"name": "%s", "arguments": %s}}""".formatted(ferramenta, argumentos));
        assertEquals(200, resposta.statusCode());
        return json.readTree(resposta.body()).path("result");
    }

    private static String texto(JsonNode resultado) {
        return resultado.path("content").get(0).path("text").asText();
    }

    @Test
    void semTokenValidoNaoAtende() throws Exception {
        assertEquals(401, post(null, "{}").statusCode());
        assertEquals(401, post("errado", "{}").statusCode());
    }

    @Test
    void inicializaEListaAsFerramentas() throws Exception {
        JsonNode inicio = json.readTree(post(tokenExterno, """
                {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2025-06-18",
                 "capabilities": {}, "clientInfo": {"name": "teste", "version": "1"}}}""").body());
        assertEquals("2025-06-18", inicio.path("result").path("protocolVersion").asText());
        assertEquals("overtimer", inicio.path("result").path("serverInfo").path("name").asText());

        HttpResponse<String> notificacao = post(tokenExterno, """
                {"jsonrpc": "2.0", "method": "notifications/initialized"}""");
        assertEquals(202, notificacao.statusCode());

        JsonNode lista = json.readTree(post(tokenExterno, """
                {"jsonrpc": "2.0", "id": 2, "method": "tools/list"}""").body());
        List<String> nomes = new java.util.ArrayList<>();
        lista.path("result").path("tools").forEach(t -> nomes.add(t.path("name").asText()));
        assertEquals(List.of("listar_projetos", "listar_tasks", "sugerir_task", "sugerir_subtarefas"), nomes);
    }

    @Test
    void agenteExternoSugereTaskComSubtarefasEmQualquerProjeto() throws Exception {
        JsonNode resultado = chamar(tokenExterno, "sugerir_task", """
                {"projeto": "meu app", "titulo": "Login", "descricao": "Falta login", "esforco": "max",
                 "subtarefas": [
                   {"titulo": "Tela", "instrucao": "Crie a tela"},
                   {"titulo": "API", "instrucao": "Ligue na API", "requerVerificacao": true, "dependeDe": [0],
                    "esforco": "low"}
                 ]}""");
        assertFalse(resultado.path("isError").asBoolean(), texto(resultado));

        Task task = projetos.buscar(projeto.id()).orElseThrow().tasks().getFirst();
        assertTrue(task.sugerida());
        assertEquals("Login", task.titulo());
        assertEquals(Esforco.MAX, task.esforco());
        Subtarefa tela = task.subtarefas().get(0);
        Subtarefa api = task.subtarefas().get(1);
        assertEquals(List.of(tela.id()), api.dependeDe());
        assertTrue(api.requerVerificacao());
        assertEquals(Esforco.LOW, api.esforco());
        assertEquals(Estado.SUGERIDA, api.estado());
        assertTrue(projetos.proxima().isEmpty());
    }

    @Test
    void agenteExternoPrecisaDizerOProjeto() throws Exception {
        JsonNode resultado = chamar(tokenExterno, "sugerir_task", """
                {"titulo": "Login", "descricao": ""}""");
        assertTrue(resultado.path("isError").asBoolean());
        assertTrue(texto(resultado).contains("listar_projetos"), texto(resultado));
    }

    @Test
    void execucaoUsaOProprioProjetoEPerdeOAcessoAoTerminar() throws Exception {
        String token;
        Path config;
        try (ServidorMcp.Sessao sessao = servidor.abrir(projeto.id()).orElseThrow()) {
            config = sessao.config();
            JsonNode arquivo = json.readTree(config.toFile()).path("mcpServers").path("overtimer");
            assertEquals("http", arquivo.path("type").asText());
            assertEquals(servidor.endereco(), arquivo.path("url").asText());
            Matcher m = Pattern.compile("Bearer (.+)").matcher(arquivo.path("headers").path("Authorization").asText());
            assertTrue(m.matches());
            token = m.group(1);

            JsonNode resultado = chamar(token, "sugerir_task", """
                    {"titulo": "Sem projeto explícito", "descricao": ""}""");
            assertFalse(resultado.path("isError").asBoolean(), texto(resultado));
        }
        assertEquals("Sem projeto explícito", projetos.buscar(projeto.id()).orElseThrow().tasks().getFirst().titulo());
        assertFalse(Files.exists(config), "o arquivo com o token é apagado");
        assertEquals(401, post(token, "{}").statusCode());
    }

    @Test
    void sugereSubtarefasNumaTaskExistenteEListaAsTasks() throws Exception {
        Task task = Task.nova("Existente", "", Provedor.CLAUDE_CODE, Esforco.LOW);
        projetos.salvarTask(projeto, task);
        projetos.salvarSubtarefa(projetos.buscar(projeto.id()).orElseThrow(), task,
                Subtarefa.nova("A", "", "", false, List.of()));
        String a = projetos.buscar(projeto.id()).orElseThrow().tasks().getFirst().subtarefas().getFirst().id();

        JsonNode resultado = chamar(tokenExterno, "sugerir_subtarefas", """
                {"projeto": "%s", "task": "%s", "subtarefas": [
                  {"titulo": "B", "instrucao": "Faça B", "dependeDeExistentes": ["%s"]}]}"""
                .formatted(projeto.id(), task.id(), a));
        assertFalse(resultado.path("isError").asBoolean(), texto(resultado));

        JsonNode lista = json.readTree(texto(chamar(tokenExterno, "listar_tasks",
                "{\"projeto\": \"" + projeto.id() + "\"}")));
        JsonNode existente = lista.path("tasks").get(0);
        assertTrue(existente.path("sugerida").asBoolean());
        assertEquals("sugerida", existente.path("subtarefas").get(1).path("estado").asText());

        JsonNode dependenciaErrada = chamar(tokenExterno, "sugerir_subtarefas", """
                {"projeto": "%s", "task": "%s", "subtarefas": [
                  {"titulo": "C", "instrucao": "", "dependeDeExistentes": ["nao-existe"]}]}"""
                .formatted(projeto.id(), task.id()));
        assertTrue(dependenciaErrada.path("isError").asBoolean());
    }

    @Test
    void recusaPedidosDeOutrasOrigens() throws Exception {
        assertTrue(ServidorMcp.origemLocal(null));
        assertTrue(ServidorMcp.origemLocal("http://localhost:5173"));
        assertFalse(ServidorMcp.origemLocal("https://exemplo.com"));
        HttpResponse<String> resposta = http.send(HttpRequest.newBuilder(URI.create(servidor.endereco()))
                .header("Authorization", "Bearer " + tokenExterno)
                .header("Origin", "https://exemplo.com")
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, resposta.statusCode());
    }

    @Test
    void comandoDoClaudeCodeLevaOTokenExterno() {
        assertEquals("claude mcp add --transport http overtimer " + servidor.endereco()
                + " --header \"Authorization: Bearer segredo\"", servidor.comandoClaudeCode());
    }
}
