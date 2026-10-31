package io.github.luanfrombrazil.overtimedispatcher;

import com.formdev.flatlaf.FlatDarkLaf;
import io.github.luanfrombrazil.overtimedispatcher.view.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Ponto de entrada da aplicação.
 */
public final class OvertimeDispatcher {

    private OvertimeDispatcher() {
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
