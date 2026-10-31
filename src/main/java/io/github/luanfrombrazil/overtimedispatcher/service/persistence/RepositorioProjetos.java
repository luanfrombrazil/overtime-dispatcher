package io.github.luanfrombrazil.overtimedispatcher.service.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.luanfrombrazil.overtimedispatcher.model.Esforco;
import io.github.luanfrombrazil.overtimedispatcher.model.Permissoes;
import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Provedor;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Lê e grava os projetos em JSON:
 * <pre>
 * projetos/&lt;id do projeto&gt;/projeto.json             dados do projeto e a ordem das Tasks
 * projetos/&lt;id do projeto&gt;/tasks/&lt;id da task&gt;/task.json  a Task com as suas subtarefas
 * projetos/&lt;id do projeto&gt;/tasks/&lt;id da task&gt;/log.txt    o que o agente fez em cada execução
 * </pre>
 */
public class RepositorioProjetos {

    private static final String ARQUIVO_PROJETO = "projeto.json";
    private static final String ARQUIVO_TASK = "task.json";
    private static final String ARQUIVO_LOG = "log.txt";

    /** Conteúdo de projeto.json: o projeto sem as Tasks, que ficam nas suas próprias pastas. */
    private record ArquivoProjeto(String id, String nome, String sigla, String descricao, String pasta, String branch,
                                  Provedor provedor, Esforco esforco, Permissoes permissoes, List<String> tasks) {
    }

    private final Path raiz;
    private final ObjectMapper json = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** @param pastaDados pasta central da aplicação; os projetos ficam em {@code projetos/} dentro dela */
    public RepositorioProjetos(Path pastaDados) {
        this.raiz = pastaDados.resolve("projetos");
    }

    public List<Projeto> carregar() {
        if (!Files.isDirectory(raiz)) {
            return List.of();
        }
        List<Projeto> projetos = new ArrayList<>();
        try (Stream<Path> pastas = Files.list(raiz)) {
            for (Path pasta : pastas.filter(Files::isDirectory).toList()) {
                Path arquivo = pasta.resolve(ARQUIVO_PROJETO);
                if (Files.isRegularFile(arquivo)) {
                    projetos.add(ler(pasta, json.readValue(arquivo.toFile(), ArquivoProjeto.class)));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler os projetos em " + raiz, e);
        }
        return projetos;
    }

    private Projeto ler(Path pasta, ArquivoProjeto arquivo) throws IOException {
        List<Task> tasks = new ArrayList<>();
        for (String id : arquivo.tasks() == null ? List.<String>of() : arquivo.tasks()) {
            Path arquivoTask = pasta.resolve("tasks").resolve(id).resolve(ARQUIVO_TASK);
            if (Files.isRegularFile(arquivoTask)) {
                tasks.add(json.readValue(arquivoTask.toFile(), Task.class));
            }
        }
        return new Projeto(arquivo.id(), arquivo.nome(), arquivo.sigla(), arquivo.descricao(), arquivo.pasta(),
                arquivo.branch(), arquivo.provedor(), arquivo.esforco(), arquivo.permissoes(), tasks);
    }

    /** Grava o projeto e as suas Tasks, e apaga as pastas das Tasks que não existem mais. */
    public void salvar(Projeto projeto) {
        Path pasta = pastaDo(projeto.id());
        Path pastaTasks = pasta.resolve("tasks");
        try {
            Files.createDirectories(pastaTasks);
            Set<String> ids = new HashSet<>();
            for (Task task : projeto.tasks()) {
                ids.add(task.id());
                Path pastaTask = pastaTasks.resolve(task.id());
                Files.createDirectories(pastaTask);
                gravar(pastaTask.resolve(ARQUIVO_TASK), task);
            }
            gravar(pasta.resolve(ARQUIVO_PROJETO), new ArquivoProjeto(projeto.id(), projeto.nome(), projeto.sigla(),
                    projeto.descricao(), projeto.pasta(), projeto.branch(), projeto.provedor(), projeto.esforco(),
                    projeto.permissoes(), projeto.tasks().stream().map(Task::id).toList()));
            try (Stream<Path> existentes = Files.list(pastaTasks)) {
                for (Path antiga : existentes.filter(p -> !ids.contains(p.getFileName().toString())).toList()) {
                    apagar(antiga);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar o projeto " + projeto.nome(), e);
        }
    }

    /** Acrescenta uma linha ao {@code log.txt} da Task, se a pasta dela existir. */
    public void registrarLog(String projetoId, String taskId, String linha) {
        Path pastaTask = pastaDo(projetoId).resolve("tasks").resolve(taskId);
        if (!Files.isDirectory(pastaTask)) {
            return;
        }
        try {
            Files.writeString(pastaTask.resolve(ARQUIVO_LOG), linha + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar o log da task " + taskId, e);
        }
    }

    /** Apaga os dados do projeto na pasta da aplicação. A pasta do projeto no sistema não é tocada. */
    public void excluir(String id) {
        try {
            apagar(pastaDo(id));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível excluir o projeto " + id, e);
        }
    }

    private Path pastaDo(String id) {
        Path pasta = raiz.resolve(id).normalize();
        if (!pasta.getParent().equals(raiz.normalize())) {
            throw new IllegalArgumentException("Id de projeto inválido: " + id);
        }
        return pasta;
    }

    /** Escreve num arquivo temporário e troca, para não deixar um JSON pela metade. */
    private void gravar(Path destino, Object valor) throws IOException {
        Path temporario = destino.resolveSibling(destino.getFileName() + ".tmp");
        json.writeValue(temporario.toFile(), valor);
        Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static void apagar(Path caminho) throws IOException {
        if (!Files.exists(caminho)) {
            return;
        }
        try (Stream<Path> arvore = Files.walk(caminho)) {
            for (Path p : arvore.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
