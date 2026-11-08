package io.github.luanfrombrazil.overtimer.service.consumo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luanfrombrazil.overtimer.model.Consumo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Uso da assinatura do Claude, o mesmo que o {@code /usage} do Claude Code mostra.
 * <p>
 * Usa o token OAuth que o Claude Code guarda em {@code ~/.claude/.credentials.json} (ou em
 * {@code $CLAUDE_CONFIG_DIR}) e consulta o endpoint de uso da Anthropic. A sessão é a janela de 5 horas
 * e o semanal é a janela de 7 dias.
 */
public class MedidorClaude implements Medidor {

    static final URI ENDERECO = URI.create("https://api.anthropic.com/api/oauth/usage");
    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(15);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Path credenciais;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TEMPO_LIMITE).build();
    /** Última medição com porcentagem, devolvida quando o provedor recusa por excesso de consultas. */
    private volatile Consumo ultimo;
    private volatile Instant medidoEm;

    public MedidorClaude() {
        this(pastaPadrao().resolve(".credentials.json"));
    }

    MedidorClaude(Path credenciais) {
        this.credenciais = credenciais;
    }

    @Override
    public String provedor() {
        return "Claude";
    }

    @Override
    public Consumo medir() {
        String token;
        try {
            token = token();
        } catch (IOException | RuntimeException e) {
            return Consumo.desconhecido(provedor(), "Não foi possível ler as credenciais do Claude Code.");
        }
        if (token == null) {
            return Consumo.desconhecido(provedor(), "Faça login no Claude Code para ver o uso.");
        }
        HttpRequest pedido = HttpRequest.newBuilder(ENDERECO)
                .timeout(TEMPO_LIMITE)
                .header("Authorization", "Bearer " + token)
                .header("anthropic-beta", "oauth-2025-04-20")
                .header("Accept", "application/json")
                .header("User-Agent", "overtimer")
                .GET()
                .build();
        try {
            HttpResponse<String> resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() == 401) {
                return Consumo.desconhecido(provedor(), "Login do Claude Code expirado: abra o Claude Code para renovar.");
            }
            if (resposta.statusCode() == 429) {
                return limiteAtingido();
            }
            if (resposta.statusCode() != 200) {
                return Consumo.desconhecido(provedor(), "Uso indisponível (HTTP " + resposta.statusCode() + ").");
            }
            Consumo medido = interpretar(provedor(), JSON.readTree(resposta.body()));
            if (medido.conhecido()) {
                lembrar(medido, Instant.now());
            }
            return medido;
        } catch (IOException | RuntimeException e) {
            return Consumo.desconhecido(provedor(), "Sem conexão com a Anthropic.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Consumo.desconhecido(provedor(), "Leitura interrompida.");
        }
    }

    void lembrar(Consumo medido, Instant quando) {
        ultimo = medido;
        medidoEm = quando;
    }

    /**
     * Resposta ao HTTP 429 (consultas demais): mantém a última medição boa, avisando na dica de quando ela é. Sem
     * medição anterior, fica sem porcentagem.
     */
    Consumo limiteAtingido() {
        Consumo anterior = ultimo;
        if (anterior == null) {
            return Consumo.desconhecido(provedor(), "Limite de consultas do Claude atingido; o uso aparece na próxima medição.");
        }
        String quando = LocalTime.ofInstant(medidoEm, ZoneId.systemDefault()).format(HORA);
        String aviso = "medido às " + quando + " (limite de consultas atingido)";
        String detalhe = anterior.detalhe() == null ? aviso : anterior.detalhe() + " · " + aviso;
        return new Consumo(anterior.provedor(), anterior.sessao(), anterior.semanal(), detalhe);
    }

    /** Transforma a resposta do endpoint ({@code five_hour}/{@code seven_day} com {@code utilization} de 0 a 100). */
    static Consumo interpretar(String provedor, JsonNode uso) {
        JsonNode sessao = uso.path("five_hour");
        JsonNode semana = uso.path("seven_day");
        if (!sessao.hasNonNull("utilization") && !semana.hasNonNull("utilization")) {
            return Consumo.desconhecido(provedor, "O Claude não informou o uso.");
        }
        List<String> detalhe = new ArrayList<>();
        int usoSessao = Consumo.DESCONHECIDO;
        int usoSemana = Consumo.DESCONHECIDO;
        if (sessao.hasNonNull("utilization")) {
            usoSessao = (int) Math.round(sessao.get("utilization").asDouble());
            detalhe.add("Janela de 5 h: " + usoSessao + "%" + renova(sessao));
        }
        if (semana.hasNonNull("utilization")) {
            usoSemana = (int) Math.round(semana.get("utilization").asDouble());
            detalhe.add("Semana: " + usoSemana + "%" + renova(semana));
        }
        return new Consumo(provedor, usoSessao, usoSemana, String.join(" · ", detalhe));
    }

    private static String renova(JsonNode janela) {
        if (!janela.hasNonNull("resets_at")) {
            return "";
        }
        try {
            return Renovacao.texto(OffsetDateTime.parse(janela.get("resets_at").asText()).toInstant());
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    private String token() throws IOException {
        if (!Files.isRegularFile(credenciais)) {
            return null;
        }
        JsonNode oauth = JSON.readTree(credenciais.toFile()).path("claudeAiOauth");
        if (!oauth.hasNonNull("accessToken")) {
            return null;
        }
        return oauth.get("accessToken").asText();
    }

    static Path pastaPadrao() {
        String configurada = System.getenv("CLAUDE_CONFIG_DIR");
        if (configurada != null && !configurada.isBlank()) {
            return Path.of(configurada);
        }
        return Path.of(System.getProperty("user.home"), ".claude");
    }
}
