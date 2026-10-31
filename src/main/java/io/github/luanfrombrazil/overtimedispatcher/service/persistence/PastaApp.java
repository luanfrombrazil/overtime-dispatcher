package io.github.luanfrombrazil.overtimedispatcher.service.persistence;

import java.nio.file.Path;

/**
 * Pasta central onde a app guarda seus dados.
 */
public final class PastaApp {

    private PastaApp() {
    }

    /** {@code %APPDATA%\OvertimeDispatcher} no Windows; {@code ~/.overtime-dispatcher} nos demais sistemas. */
    public static Path padrao() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "OvertimeDispatcher");
        }
        return Path.of(System.getProperty("user.home"), ".overtime-dispatcher");
    }
}
