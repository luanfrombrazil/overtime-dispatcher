package io.github.luanfrombrazil.overtimedispatcher.view.trilha;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Fileira de blocos de uma tarefa: feitos (verde), na agenda (contorno azul) e a agendar (cinza).
 */
class Blocos extends JComponent {

    static final int LARGURA = 18;
    static final int ALTURA = 8;
    private static final int ESPACO = 3;

    private final int feitos;
    private final int naAgenda;
    private final int total;

    Blocos(int feitos, int naAgenda, int total) {
        this.feitos = feitos;
        this.naAgenda = naAgenda;
        this.total = total;
        Dimension d = new Dimension(Math.max(0, total * (LARGURA + ESPACO) - ESPACO), ALTURA + 2);
        setPreferredSize(d);
        setMaximumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < total; i++) {
            int x = i * (LARGURA + ESPACO);
            if (i < feitos) {
                g2.setColor(Tema.VERDE);
                g2.fillRoundRect(x, 1, LARGURA, ALTURA, 4, 4);
            } else if (i < feitos + naAgenda) {
                g2.setColor(Tema.ACENTO);
                g2.drawRoundRect(x + 1, 1, LARGURA - 2, ALTURA - 1, 4, 4);
            } else {
                g2.setColor(Tema.ATIVIDADE[0]);
                g2.fillRoundRect(x, 1, LARGURA, ALTURA, 4, 4);
            }
        }
        g2.dispose();
    }
}
