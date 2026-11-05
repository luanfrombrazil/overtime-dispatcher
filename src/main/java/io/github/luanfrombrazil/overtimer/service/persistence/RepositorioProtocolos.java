package io.github.luanfrombrazil.overtimer.service.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.luanfrombrazil.overtimer.model.ExecucaoProtocolo;
import io.github.luanfrombrazil.overtimer.model.Protocolo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lê e grava os protocolos em JSON:
 * <pre>
 * protocolos-rotina/&lt;id do protocolo&gt;/protocolo.json  o protocolo com a agenda, os acessos e as filas
 * protocolos-rotina/&lt;id do protocolo&gt;/historico.json  as últimas execuções
 * </pre>
 */
public class RepositorioProtocolos {

    private static final String ARQUIVO_PROTOCOLO = "protocolo.json";
    private static final String ARQUIVO_HISTORICO = "historico.json";

    /** Conteúdo de historico.json. */
    private record ArquivoHistorico(List<ExecucaoProtocolo> execucoes) {
    }

    private final Path raiz;
    private final ObjectMapper json = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** @param pastaDados pasta central da aplicação; os protocolos ficam em {@code protocolos-rotina/} dentro dela */
    public RepositorioProtocolos(Path pastaDados) {
        this.raiz = pastaDados.resolve("protocolos-rotina");
    }

    public List<Protocolo> carregar() {
        if (!Files.isDirectory(raiz)) {
            return List.of();
        }
        List<Protocolo> protocolos = new ArrayList<>();
        try (Stream<Path> pastas = Files.list(raiz)) {
            for (Path pasta : pastas.filter(Files::isDirectory).toList()) {
                Path arquivo = pasta.resolve(ARQUIVO_PROTOCOLO);
                if (Files.isRegularFile(arquivo)) {
                    protocolos.add(json.readValue(arquivo.toFile(), Protocolo.class));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler os protocolos em " + raiz, e);
        }
        return protocolos;
    }

    public void salvar(Protocolo protocolo) {
        Path pasta = pastaDo(protocolo.id());
        try {
            Files.createDirectories(pasta);
            gravar(pasta.resolve(ARQUIVO_PROTOCOLO), protocolo);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar o protocolo " + protocolo.nome(), e);
        }
    }

    /** Execuções guardadas do protocolo, da mais recente para a mais antiga; vazia se não há histórico. */
    public List<ExecucaoProtocolo> carregarHistorico(String id) {
        Path arquivo = pastaDo(id).resolve(ARQUIVO_HISTORICO);
        if (!Files.isRegularFile(arquivo)) {
            return List.of();
        }
        try {
            ArquivoHistorico lido = json.readValue(arquivo.toFile(), ArquivoHistorico.class);
            return lido.execucoes() == null ? List.of() : lido.execucoes();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o histórico do protocolo " + id, e);
        }
    }

    /** Grava o histórico ao lado do protocolo; não faz nada se a pasta dele não existe mais. */
    public void salvarHistorico(String id, List<ExecucaoProtocolo> execucoes) {
        Path pasta = pastaDo(id);
        if (!Files.isDirectory(pasta)) {
            return;
        }
        try {
            gravar(pasta.resolve(ARQUIVO_HISTORICO), new ArquivoHistorico(execucoes));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar o histórico do protocolo " + id, e);
        }
    }

    public void excluir(String id) {
        Path pasta = pastaDo(id);
        if (!Files.exists(pasta)) {
            return;
        }
        try (Stream<Path> arvore = Files.walk(pasta)) {
            for (Path p : arvore.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível excluir o protocolo " + id, e);
        }
    }

    private Path pastaDo(String id) {
        Path pasta = raiz.resolve(id).normalize();
        if (!raiz.normalize().equals(pasta.getParent())) {
            throw new IllegalArgumentException("Id de protocolo inválido: " + id);
        }
        return pasta;
    }

    /** Escreve num arquivo temporário e troca, para não deixar um JSON pela metade. */
    private void gravar(Path destino, Object valor) throws IOException {
        Path temporario = destino.resolveSibling(destino.getFileName() + ".tmp");
        json.writeValue(temporario.toFile(), valor);
        Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
