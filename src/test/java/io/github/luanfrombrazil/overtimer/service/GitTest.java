package io.github.luanfrombrazil.overtimer.service;

import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProjetos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GitTest {

    @TempDir
    Path repositorio;

    @TempDir
    Path dados;

    private void git(String... argumentos) throws Exception {
        Git.Saida saida = Git.executar(repositorio, argumentos);
        assertTrue(saida.ok(), saida.texto());
    }

    @BeforeEach
    void criarRepositorio() throws Exception {
        try {
            assumeTrue(Git.executar(repositorio, "--version").ok(), "git não instalado");
        } catch (java.io.IOException e) {
            assumeTrue(false, "git não instalado");
        }
        git("init", "-b", "main");
        git("config", "user.email", "teste@example.com");
        git("config", "user.name", "Teste");
        Files.writeString(repositorio.resolve("README.md"), "oi");
        git("add", ".");
        git("commit", "-m", "inicial");
        git("branch", "develop");
        git("branch", "feature/login");
        git("branch", "overtime/task-da-app-1234abcd");
    }

    @Test
    void listaAsBranchesLocaisSemAsDaApp() {
        assertEquals(List.of("develop", "feature/login", "main"), Git.branches(repositorio));
    }

    @Test
    void pastaSemRepositorioNaoTemBranches(@TempDir Path vazia) {
        assertTrue(Git.branches(vazia).isEmpty());
    }

    @Test
    void projetoNaoAceitaBranchQueNaoExisteNaPasta() {
        ProjetoService servico = new ProjetoService(new RepositorioProjetos(dados));

        assertThrows(IllegalArgumentException.class, () -> servico.salvar(Projeto.novo("P", "P", "",
                repositorio.toString(), "nao-existe", Provedor.AUTOMATICO, Esforco.MEDIUM, Permissoes.padrao())));
        assertEquals("feature/login", servico.salvar(Projeto.novo("P", "P", "", repositorio.toString(),
                "feature/login", Provedor.AUTOMATICO, Esforco.MEDIUM, Permissoes.padrao())).branch());
    }
}
