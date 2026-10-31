package io.github.luanfrombrazil.overtimedispatcher.service.agente;

import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import io.github.luanfrombrazil.overtimedispatcher.service.Git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Decide em que pasta o agente trabalha.
 * <p>
 * Sem git, é a própria pasta do projeto. Com git, cada Task ganha um worktree em
 * {@code <pasta da app>/worktrees/<id da task>}, na branch {@code overtime/<título>-<id>} criada a partir da
 * branch padrão do projeto: o agente nunca mexe na pasta do projeto nem na branch principal, e o merge fica
 * para depois da sua verificação.
 */
public class AreaDeTrabalho {

    private final Path worktrees;

    public AreaDeTrabalho(Path pastaDados) {
        this.worktrees = pastaDados.resolve("worktrees");
    }

    /** Prepara a pasta da Task, criando o worktree e a branch na primeira vez. */
    public Path preparar(Projeto projeto, Task task) throws IOException, InterruptedException {
        Path pastaProjeto = Path.of(projeto.pasta());
        if (!Files.isDirectory(pastaProjeto)) {
            throw new IOException("A pasta do projeto não existe: " + pastaProjeto);
        }
        if (!projeto.usaGit()) {
            return pastaProjeto;
        }
        Path pasta = worktrees.resolve(task.id());
        if (Files.isDirectory(pasta.resolve(".git")) || Files.isRegularFile(pasta.resolve(".git"))) {
            return pasta;
        }
        Files.createDirectories(worktrees);
        Git.executar(pastaProjeto, "worktree", "prune");
        String branch = branch(task);
        if (Git.executar(pastaProjeto, "rev-parse", "--verify", "--quiet", "refs/heads/" + branch).ok()) {
            exigir(Git.executar(pastaProjeto, "worktree", "add", pasta.toString(), branch));
        } else {
            exigir(Git.executar(pastaProjeto, "worktree", "add", "-b", branch, pasta.toString(), projeto.branch()));
        }
        return pasta;
    }

    /** Ex.: "Corrigir login com e-mail" → {@code overtime/corrigir-login-com-e-mail-1a2b3c4d}. */
    public static String branch(Task task) {
        String semAcentos = Normalizer.normalize(task.titulo(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = semAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (slug.length() > 40) {
            slug = slug.substring(0, 40).replaceAll("-$", "");
        }
        String id = task.id().replace("-", "");
        return "overtime/" + (slug.isEmpty() ? "" : slug + "-") + id.substring(0, Math.min(8, id.length()));
    }

    private static void exigir(Git.Saida saida) throws IOException {
        if (!saida.ok()) {
            throw new IOException("Não foi possível criar o worktree: " + saida.texto());
        }
    }
}
