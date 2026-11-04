package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

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
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Pop-up modal sem moldura do sistema, mostrado sobre a janela escurecida.
 */
public class Dialogo extends JDialog {

    private final JFrame dono;
    private Runnable aoClicarFora;
    private boolean saindo;

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

    /**
     * Faz um clique fora do pop-up, em qualquer ponto da janela, rodar {@code acao} (que deve fechá-lo) em vez de
     * ser ignorado. Para a janela receber o clique, o pop-up deixa de ser modal; {@link #abrir()} continua
     * esperando ele fechar. Chamar antes de abrir.
     */
    protected void aoClicarFora(Runnable acao) {
        this.aoClicarFora = acao;
        setModal(false);
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                // Só a própria janela conta: avisos abertos pelo pop-up ou outro programa não descartam nada.
                if (e.getOppositeWindow() == dono) {
                    clicouFora();
                }
            }
        });
    }

    private void clicouFora() {
        if (aoClicarFora != null && !saindo && isVisible()) {
            saindo = true;
            aoClicarFora.run();
        }
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
            @Override
            public void mousePressed(MouseEvent e) {
                clicouFora();
            }
        });
        dono.setGlassPane(escurecer);
        escurecer.setVisible(true);
        try {
            if (isModal()) {
                setVisible(true);
            } else {
                SecondaryLoop espera = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
                addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        espera.exit();
                    }
                });
                setVisible(true);
                espera.enter();
            }
        } finally {
            escurecer.setVisible(false);
            dono.setGlassPane(vidroAnterior);
        }
    }

    public void fechar() {
        saindo = true;
        setVisible(false);
        dispose();
    }
}
