package io.github.luanfrombrazil.overtimer.service.consumo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luanfrombrazil.overtimer.model.Consumo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Uso do Gemini pelo Antigravity CLI ({@code agy}), o mesmo que o {@code /usage} dele mostra.
 * <p>
 * O token do Google fica no cofre do sistema, então a cota é pedida ao servidor de linguagem que o próprio
 * {@code agy} sobe enquanto está aberto: a porta e o pid saem do log mais recente em
 * {@code ~/.gemini/antigravity-cli/log}, e o token CSRF da linha de comando desse processo. Cada modelo tem a sua
 * cota ({@code remainingFraction} de 0 a 1 e {@code resetTime}); a barra mostra a do modelo selecionado no
 * {@code agy} ou, sem ele, a do modelo mais gasto. O Antigravity não separa sessão de semana: só a sessão aparece.
 * <p>
 * Com o {@code agy} fechado, repete a última medição boa avisando de quando ela é.
 */
public class MedidorAntigravity implements Medidor {

    static final String CAMINHO = "/exa.language_server_pb.LanguageServerService/GetUserStatus";
    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(10);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern PID = Pattern.compile("Starting language server process with pid (\\d+)");
    private static final Pattern PORTA = Pattern.compile("listening on random port at (\\d+) for HTTP\\b(?!S)");
    private static final Pattern MODELO = Pattern.compile("selected model override to backend: label=\"([^\"]+)\"");
    private static final Pattern CSRF = Pattern.compile("--csrf_token[= ]\"?([\\w-]+)");

