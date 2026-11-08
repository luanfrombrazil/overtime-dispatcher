package io.github.luanfrombrazil.overtimer.service.agente;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Lê a saída {@code --output-format stream-json} do Claude Code, linha a linha: transforma o que o agente
 * faz em linhas curtas de log e guarda o evento final ({@code "type":"result"}).
 */
class LeitorClaudeCode {

    private static final int LIMITE_LINHA = 160;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Consumer<String> log;
    private boolean terminou;
    private boolean erro;
    private String resposta;
    private String sessao;

    LeitorClaudeCode(Consumer<String> log) {
        this.log = log;
    }

    void processar(String linha) {
        if (linha.isBlank()) {
            return;
        }
        JsonNode evento;
        try {
            evento = JSON.readTree(linha);
        } catch (JsonProcessingException e) {
            log.accept(curta(linha));
            return;
        }
        if (evento.hasNonNull("session_id")) {
            sessao = evento.get("session_id").asText();
        }
        switch (evento.path("type").asText()) {
            case "assistant" -> {
                for (JsonNode parte : evento.path("message").path("content")) {
                    switch (parte.path("type").asText()) {
                        case "text" -> log.accept(curta(parte.path("text").asText()));
                        case "tool_use" -> log.accept(ferramenta(parte.path("name").asText(), parte.path("input")));
                        default -> {
                        }
                    }
                }
            }
            case "result" -> {
                terminou = true;
                erro = evento.path("is_error").asBoolean(false) || !"success".equals(evento.path("subtype").asText());
                resposta = evento.hasNonNull("result") ? evento.get("result").asText().strip()
                        : evento.path("subtype").asText("sem resposta");
                for (JsonNode negada : evento.path("permission_denials")) {
                    log.accept("Negado: " + negada.path("tool_name").asText());
                }
            }
            default -> {
            }
        }
    }

    boolean terminou() {
        return terminou;
    }

    boolean erro() {
        return erro;
    }

    String resposta() {
        return resposta;
    }

    String sessao() {
        return sessao;
    }

    /** Ex.: "› Edit Login.java", "› Bash mvn test". */
    private static String ferramenta(String nome, JsonNode entrada) {
        String alvo = "";
        if (entrada.hasNonNull("file_path")) {
            alvo = nomeDoArquivo(entrada.get("file_path").asText());
        } else if (entrada.hasNonNull("command")) {
            alvo = entrada.get("command").asText();
        } else if (entrada.hasNonNull("pattern")) {
            alvo = entrada.get("pattern").asText();
        } else if (entrada.hasNonNull("url")) {
            alvo = entrada.get("url").asText();
        }
        return curta("› " + nome + (alvo.isBlank() ? "" : " " + alvo));
    }

    private static String nomeDoArquivo(String caminho) {
        try {
            Path nome = Path.of(caminho).getFileName();
            return nome == null ? caminho : nome.toString();
        } catch (RuntimeException e) {
            return caminho;
        }
    }

    /** Primeira linha com conteúdo, cortada. */
    static String curta(String texto) {
        String primeira = texto.strip().lines().findFirst().orElse("");
        return primeira.length() > LIMITE_LINHA ? primeira.substring(0, LIMITE_LINHA - 1) + "…" : primeira;
    }
}
