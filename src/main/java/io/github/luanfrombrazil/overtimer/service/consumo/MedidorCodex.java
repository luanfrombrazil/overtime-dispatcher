package io.github.luanfrombrazil.overtimer.service.consumo;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Uso da assinatura do Codex, o mesmo que o {@code /status} do Codex mostra.
 * <p>
 * Com o login do ChatGPT em {@code ~/.codex/auth.json} (ou em {@code $CODEX_HOME}), pergunta ao endpoint de uso
 * ({@code primary_window} é a janela curta e {@code secondary_window} a semana). Sem login, ou se a consulta
 * falhar, lê os registros de sessão que o próprio CLI grava em {@code sessions/AAAA/MM/DD/rollout-*.jsonl}: cada
 * evento {@code token_count} traz {@code rate_limits.primary} e {@code secondary}. Vale o último evento gravado; se
 * a janela já renovou desde então, o uso volta a 0%.
 */
public class MedidorCodex implements Medidor {

    static final URI ENDERECO = URI.create("https://chatgpt.com/backend-api/wham/usage");
    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(15);
    /** Quantos arquivos de sessão, dos mais novos para os mais antigos, procurar até achar um com limites. */
    private static final int ARQUIVOS_PROCURADOS = 20;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Path sessoes;
    private final Path credenciais;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TEMPO_LIMITE).build();

    public MedidorCodex() {
        this(pastaPadrao().resolve("sessions"), pastaPadrao().resolve("auth.json"));
    }

    MedidorCodex(Path sessoes, Path credenciais) {
        this.sessoes = sessoes;
        this.credenciais = credenciais;
    }

    @Override
    public String provedor() {
        return "Codex";
    }

    @Override
    public Consumo medir() {
        Consumo aoVivo = consultar();
        return aoVivo != null && aoVivo.conhecido() ? aoVivo : dasSessoes();
    }

    /** Pergunta ao endpoint de uso; null quando não há login do ChatGPT ou a consulta falha. */
    private Consumo consultar() {
        JsonNode tokens;
        try {
            if (!Files.isRegularFile(credenciais)) {
                return null;
            }
            tokens = JSON.readTree(credenciais.toFile()).path("tokens");
        } catch (IOException | RuntimeException e) {
            return null;
        }
        if (!tokens.hasNonNull("access_token")) {
            return null;
        }
        HttpRequest.Builder pedido = HttpRequest.newBuilder(ENDERECO)
                .timeout(TEMPO_LIMITE)
                .header("Authorization", "Bearer " + tokens.get("access_token").asText())
                .header("Accept", "application/json")
                .header("User-Agent", "overtimer")
                .GET();
        if (tokens.hasNonNull("account_id")) {
            pedido.header("ChatGPT-Account-Id", tokens.get("account_id").asText());
        }
        try {
            HttpResponse<String> resposta = http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) {
                return null;
            }
            return interpretarApi(provedor(), JSON.readTree(resposta.body()), Instant.now());
        } catch (IOException | RuntimeException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Resposta do endpoint: {@code rate_limit.primary_window}/{@code secondary_window} com {@code used_percent},
     * {@code limit_window_seconds} e {@code reset_at} (segundos desde 1970) ou {@code reset_after_seconds}.
     */
    static Consumo interpretarApi(String provedor, JsonNode resposta, Instant agora) {
        JsonNode limites = resposta.path("rate_limit");
        List<String> detalhe = new ArrayList<>();
        int usoSessao = Consumo.DESCONHECIDO;
        int usoSemana = Consumo.DESCONHECIDO;
        JsonNode primaria = limites.path("primary_window");
        if (primaria.hasNonNull("used_percent")) {
            usoSessao = (int) Math.round(primaria.get("used_percent").asDouble());
            detalhe.add("Janela de " + janelaApi(primaria, "5 h") + ": " + usoSessao + "%" + renovaApi(primaria, agora));
        }
        JsonNode secundaria = limites.path("secondary_window");
        if (secundaria.hasNonNull("used_percent")) {
            usoSemana = (int) Math.round(secundaria.get("used_percent").asDouble());
            detalhe.add("Janela de " + janelaApi(secundaria, "7 dias") + ": " + usoSemana + "%"
                    + renovaApi(secundaria, agora));
        }
        if (detalhe.isEmpty()) {
            return Consumo.desconhecido(provedor, "O Codex ainda não informou o uso.");
        }
        return new Consumo(provedor, usoSessao, usoSemana, String.join(" · ", detalhe));
    }

    private static String janelaApi(JsonNode janela, String padrao) {
        return janela.hasNonNull("limit_window_seconds")
                ? duracao(janela.get("limit_window_seconds").asLong() / 60)
                : padrao;
    }

    private static String renovaApi(JsonNode janela, Instant agora) {
        Instant renovacao = null;
        if (janela.hasNonNull("reset_at") && janela.get("reset_at").isNumber()) {
            renovacao = Instant.ofEpochSecond(janela.get("reset_at").asLong());
        } else if (janela.hasNonNull("reset_after_seconds")) {
            renovacao = agora.plusSeconds(janela.get("reset_after_seconds").asLong());
        }
        return renovacao == null || !renovacao.isAfter(agora) ? "" : Renovacao.texto(renovacao);
    }

    private Consumo dasSessoes() {
        if (!Files.isDirectory(sessoes)) {
            return Consumo.desconhecido(provedor(), "Nenhuma sessão do Codex encontrada.");
        }
        try {
            for (Path arquivo : maisRecentes()) {
                JsonNode evento = ultimoComLimites(arquivo);
                if (evento != null) {
                    return interpretar(provedor(), evento, Instant.now());
                }
            }
        } catch (IOException | RuntimeException e) {
            return Consumo.desconhecido(provedor(), "Não foi possível ler as sessões do Codex.");
        }
        return Consumo.desconhecido(provedor(), "O Codex ainda não informou o uso.");
    }

    /** Evento com {@code payload.rate_limits} (ou {@code rate_limits} na raiz, nas versões antigas). */
    static Consumo interpretar(String provedor, JsonNode evento, Instant agora) {
        JsonNode limites = limites(evento);
        Instant quando = instante(evento);
        List<String> detalhe = new ArrayList<>();
        int usoSessao = Consumo.DESCONHECIDO;
        int usoSemana = Consumo.DESCONHECIDO;
        JsonNode primaria = limites.path("primary");
        if (primaria.hasNonNull("used_percent")) {
            usoSessao = usado(primaria, quando, agora);
            detalhe.add("Janela de " + janela(primaria, "5 h") + ": " + usoSessao + "%" + renova(primaria, quando, agora));
        }
        JsonNode secundaria = limites.path("secondary");
        if (secundaria.hasNonNull("used_percent")) {
            usoSemana = usado(secundaria, quando, agora);
            detalhe.add("Janela de " + janela(secundaria, "7 dias") + ": " + usoSemana + "%"
                    + renova(secundaria, quando, agora));
        }
        if (detalhe.isEmpty()) {
            return Consumo.desconhecido(provedor, "O Codex ainda não informou o uso.");
        }
        return new Consumo(provedor, usoSessao, usoSemana, String.join(" · ", detalhe));
    }

    private static JsonNode limites(JsonNode evento) {
        JsonNode payload = evento.path("payload");
        return payload.has("rate_limits") ? payload.path("rate_limits") : evento.path("rate_limits");
    }

    private static int usado(JsonNode janela, Instant quando, Instant agora) {
        Instant renovacao = renovacao(janela, quando);
        if (renovacao != null && !renovacao.isAfter(agora)) {
            return 0;
        }
        return (int) Math.round(janela.get("used_percent").asDouble());
    }

    private static String renova(JsonNode janela, Instant quando, Instant agora) {
        Instant renovacao = renovacao(janela, quando);
        return renovacao == null || !renovacao.isAfter(agora) ? "" : Renovacao.texto(renovacao);
    }

    /** {@code resets_at} em segundos desde 1970, ou {@code resets_in_seconds} contado a partir do evento. */
    private static Instant renovacao(JsonNode janela, Instant quando) {
        if (janela.hasNonNull("resets_at") && janela.get("resets_at").isNumber()) {
            return Instant.ofEpochSecond(janela.get("resets_at").asLong());
        }
        if (janela.hasNonNull("resets_in_seconds") && quando != null) {
            return quando.plusSeconds(janela.get("resets_in_seconds").asLong());
        }
        return null;
    }

    private static String janela(JsonNode janela, String padrao) {
        if (!janela.hasNonNull("window_minutes")) {
            return padrao;
        }
        return duracao(janela.get("window_minutes").asLong());
    }

    private static String duracao(long minutos) {
        if (minutos > 0 && minutos % (24 * 60) == 0) {
            return minutos / (24 * 60) + " dias";
        }
        return minutos % 60 == 0 ? minutos / 60 + " h" : minutos + " min";
    }

    private static Instant instante(JsonNode evento) {
        if (!evento.hasNonNull("timestamp")) {
            return null;
        }
        try {
            return Instant.parse(evento.get("timestamp").asText());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Arquivos de sessão do mais novo para o mais antigo, descendo pelas pastas de ano, mês e dia. */
    private List<Path> maisRecentes() throws IOException {
        List<Path> arquivos = new ArrayList<>();
        for (Path ano : subpastas(sessoes)) {
            for (Path mes : subpastas(ano)) {
                for (Path dia : subpastas(mes)) {
                    try (Stream<Path> conteudo = Files.list(dia)) {
                        conteudo.filter(p -> p.getFileName().toString().endsWith(".jsonl"))
                                .sorted(Comparator.comparing(MedidorCodex::modificado).reversed())
                                .forEach(arquivos::add);
                    }
                    if (arquivos.size() >= ARQUIVOS_PROCURADOS) {
                        return arquivos.subList(0, ARQUIVOS_PROCURADOS);
                    }
                }
            }
        }
        return arquivos;
    }

    private static List<Path> subpastas(Path pasta) throws IOException {
        try (Stream<Path> conteudo = Files.list(pasta)) {
            return conteudo.filter(Files::isDirectory)
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .toList();
        }
    }

    private static long modificado(Path arquivo) {
        try {
            return Files.getLastModifiedTime(arquivo).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    private static JsonNode ultimoComLimites(Path arquivo) throws IOException {
        JsonNode ultimo = null;
        for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
            if (!linha.contains("rate_limits")) {
                continue;
            }
            try {
                JsonNode evento = JSON.readTree(linha);
                if (limites(evento).path("primary").hasNonNull("used_percent")
                        || limites(evento).path("secondary").hasNonNull("used_percent")) {
                    ultimo = evento;
                }
            } catch (JsonProcessingException e) {
                // Linha cortada (o Codex ainda está escrevendo): fica a anterior.
            }
        }
        return ultimo;
    }

    static Path pastaPadrao() {
        String configurada = System.getenv("CODEX_HOME");
        if (configurada != null && !configurada.isBlank()) {
            return Path.of(configurada);
        }
        return Path.of(System.getProperty("user.home"), ".codex");
    }
}
