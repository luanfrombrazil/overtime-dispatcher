package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Barra fina de porcentagem (ex.: consumo de um provedor).
 */
public class BarraProgresso extends JComponent {

    private final int porcentagem;
    private final Color cor;

    public BarraProgresso(int porcentagem, Color cor, int largura) {
        this.porcentagem = porcentagem;
        this.cor = cor;
        Dimension d = new Dimension(largura, 6);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Tema.SUTIL);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
        g2.setColor(cor);
        g2.fillRoundRect(0, 0, getWidth() * porcentagem / 100, getHeight(), 6, 6);
        g2.dispose();
    }
}
