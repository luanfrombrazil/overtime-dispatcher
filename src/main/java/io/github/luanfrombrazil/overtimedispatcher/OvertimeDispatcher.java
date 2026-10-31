package io.github.luanfrombrazil.overtimedispatcher;

import io.github.luanfrombrazil.overtimedispatcher.controller.MainController;
import io.github.luanfrombrazil.overtimedispatcher.service.prototipo.DadosPrototipo;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.SwingUtilities;

/**
 * Ponto de entrada da aplicação.
 */
public final class OvertimeDispatcher {

    private OvertimeDispatcher() {
    }

    public static void main(String[] args) {
        Tema.instalar();
        SwingUtilities.invokeLater(() -> new MainController(new DadosPrototipo()).iniciar());
    }
}
