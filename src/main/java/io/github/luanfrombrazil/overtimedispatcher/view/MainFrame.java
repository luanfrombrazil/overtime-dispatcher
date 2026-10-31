package io.github.luanfrombrazil.overtimedispatcher.view;

import javax.swing.JFrame;
import java.awt.Dimension;

/**
 * Janela principal da aplicação.
 */
public class MainFrame extends JFrame {

    public MainFrame() {
        super("Overtime Dispatcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);
    }
}
