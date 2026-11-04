package io.github.luanfrombrazil.overtimer.view;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Janela principal: barra lateral (da altura da janela), barra superior e a área central, que troca entre Home,
 * Projeto, Protocolo e Trilha.
 */
public class MainFrame extends JFrame {

    private final JPanel centro = new JPanel(new BorderLayout());

    public MainFrame(BarraSuperior barraSuperior, BarraLateral barraLateral) {
        super("Overtimer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.APP);
        centro.setBackground(Tema.APP);
        // A barra lateral vai até o topo da janela; a superior fica só sobre a área central.
        JPanel direita = new JPanel(new BorderLayout());
        direita.setBackground(Tema.APP);
        direita.add(barraSuperior, BorderLayout.NORTH);
        direita.add(centro, BorderLayout.CENTER);
        barraLateral.alinharTopoCom(barraSuperior);
        raiz.add(barraLateral, BorderLayout.WEST);
        raiz.add(direita, BorderLayout.CENTER);
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
