package io.github.luanfrombrazil.overtimer.service.agente;

import io.github.luanfrombrazil.overtimer.model.Acessos;
import io.github.luanfrombrazil.overtimer.model.Agenda;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProtocolos;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private static Protocolo protocolo() {
        return Protocolo.novo("Agenda", "AG", "Passos", Agenda.aCada(60), Provedor.AUTOMATICO, Esforco.MEDIUM,
                Acessos.nenhum(), List.of());
    }

    @Test
    void protocoloGanhaPastaDeTrabalhoNaPrimeiraExecucao() throws Exception {
        Protocolo protocolo = protocolo();
        Path esperada = dados.resolve("protocolos-rotina").resolve(protocolo.id()).resolve("trabalho");
        assertFalse(Files.exists(esperada));

        AreaDeTrabalho area = new AreaDeTrabalho(dados);
        assertEquals(esperada, area.preparar(protocolo));
        assertTrue(Files.isDirectory(esperada));

        Files.writeString(esperada.resolve("notas.md"), "da execução anterior");
        assertEquals(esperada, area.preparar(protocolo), "a segunda vez reaproveita a pasta");
        assertTrue(Files.isRegularFile(esperada.resolve("notas.md")));
    }

    @Test
    void pastaDeTrabalhoNaoAtrapalhaACargaDosProtocolos() throws Exception {
        Protocolo protocolo = protocolo();
        RepositorioProtocolos repositorio = new RepositorioProtocolos(dados);
        repositorio.salvar(protocolo);
        new AreaDeTrabalho(dados).preparar(protocolo);
        assertEquals(List.of(protocolo), repositorio.carregar());
    }

    @Test
    void protocoloComIdForaDaPastaEhRecusado() {
        Protocolo invalido = new Protocolo("../worktrees", "X", "X", "Passos", null, null, null, null, null, true,
                null, null);
        assertThrows(IOException.class, () -> new AreaDeTrabalho(dados).preparar(invalido));
    }
}
