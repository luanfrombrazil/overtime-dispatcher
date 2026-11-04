package io.github.luanfrombrazil.overtimedispatcher.service.consumo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luanfrombrazil.overtimedispatcher.model.Consumo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Uso da assinatura do Codex, lido dos registros de sessão que o próprio CLI grava em
 * {@code ~/.codex/sessions/AAAA/MM/DD/rollout-*.jsonl} (ou em {@code $CODEX_HOME}).
 * <p>
 * Cada evento {@code token_count} traz {@code rate_limits.primary} (janela curta) e {@code secondary} (semana).
 * Vale o último evento gravado; se a janela já renovou desde então, o uso volta a 0%.
 */
public class MedidorCodex implements Medidor {

    /** Quantos arquivos de sessão, dos mais novos para os mais antigos, procurar até achar um com limites. */
    private static final int ARQUIVOS_PROCURADOS = 20;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Path sessoes;

    public MedidorCodex() {
        this(pastaPadrao().resolve("sessions"));
    }

    MedidorCodex(Path sessoes) {
        this.sessoes = sessoes;
    }

    @Override
    public String provedor() {
        return "Codex";
    }

    @Override
    public Consumo medir() {
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
        long minutos = janela.get("window_minutes").asLong();
        if (minutos % (24 * 60) == 0) {
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
