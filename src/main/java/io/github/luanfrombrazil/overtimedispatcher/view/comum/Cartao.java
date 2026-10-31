package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * Painel com fundo e borda arredondados.
 */
public class Cartao extends JPanel {

    private final Color fundo;
    private final Color borda;
    private final int raio;

    public Cartao() {
        this(Tema.PAINEL, Tema.BORDA, new BorderLayout());
    }

    public Cartao(Color fundo, Color borda, LayoutManager layout) {
        this(fundo, borda, 6, layout);
    }

    public Cartao(Color fundo, Color borda, int raio, LayoutManager layout) {
        super(layout);
        this.fundo = fundo;
        this.borda = borda;
        this.raio = raio;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fundo);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, raio * 2, raio * 2);
        if (borda != null) {
            g2.setColor(borda);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, raio * 2, raio * 2);
        }
        g2.dispose();
    }
}
