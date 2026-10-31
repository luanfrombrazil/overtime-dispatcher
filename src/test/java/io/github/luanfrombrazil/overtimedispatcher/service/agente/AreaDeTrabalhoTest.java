package io.github.luanfrombrazil.overtimedispatcher.service.agente;

import io.github.luanfrombrazil.overtimedispatcher.model.Esforco;
import io.github.luanfrombrazil.overtimedispatcher.model.Permissoes;
import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Provedor;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaDeTrabalhoTest {

    @TempDir
    Path dados;

    @TempDir
    Path repositorio;

    private static String git(Path pasta, String... argumentos) throws IOException, InterruptedException {
        List<String> comando = new ArrayList<>(List.of("git", "-C", pasta.toString()));
        comando.addAll(List.of(argumentos));
        Process processo = new ProcessBuilder(comando).redirectErrorStream(true).start();
        String saida = new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        assertEquals(0, processo.waitFor(), saida);
        return saida;
    }

    @Test
    void projetoComGitGanhaWorktreeNaBranchDaTask() throws Exception {
        Assumptions.assumeTrue(Executavel.localizar("git").isPresent(), "git não instalado");
        git(repositorio, "init", "-b", "main");
        git(repositorio, "config", "user.email", "teste@example.com");
        git(repositorio, "config", "user.name", "Teste");
        Files.writeString(repositorio.resolve("README.md"), "oi");
        git(repositorio, "add", ".");
        git(repositorio, "commit", "-m", "inicial");

        Projeto projeto = Projeto.novo("P", "P", "", repositorio.toString(), "main",
                Provedor.AUTOMATICO, Esforco.MEDIUM, Permissoes.padrao());
        Task task = Task.nova("Minha task", "", Provedor.AUTOMATICO, Esforco.MEDIUM);
        AreaDeTrabalho area = new AreaDeTrabalho(dados);

        Path pasta = area.preparar(projeto, task);

        assertEquals(dados.resolve("worktrees").resolve(task.id()), pasta);
        assertTrue(Files.isRegularFile(pasta.resolve("README.md")));
        assertEquals(AreaDeTrabalho.branch(task), git(pasta, "branch", "--show-current"));
        assertEquals("main", git(repositorio, "branch", "--show-current"), "a pasta do projeto não muda de branch");
        assertEquals(pasta, area.preparar(projeto, task), "a segunda vez reaproveita o worktree");
    }

    @Test
    void projetoSemGitUsaAPropriaPasta() throws Exception {
        Projeto projeto = Projeto.novo("P", "P", "", repositorio.toString(), null,
                Provedor.AUTOMATICO, Esforco.MEDIUM, Permissoes.padrao());

        assertEquals(repositorio, new AreaDeTrabalho(dados).preparar(projeto, Task.nova("T", "", Provedor.AUTOMATICO, Esforco.MEDIUM)));
    }
}
