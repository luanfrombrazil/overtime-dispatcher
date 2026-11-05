package io.github.luanfrombrazil.overtimer.service.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Aviso;
import io.github.luanfrombrazil.overtimer.model.Notificacao;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê e grava as notificações em {@code notificacoes.json}, na pasta central da app:
 * <pre>
 * { "notificacoes": [ { "id": "...", "tipo": "FALHAS", "titulo": "...", "texto": "...", "projetoId": "...",
 *   "taskId": "...", "instante": "2026-11-05T14:30:00", "lida": false } ] }
 * </pre>
 * O formato está em docs/modulos/persistencia.md.
 */
public class NotificacoesRepositorio {

    private static final String ARQUIVO = "notificacoes.json";

    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path arquivo;

    public NotificacoesRepositorio() {
        this(PastaDados.padrao());
    }

    public NotificacoesRepositorio(Path pasta) {
        this.arquivo = pasta.resolve(ARQUIVO);
    }

    /** Notificações salvas, na ordem do arquivo; sem arquivo ou com ele inválido, nenhuma. */
    public List<Notificacao> carregar() {
        if (!Files.exists(arquivo)) {
            return List.of();
        }
        try {
            JsonNode raiz = json.readTree(arquivo.toFile());
            if (raiz == null || !raiz.isObject()) {
                throw new IllegalArgumentException("o arquivo não contém um objeto JSON");
            }
            List<Notificacao> notificacoes = new ArrayList<>();
            for (JsonNode no : raiz.path("notificacoes")) {
                Notificacao notificacao = ler(no);
                if (notificacao != null) {
                    notificacoes.add(notificacao);
                }
            }
            return List.copyOf(notificacoes);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Notificações inválidas em " + arquivo + "; começando sem nenhuma: " + e.getMessage());
            return List.of();
        }
    }

    public void salvar(List<Notificacao> notificacoes) {
        ObjectNode raiz = json.createObjectNode();
        ArrayNode lista = raiz.putArray("notificacoes");
        notificacoes.forEach(n -> lista.add(escrever(n)));
        try {
            Files.createDirectories(arquivo.getParent());
            Path temporario = arquivo.resolveSibling(ARQUIVO + ".tmp");
            json.writeValue(temporario.toFile(), raiz);
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar as notificações em " + arquivo, e);
        }
    }

    /** Uma notificação com tipo que não existe mais, ou sem id ou instante, é ignorada sem invalidar as outras. */
    private static Notificacao ler(JsonNode no) {
        String id = texto(no, "id");
        String instante = texto(no, "instante");
        Aviso tipo = tipo(texto(no, "tipo"));
        if (id == null || instante == null || tipo == null) {
            return null;
        }
        try {
            return new Notificacao(id, tipo, no.path("titulo").asText(""), no.path("texto").asText(""),
                    texto(no, "projetoId"), texto(no, "taskId"), LocalDateTime.parse(instante),
                    no.path("lida").asBoolean(false));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private ObjectNode escrever(Notificacao notificacao) {
        ObjectNode no = json.createObjectNode()
                .put("id", notificacao.id())
                .put("tipo", notificacao.tipo().name())
                .put("titulo", notificacao.titulo())
                .put("texto", notificacao.texto());
        // Ausentes quando o aviso não é de um projeto ou de uma Task.
        if (notificacao.projetoId() != null) {
            no.put("projetoId", notificacao.projetoId());
        }
        if (notificacao.taskId() != null) {
            no.put("taskId", notificacao.taskId());
        }
        return no.put("instante", notificacao.instante().toString())
                .put("lida", notificacao.lida());
    }

    private static String texto(JsonNode no, String campo) {
        JsonNode valor = no.path(campo);
        return valor.isTextual() && !valor.asText().isBlank() ? valor.asText() : null;
    }

    private static Aviso tipo(String nome) {
        for (Aviso aviso : Aviso.values()) {
            if (aviso.name().equals(nome)) {
                return aviso;
            }
        }
        return null;
    }
}