    private final Path logs;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TEMPO_LIMITE).build();
    private volatile Consumo ultimo;
    private volatile Instant medidoEm;

    public MedidorAntigravity() {
        this(pastaPadrao().resolve("log"));
    }

    MedidorAntigravity(Path logs) {
        this.logs = logs;
    }

    @Override
    public String provedor() {
        return "Gemini";
    }

    @Override
    public Consumo medir() {
        Servidor servidor;
        try {
            servidor = servidor();
        } catch (IOException | RuntimeException e) {
            return semServidor("Não foi possível ler os logs do Antigravity.");
        }
        if (servidor == null) {
            return semServidor("Abra o Antigravity (agy) para medir o uso do Gemini.");
        }
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + servidor.porta() + CAMINHO))
                .timeout(TEMPO_LIMITE)
                .header("Content-Type", "application/json")
                .header("Connect-Protocol-Version", "1")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"metadata\":{\"ideName\":\"antigravity\",\"extensionName\":\"antigravity\",\"locale\":\"pt\"}}"));
        String csrf = csrf(servidor.pid());
        if (csrf != null) {
            pedido.header("X-Codeium-Csrf-Token", csrf);
        }
        try {
            HttpResponse<String> resposta = http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) {
                return semServidor("Uso do Gemini indisponível (HTTP " + resposta.statusCode() + ").");
            }
            Consumo medido = interpretar(provedor(), JSON.readTree(resposta.body()), servidor.modelo());
            if (medido.conhecido()) {
                lembrar(medido, Instant.now());
            }
            return medido;
        } catch (IOException | RuntimeException e) {
            return semServidor("Sem conexão com o Antigravity (agy).");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Consumo.desconhecido(provedor(), "Leitura interrompida.");
        }
    }

    void lembrar(Consumo medido, Instant quando) {
        ultimo = medido;
        medidoEm = quando;
    }

    /** Sem resposta do {@code agy}: mantém a última medição boa ou, sem ela, fica sem porcentagem com o motivo. */
    Consumo semServidor(String motivo) {
        Consumo anterior = ultimo;
        if (anterior == null) {
            return Consumo.desconhecido(provedor(), motivo);
        }
        String aviso = "medido às " + LocalTime.ofInstant(medidoEm, ZoneId.systemDefault()).format(HORA)
                + " (Antigravity fechado)";
        String detalhe = anterior.detalhe() == null ? aviso : anterior.detalhe() + " · " + aviso;
        return new Consumo(anterior.provedor(), anterior.sessao(), anterior.semanal(), detalhe);
    }

    /**
     * Transforma a resposta do {@code GetUserStatus}
     * ({@code userStatus.cascadeModelConfigData.clientModelConfigs[].quotaInfo}).
     *
     * @param selecionado rótulo do modelo escolhido no {@code agy}, ou null para usar o mais gasto
     */
    static Consumo interpretar(String provedor, JsonNode resposta, String selecionado) {
        JsonNode modelos = resposta.path("userStatus").path("cascadeModelConfigData").path("clientModelConfigs");
        JsonNode escolhido = null;
        double maiorUso = -1;
        for (JsonNode modelo : modelos) {
            JsonNode cota = modelo.path("quotaInfo");
            if (cota.isMissingNode() || cota.isNull()) {
                continue;
            }
            if (selecionado != null && selecionado.equals(modelo.path("label").asText(null))) {
                escolhido = modelo;
                break;
            }
            double uso = 1 - cota.path("remainingFraction").asDouble(0);
            if (uso > maiorUso) {
                maiorUso = uso;
                escolhido = modelo;
            }
        }
        if (escolhido == null) {
            return Consumo.desconhecido(provedor, "O Antigravity não informou o uso.");
        }
        JsonNode cota = escolhido.path("quotaInfo");
        // Sem remainingFraction a cota acabou (o protobuf omite o zero).
        int uso = (int) Math.round((1 - cota.path("remainingFraction").asDouble(0)) * 100);
        String nome = escolhido.path("label").asText("Gemini");
        return new Consumo(provedor, uso, Consumo.DESCONHECIDO, nome + ": " + uso + "%" + renova(cota));
    }

    private static String renova(JsonNode cota) {
        if (!cota.hasNonNull("resetTime")) {
            return "";
        }
        try {
            Instant renovacao = Instant.parse(cota.get("resetTime").asText());
            return renovacao.isAfter(Instant.now()) ? Renovacao.texto(renovacao) : "";
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    /** Servidor do {@code agy} aberto agora, segundo o log mais recente; null se ele não estiver rodando. */
    private Servidor servidor() throws IOException {
        Path log = maisRecente();
        if (log == null) {
            return null;
        }
        Long pid = null;
        Integer porta = null;
        String modelo = null;
        for (String linha : Files.readAllLines(log, StandardCharsets.UTF_8)) {
            Matcher m = PID.matcher(linha);
            if (m.find()) {
                pid = Long.parseLong(m.group(1));
            }
            m = PORTA.matcher(linha);
            if (m.find()) {
                porta = Integer.parseInt(m.group(1));
            }
            m = MODELO.matcher(linha);
            if (m.find()) {
                modelo = m.group(1);
            }
        }
        if (pid == null || porta == null || ProcessHandle.of(pid).filter(ProcessHandle::isAlive).isEmpty()) {
            return null;
        }
        return new Servidor(pid, porta, modelo);
    }

    private Path maisRecente() throws IOException {
        if (!Files.isDirectory(logs)) {
            return null;
        }
        try (Stream<Path> conteudo = Files.list(logs)) {
            return conteudo.filter(p -> p.getFileName().toString().endsWith(".log"))
                    .max(Comparator.comparing(MedidorAntigravity::modificado))
                    .orElse(null);
        }
    }

    private static long modificado(Path arquivo) {
        try {
            return Files.getLastModifiedTime(arquivo).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    /** Token CSRF da linha de comando do servidor; null se não der para ler (o pedido vai sem ele). */
    private static String csrf(long pid) {
        String linha = ProcessHandle.of(pid).flatMap(p -> p.info().arguments())
                .map(argumentos -> String.join(" ", argumentos))
                .orElse(null);
        if (linha == null && System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            linha = linhaDeComandoWindows(pid);
        }
        if (linha == null) {
            return null;
        }
        Matcher m = CSRF.matcher(linha);
        return m.find() ? m.group(1) : null;
    }

    /** O Java não expõe os argumentos de outros processos no Windows; o PowerShell expõe. */
    private static String linhaDeComandoWindows(long pid) {
        try {
            Process processo = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "(Get-CimInstance Win32_Process -Filter 'ProcessId=" + pid + "').CommandLine")
                    .redirectErrorStream(true)
                    .start();
            processo.getOutputStream().close();
            String saida = new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!processo.waitFor(TEMPO_LIMITE.toSeconds(), TimeUnit.SECONDS)) {
                processo.destroyForcibly();
                return null;
            }
            return saida.isBlank() ? null : saida.trim();
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    static Path pastaPadrao() {
        return Path.of(System.getProperty("user.home"), ".gemini", "antigravity-cli");
    }

    private record Servidor(long pid, int porta, String modelo) {
    }
}
