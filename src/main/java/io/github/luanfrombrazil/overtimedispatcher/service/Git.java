package io.github.luanfrombrazil.overtimedispatcher.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Chamadas ao git de linha de comando.
 */
public final class Git {

    private static final long LIMITE_SEGUNDOS = 120;

    private Git() {
    }

    /** Código de saída e texto (saída padrão e de erro juntas). */
    public record Saida(int codigo, String texto) {
        public boolean ok() {
            return codigo == 0;
        }
    }

    public static Saida executar(Path pasta, String... argumentos) throws IOException, InterruptedException {
        List<String> comando = new ArrayList<>(List.of("git", "-C", pasta.toString()));
        comando.addAll(List.of(argumentos));
        Process processo = new ProcessBuilder(comando).redirectErrorStream(true).start();
        processo.getOutputStream().close();
        byte[] saida = processo.getInputStream().readAllBytes();
        if (!processo.waitFor(LIMITE_SEGUNDOS, TimeUnit.SECONDS)) {
            processo.destroyForcibly();
            throw new IOException("O git não respondeu: " + String.join(" ", argumentos));
        }
        return new Saida(processo.exitValue(), new String(saida, StandardCharsets.UTF_8).strip());
    }

    /** Se a pasta é a raiz de um repositório git. */
    public static boolean repositorio(Path pasta) {
        return Files.exists(pasta.resolve(".git"));
    }

    /**
     * Branches locais do repositório, sem as {@code overtime/…} que a própria app cria para as Tasks.
     * Vazio se a pasta não é um repositório, se ainda não há commits ou se o git não está instalado.
     */
    public static List<String> branches(Path pasta) {
        if (!repositorio(pasta)) {
            return List.of();
        }
        try {
            Saida saida = executar(pasta, "for-each-ref", "--format=%(refname:short)", "refs/heads");
            if (!saida.ok()) {
                return List.of();
            }
            return saida.texto().lines()
                    .map(String::strip)
                    .filter(b -> !b.isEmpty() && !b.startsWith("overtime/"))
                    .toList();
        } catch (IOException e) {
            return List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }
}
