package io.github.luanfrombrazil.overtimer.service.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.luanfrombrazil.overtimer.service.ProjetoService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor MCP da app, por HTTP (Streamable HTTP, só com respostas JSON) em {@code 127.0.0.1}. Oferece as
 * {@linkplain FerramentasMcp ferramentas} de ler projetos e sugerir Tasks e subtarefas.
 * <p>
 * Toda chamada precisa de {@code Authorization: Bearer <token>}. Há dois tipos de token:
 * <ul>
 *     <li>o <b>externo</b>, fixo, guardado em {@code servidor-mcp.json} na pasta de dados com a porta, para você
 *     registrar o servidor num agente de fora da app ({@link #comandoClaudeCode()});</li>
 *     <li>um por <b>execução</b> do agendador ({@link #abrir}), que vale só enquanto ela roda e já diz o projeto.</li>
 * </ul>
 */
public class ServidorMcp implements AutoCloseable {

    /** Nome do servidor no {@code --mcp-config}; as ferramentas ficam {@code mcp__overtimer__<nome>}. */
    public static final String NOME = "overtimer";
    public static final int PORTA_PADRAO = 47821;
    static final String CAMINHO = "/mcp";
    private static final String ARQUIVO = "servidor-mcp.json";
    private static final List<String> VERSOES = List.of("2025-06-18", "2025-03-26", "2024-11-05");
    private static final int TAMANHO_MAXIMO = 1 << 20;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    /** Acesso de uma execução: o arquivo do {@code --mcp-config}, apagado com o token ao fechar. */
    public final class Sessao implements AutoCloseable {
        private final String token;
        private final Path config;

        private Sessao(String token, Path config) {
            this.token = token;
            this.config = config;
        }

        public Path config() {
            return config;
        }

        @Override
        public void close() {
            acessos.remove(token);
            try {
                Files.deleteIfExists(config);
            } catch (IOException e) {
                System.err.println("Servidor MCP: não foi possível apagar " + config + ": " + e.getMessage());
            }
        }
    }

    private final ObjectMapper json = new ObjectMapper();
    private final FerramentasMcp ferramentas;
    private final Path pastaDados;
    /** Token de cada execução em andamento e o projeto dela. */
    private final Map<String, String> acessos = new ConcurrentHashMap<>();
    private volatile HttpServer servidor;
    private ExecutorService threads;
    private volatile int porta = PORTA_PADRAO;
    private volatile String tokenExterno;
    private volatile String erro;

    public ServidorMcp(ProjetoService projetos, Path pastaDados) {
        this.ferramentas = new FerramentasMcp(projetos, json);
        this.pastaDados = pastaDados;
    }

    /**
     * Lê (ou cria) a porta e o token externo e começa a atender. Se não conseguir (ex.: porta em uso), guarda o
     * motivo em {@link #erro()} e a app segue sem o servidor.
     */
    public synchronized void iniciar() {
        if (servidor != null) {
            return;
        }
        try {
            carregarOuCriar();
            HttpServer novo = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), porta), 0);
            threads = Executors.newVirtualThreadPerTaskExecutor();
            novo.setExecutor(threads);
            novo.createContext(CAMINHO, this::tratar);
            novo.start();
            servidor = novo;
            erro = null;
        } catch (IOException | RuntimeException e) {
            erro = "Servidor MCP desligado: " + (e.getMessage() == null ? e.toString() : e.getMessage());
            System.err.println(erro);
        }
    }

    public boolean ativo() {
        return servidor != null;
    }

    /** Por que o servidor não está atendendo, ou vazio se está. */
    public Optional<String> erro() {
        return Optional.ofNullable(erro);
    }

    public int porta() {
        return porta;
    }

    public String endereco() {
        return "http://127.0.0.1:" + porta + CAMINHO;
    }

    /** Comando que registra o servidor no Claude Code de fora da app, com o token externo. */
    public String comandoClaudeCode() {
        return "claude mcp add --transport http " + NOME + " " + endereco()
                + " --header \"Authorization: Bearer " + tokenExterno + "\"";
    }

    /**
     * Abre o acesso de uma execução ao servidor: um token só dela, ligado ao projeto, e o arquivo do
     * {@code --mcp-config} com ele. Vazio se o servidor não está atendendo.
     */
    public Optional<Sessao> abrir(String projetoId) throws IOException {
        if (servidor == null) {
            return Optional.empty();
        }
        String token = token();
        ObjectNode config = json.createObjectNode();
        ObjectNode este = config.putObject("mcpServers").putObject(NOME);
        este.put("type", "http").put("url", endereco());
        este.putObject("headers").put("Authorization", "Bearer " + token);
        Path arquivo = Files.createTempFile("overtimer-mcp-", ".json");
        Files.writeString(arquivo, json.writeValueAsString(config));
        acessos.put(token, projetoId);
        return Optional.of(new Sessao(token, arquivo));
    }

    @Override
    public synchronized void close() {
        if (servidor != null) {
            servidor.stop(0);
            servidor = null;
            threads.shutdownNow();
        }
    }

    private void carregarOuCriar() throws IOException {
        Path arquivo = pastaDados.resolve(ARQUIVO);
        if (Files.isRegularFile(arquivo)) {
            JsonNode salvo = json.readTree(arquivo.toFile());
            porta = salvo.path("porta").asInt(PORTA_PADRAO);
            tokenExterno = salvo.path("token").asText("");
        }
        if (tokenExterno == null || tokenExterno.isBlank()) {
            tokenExterno = token();
            Files.createDirectories(pastaDados);
            ObjectNode novo = json.createObjectNode().put("porta", porta).put("token", tokenExterno);
            Files.writeString(arquivo, json.copy().enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsString(novo));
        }
    }

    private static String token() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ---------- HTTP ----------

    private void tratar(HttpExchange troca) throws IOException {
        try (troca) {
            if (!origemLocal(troca.getRequestHeaders().getFirst("Origin"))) {
                enviar(troca, 403, null);
                return;
            }
            if (!"POST".equals(troca.getRequestMethod())) {
                // Sem fluxo de eventos do servidor para o cliente: só respostas às chamadas.
                troca.getResponseHeaders().add("Allow", "POST");
                enviar(troca, 405, null);
                return;
            }
            Optional<String> projeto = autorizado(troca.getRequestHeaders().getFirst("Authorization"));
            if (projeto.isEmpty()) {
                enviar(troca, 401, null);
                return;
            }
            byte[] corpo;
            try (InputStream entrada = troca.getRequestBody()) {
                corpo = entrada.readNBytes(TAMANHO_MAXIMO + 1);
            }
            if (corpo.length > TAMANHO_MAXIMO) {
                enviar(troca, 413, null);
                return;
            }
            JsonNode resposta = processar(corpo, projeto.get().isEmpty() ? null : projeto.get());
            if (resposta == null) {
                enviar(troca, 202, null);
            } else {
                enviar(troca, 200, json.writeValueAsBytes(resposta));
            }
        }
    }

    /**
     * O projeto do token: vazio (string vazia) para o token externo, que não tem projeto, ou o da execução; ou
     * {@code Optional.empty()} se o token não vale.
     */
    private Optional<String> autorizado(String cabecalho) {
        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = cabecalho.substring("Bearer ".length()).strip();
        if (tokenExterno != null && java.security.MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                tokenExterno.getBytes(StandardCharsets.UTF_8))) {
            return Optional.of("");
        }
        return Optional.ofNullable(acessos.get(token));
    }

    /** Sem {@code Origin} (clientes que não são navegador) ou de uma página local; outra origem pode ser um site. */
    static boolean origemLocal(String origem) {
        if (origem == null) {
            return true;
        }
        try {
            String host = URI.create(origem).getHost();
            return "localhost".equals(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void enviar(HttpExchange troca, int codigo, byte[] corpo) throws IOException {
        if (corpo == null) {
            troca.sendResponseHeaders(codigo, -1);
            return;
        }
        troca.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        troca.sendResponseHeaders(codigo, corpo.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(corpo);
        }
    }

    // ---------- JSON-RPC ----------

    /**
     * Responde ao corpo de um POST: uma mensagem JSON-RPC ou uma lista delas. Null quando só vieram notificações e
     * respostas, que não têm resposta.
     *
     * @param projeto projeto da execução que chamou, ou null para o token externo
     */
    JsonNode processar(byte[] corpo, String projeto) {
        JsonNode pedido;
        try {
            pedido = json.readTree(corpo);
        } catch (IOException e) {
            return erro(null, -32700, "JSON inválido.");
        }
        if (pedido == null || pedido.isMissingNode()) {
            return erro(null, -32600, "Pedido vazio.");
        }
        if (!pedido.isArray()) {
            return responder(pedido, projeto);
        }
        ArrayNode respostas = json.createArrayNode();
        for (JsonNode um : pedido) {
            JsonNode resposta = responder(um, projeto);
            if (resposta != null) {
                respostas.add(resposta);
            }
        }
        return respostas.isEmpty() ? null : respostas;
    }

    private JsonNode responder(JsonNode pedido, String projeto) {
        JsonNode id = pedido.get("id");
        String metodo = pedido.path("method").asText(null);
        if (metodo == null) {
            // Uma resposta do cliente (não pedimos nada a ele) ou lixo sem id: nada a responder.
            return id == null ? null : erro(id, -32600, "Pedido sem método.");
        }
        if (id == null) {
            // Notificação (ex.: notifications/initialized): não tem resposta.
            return null;
        }
        JsonNode params = pedido.path("params");
        return switch (metodo) {
            case "initialize" -> resultado(id, inicializar(params));
            case "ping" -> resultado(id, json.createObjectNode());
            case "tools/list" -> {
                ObjectNode lista = json.createObjectNode();
                lista.set("tools", ferramentas.definicoes());
                yield resultado(id, lista);
            }
            case "tools/call" -> chamar(id, params, projeto);
            default -> erro(id, -32601, "Método desconhecido: " + metodo);
        };
    }

    private ObjectNode inicializar(JsonNode params) {
        String pedida = params.path("protocolVersion").asText("");
        ObjectNode resposta = json.createObjectNode()
                .put("protocolVersion", VERSOES.contains(pedida) ? pedida : VERSOES.getFirst());
        resposta.putObject("capabilities").putObject("tools").put("listChanged", false);
        resposta.putObject("serverInfo").put("name", NOME).put("version", "0.1.0");
        resposta.put("instructions", "Ferramentas do Overtimer, que organiza Tasks e subtarefas executadas por "
                + "agentes. Para planejar trabalho num projeto: leia o projeto, use listar_tasks para ver o que já "
                + "existe e sugira Tasks (sugerir_task) ou subtarefas em Tasks existentes (sugerir_subtarefas). "
                + "Tudo o que você sugere espera a pessoa aceitar antes de ser executado.");
        return resposta;
    }

    private JsonNode chamar(JsonNode id, JsonNode params, String projeto) {
        String nome = params.path("name").asText("");
        if (!ferramentas.existe(nome)) {
            return erro(id, -32602, "Ferramenta desconhecida: " + nome);
        }
        ObjectNode resultado = json.createObjectNode();
        String texto;
        boolean falhou;
        try {
            texto = ferramentas.chamar(nome, params.get("arguments"), projeto);
            falhou = false;
        } catch (FerramentasMcp.ErroDaFerramenta e) {
            texto = e.getMessage();
            falhou = true;
        } catch (RuntimeException e) {
            texto = "Erro inesperado: " + (e.getMessage() == null ? e.toString() : e.getMessage());
            falhou = true;
        }
        resultado.putArray("content").addObject().put("type", "text").put("text", texto);
        resultado.put("isError", falhou);
        return resultado(id, resultado);
    }

    private ObjectNode resultado(JsonNode id, JsonNode resultado) {
        ObjectNode resposta = json.createObjectNode().put("jsonrpc", "2.0");
        resposta.set("id", id);
        resposta.set("result", resultado);
        return resposta;
    }

    private ObjectNode erro(JsonNode id, int codigo, String mensagem) {
        ObjectNode resposta = json.createObjectNode().put("jsonrpc", "2.0");
        resposta.set("id", id == null ? json.nullNode() : id);
        resposta.putObject("error").put("code", codigo).put("message", mensagem);
        return resposta;
    }
}
