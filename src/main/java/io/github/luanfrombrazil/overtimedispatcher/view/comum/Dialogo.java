package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;

/**
 * Pop-up modal sem moldura do sistema, mostrado sobre a janela escurecida.
 */
public class Dialogo extends JDialog {

    private final JFrame dono;

    public Dialogo(JFrame dono, Color borda) {
        super(dono, true);
        this.dono = dono;
        setUndecorated(true);
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.PAINEL);
        raiz.setBorder(BorderFactory.createLineBorder(borda, 1));
        setContentPane(raiz);
        getRootPane().registerKeyboardAction(e -> fechar(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Mostra o pop-up centralizado, com o fundo da janela escurecido enquanto estiver aberto. */
    public void abrir() {
        pack();
        setLocationRelativeTo(dono);
        Component vidroAnterior = dono.getGlassPane();
        JPanel escurecer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 140));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        escurecer.setOpaque(false);
        escurecer.addMouseListener(new MouseAdapter() {
        });
        dono.setGlassPane(escurecer);
        escurecer.setVisible(true);
        try {
            setVisible(true);
        } finally {
            escurecer.setVisible(false);
            dono.setGlassPane(vidroAnterior);
        }
    }

    public void fechar() {
        setVisible(false);
        dispose();
    }
}
