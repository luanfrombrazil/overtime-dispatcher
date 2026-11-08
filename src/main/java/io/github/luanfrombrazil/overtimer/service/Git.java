package io.github.luanfrombrazil.overtimer.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
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
        ProcessBuilder construtor = new ProcessBuilder(comando).redirectErrorStream(true);
        // Sem terminal para responder: um pedido de usuário e senha travaria a chamada até o limite.
        construtor.environment().put("GIT_TERMINAL_PROMPT", "0");
        Process processo = construtor.start();
        processo.getOutputStream().close();
        // Lida à parte, para o limite valer mesmo que o git trave sem fechar a saída.
        FutureTask<byte[]> saida = new FutureTask<>(() -> processo.getInputStream().readAllBytes());
        Thread.ofVirtual().start(saida);
        if (!processo.waitFor(LIMITE_SEGUNDOS, TimeUnit.SECONDS)) {
            processo.descendants().forEach(ProcessHandle::destroyForcibly);
            processo.destroyForcibly();
            throw new IOException("O git não respondeu: " + String.join(" ", argumentos));
        }
        byte[] texto;
        try {
            texto = saida.get();
        } catch (ExecutionException e) {
            throw new IOException("Não foi possível ler a saída do git", e.getCause());
        }
        return new Saida(processo.exitValue(), new String(texto, StandardCharsets.UTF_8).strip());
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
