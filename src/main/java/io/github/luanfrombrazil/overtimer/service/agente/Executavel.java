package io.github.luanfrombrazil.overtimer.service.agente;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Encontra um programa no PATH e devolve o começo da linha de comando para chamá-lo.
 */
final class Executavel {

    private Executavel() {
    }

    /**
     * No Windows, aceita {@code .exe} e também {@code .cmd}/{@code .bat} (instalações pelo npm),
     * que precisam passar pelo {@code cmd /c}.
     */
    static Optional<List<String>> localizar(String nome) {
        String path = System.getenv("PATH");
        if (path == null) {
            return Optional.empty();
        }
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        List<String> extensoes = windows ? List.of(".exe", ".cmd", ".bat") : List.of("");
        for (String pasta : path.split(File.pathSeparator)) {
            if (pasta.isBlank()) {
                continue;
            }
            for (String extensao : extensoes) {
                Path candidato;
                try {
                    candidato = Path.of(pasta.strip(), nome + extensao);
                } catch (RuntimeException e) {
                    continue;
                }
                if (Files.isRegularFile(candidato) && (windows || Files.isExecutable(candidato))) {
                    String caminho = candidato.toString();
                    return Optional.of(extensao.equals(".exe") || extensao.isEmpty()
                            ? List.of(caminho)
                            : List.of("cmd.exe", "/c", caminho));
                }
            }
        }
        return Optional.empty();
    }
}
