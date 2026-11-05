package io.github.luanfrombrazil.overtimer.service.agente;

import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.Git;

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
 * <p>
 * Um protocolo não tem pasta no sistema: o agente dele roda em {@code <pasta da app>/protocolos-rotina/<id>/trabalho},
 * ao lado do {@code protocolo.json}.
 */
public class AreaDeTrabalho {

    private final Path worktrees;
    private final Path protocolos;

    public AreaDeTrabalho(Path pastaDados) {
        this.worktrees = pastaDados.resolve("worktrees");
        this.protocolos = pastaDados.resolve("protocolos-rotina");
    }

    /** Prepara a pasta de trabalho do protocolo, criando-a na primeira execução. */
    public Path preparar(Protocolo protocolo) throws IOException {
        Path pastaProtocolo = protocolos.resolve(protocolo.id()).normalize();
        if (!protocolos.normalize().equals(pastaProtocolo.getParent())) {
            throw new IOException("Id de protocolo inválido: " + protocolo.id());
        }
        return Files.createDirectories(pastaProtocolo.resolve("trabalho"));
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
