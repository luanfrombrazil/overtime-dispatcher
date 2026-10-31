package io.github.luanfrombrazil.overtimedispatcher.service.persistence;

import java.nio.file.Path;

/**
 * Pasta central onde a aplicação guarda os seus dados.
 */
public final class PastaDados {

    /** Propriedade do sistema que troca a pasta (útil em testes e para separar dados de desenvolvimento). */
    public static final String PROPRIEDADE = "overtime.dados";

    private PastaDados() {
    }

    /** {@code -Dovertime.dados}, senão {@code %APPDATA%\OvertimeDispatcher}, senão {@code ~/.overtime-dispatcher}. */
    public static Path padrao() {
        String definida = System.getProperty(PROPRIEDADE);
        if (definida != null && !definida.isBlank()) {
            return Path.of(definida);
        }
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "OvertimeDispatcher");
        }
        return Path.of(System.getProperty("user.home"), ".overtime-dispatcher");
    }
}
