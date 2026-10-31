package io.github.luanfrombrazil.overtimedispatcher.view.home;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Grade de atividade do último ano, no estilo das contribuições do GitHub.
 */
public class MapaAtividade extends JComponent {

    static final int CELULA = 10;
    static final int ESPACO = 3;
    private static final int ESQUERDA = 30;
    private static final int TOPO = 16;
    private static final String[] MESES = {"Nov", "Dez", "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out"};

    private final int[][] niveis;

    public MapaAtividade(int[][] niveis) {
        this.niveis = niveis;
        Dimension d = new Dimension(ESQUERDA + niveis.length * (CELULA + ESPACO), TOPO + 7 * (CELULA + ESPACO));
        setPreferredSize(d);
        setMinimumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Tema.legenda());
        g2.setColor(Tema.TEXTO_SECUNDARIO);
        int ascent = g2.getFontMetrics().getAscent();
        for (int i = 0; i < MESES.length; i++) {
            int x = ESQUERDA + Math.round(i * niveis.length / 12f) * (CELULA + ESPACO);
            g2.drawString(MESES[i], x, ascent);
        }
        String[] dias = {"Seg", "Qua", "Sex"};
        int[] linhas = {1, 3, 5};
        for (int i = 0; i < dias.length; i++) {
            g2.drawString(dias[i], 0, TOPO + linhas[i] * (CELULA + ESPACO) + CELULA - 1);
        }
        for (int s = 0; s < niveis.length; s++) {
            for (int d = 0; d < 7; d++) {
                g2.setColor(Tema.ATIVIDADE[niveis[s][d]]);
                g2.fillRoundRect(ESQUERDA + s * (CELULA + ESPACO), TOPO + d * (CELULA + ESPACO), CELULA, CELULA, 4, 4);
            }
        }
        g2.dispose();
    }

    /** Legenda "Menos ▢▢▢▢▢ Mais". */
    static JComponent legenda() {
        JComponent legenda = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(Tema.legenda());
                g2.setColor(Tema.TEXTO_SECUNDARIO);
                int base = (getHeight() + g2.getFontMetrics().getAscent()) / 2 - 1;
                g2.drawString("Menos", 0, base);
                int x = g2.getFontMetrics().stringWidth("Menos") + 6;
                for (java.awt.Color cor : Tema.ATIVIDADE) {
                    g2.setColor(cor);
                    g2.fillRoundRect(x, (getHeight() - CELULA) / 2, CELULA, CELULA, 4, 4);
                    x += CELULA + 3;
                }
                g2.setColor(Tema.TEXTO_SECUNDARIO);
                g2.drawString("Mais", x + 3, base);
                g2.dispose();
            }
        };
        legenda.setPreferredSize(new Dimension(140, 16));
        legenda.setMaximumSize(legenda.getPreferredSize());
        return legenda;
    }
}
