package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.Arrays;

/**
 * Gráfico de linha com pontos sobre fundo quadriculado.
 */
public class GraficoLinha extends JComponent {

    private static final int GRADE = 12;
    private static final int MARGEM = 6;

    private final int[] valores;
    private final Color cor;

    public GraficoLinha(int[] valores, Color cor, int altura) {
        this.valores = valores;
        this.cor = cor;
        setPreferredSize(new Dimension(120, altura));
        setMinimumSize(new Dimension(40, altura));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, altura));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        g2.setColor(new Color(0x232528));
        g2.fillRoundRect(0, 0, w, h, 8, 8);
        g2.setColor(new Color(0x34373C));
        for (int x = 0; x <= w; x += GRADE) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y <= h; y += GRADE) {
            g2.drawLine(0, y, w, y);
        }

        int max = Math.max(1, Arrays.stream(valores).max().orElse(1));
        double passo = (w - 2.0 * MARGEM) / Math.max(1, valores.length - 1);
        double[] xs = new double[valores.length];
        double[] ys = new double[valores.length];
        Path2D linha = new Path2D.Double();
        for (int i = 0; i < valores.length; i++) {
            xs[i] = MARGEM + i * passo;
            ys[i] = h - MARGEM - (valores[i] / (double) max) * (h - 2.0 * MARGEM);
            if (i == 0) {
                linha.moveTo(xs[i], ys[i]);
            } else {
                linha.lineTo(xs[i], ys[i]);
            }
        }
        Path2D area = new Path2D.Double(linha);
        area.lineTo(xs[xs.length - 1], h);
        area.lineTo(xs[0], h);
        area.closePath();
        g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 40));
        g2.fill(area);
        g2.setColor(cor);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(linha);

        for (int i = 0; i < valores.length; i++) {
            boolean ultimo = i == valores.length - 1;
            double r = ultimo ? 3.5 : 2.2;
            g2.setColor(ultimo ? Tema.TEXTO : cor);
            g2.fill(new java.awt.geom.Ellipse2D.Double(xs[i] - r, ys[i] - r, 2 * r, 2 * r));
            if (ultimo) {
                g2.setColor(cor);
                g2.draw(new java.awt.geom.Ellipse2D.Double(xs[i] - r, ys[i] - r, 2 * r, 2 * r));
            }
        }
        g2.dispose();
    }
}
