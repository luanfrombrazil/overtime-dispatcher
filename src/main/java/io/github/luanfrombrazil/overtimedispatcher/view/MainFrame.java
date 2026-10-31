package io.github.luanfrombrazil.overtimedispatcher.view;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Janela principal: barra superior, barra lateral e a área central, que troca entre Home, Projeto, Protocolo e Trilha.
 */
public class MainFrame extends JFrame {

    private final JPanel centro = new JPanel(new BorderLayout());

    public MainFrame(BarraSuperior barraSuperior, BarraLateral barraLateral) {
        super("Overtime Dispatcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.APP);
        centro.setBackground(Tema.APP);
        raiz.add(barraSuperior, BorderLayout.NORTH);
        raiz.add(barraLateral, BorderLayout.WEST);
        raiz.add(centro, BorderLayout.CENTER);
        setContentPane(raiz);
    }

    /** Troca o conteúdo da área central. */
    public void mostrar(JComponent tela) {
        centro.removeAll();
        centro.add(tela, BorderLayout.CENTER);
        centro.revalidate();
        centro.repaint();
    }
}
