package io.github.luanfrombrazil.overtimer.service.persistence;

import io.github.luanfrombrazil.overtimer.service.Git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Pasta central onde a aplicação guarda os seus dados.
 */
public final class PastaDados {

    /** Propriedade do sistema que troca a pasta (útil em testes e para separar dados de desenvolvimento). */
    public static final String PROPRIEDADE = "overtimer.dados";

    private PastaDados() {
    }

    /** {@code -Dovertimer.dados}, senão {@code %APPDATA%\Overtimer}, senão {@code ~/.overtimer}. */
    public static Path padrao() {
        String definida = System.getProperty(PROPRIEDADE);
        if (definida != null && !definida.isBlank()) {
            return Path.of(definida);
        }
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "Overtimer");
        }
        return Path.of(System.getProperty("user.home"), ".overtimer");
    }

    /**
     * Traz os dados da pasta do nome antigo da app (Overtime Dispatcher), se ela existir e a nova ainda não.
     * Depois da mudança, repara os worktrees das Tasks para o git achá-los no lugar novo.
     */
    public static void migrarDoNomeAntigo(Path destino) {
        if (System.getProperty(PROPRIEDADE) != null || Files.exists(destino)) {
            return;
        }
        String appData = System.getenv("APPDATA");
        Path antiga = appData != null && !appData.isBlank()
                ? Path.of(appData, "OvertimeDispatcher")
                : Path.of(System.getProperty("user.home"), ".overtime-dispatcher");
        if (!Files.isDirectory(antiga)) {
            return;
        }
        try {
            Files.move(antiga, destino);
        } catch (IOException e) {
            System.err.println("Não foi possível mover os dados de " + antiga + " para " + destino + ": " + e);
            return;
        }
        Path worktrees = destino.resolve("worktrees");
        if (!Files.isDirectory(worktrees)) {
            return;
        }
        try (Stream<Path> pastas = Files.list(worktrees)) {
            for (Path pasta : pastas.filter(Files::isDirectory).toList()) {
                Git.executar(pasta, "worktree", "repair");
            }
        } catch (IOException e) {
            System.err.println("Não foi possível reparar os worktrees: " + e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
