package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Ícone quadrado com as iniciais de um projeto, protocolo ou trilha.
 */
public class Iniciais extends JComponent {

    private String sigla;
    private final int tamanho;

    public Iniciais(String sigla, int tamanho) {
        this.sigla = sigla;
        this.tamanho = tamanho;
        Dimension d = new Dimension(tamanho, tamanho);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Tema.SUTIL);
        g2.fillRoundRect(0, 0, tamanho - 1, tamanho - 1, 10, 10);
        g2.setColor(Tema.BORDA);
        g2.drawRoundRect(0, 0, tamanho - 1, tamanho - 1, 10, 10);
        g2.setFont(Tema.secao());
        g2.setColor(Tema.TEXTO);
        FontMetrics fm = g2.getFontMetrics();
        int x = (tamanho - fm.stringWidth(sigla)) / 2;
        int y = (tamanho - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(sigla, x, y);
        g2.dispose();
    }

    /** Duas primeiras iniciais das palavras de um nome. */
    public static String de(String nome) {
        StringBuilder sb = new StringBuilder();
        for (String parte : nome.trim().split("\\s+")) {
            if (!parte.isEmpty() && Character.isLetterOrDigit(parte.charAt(0)) && parte.length() > 2) {
                sb.append(Character.toUpperCase(parte.charAt(0)));
            }
            if (sb.length() == 2) {
                break;
            }
        }
        if (sb.isEmpty() && !nome.isBlank()) {
            sb.append(Character.toUpperCase(nome.trim().charAt(0)));
        }
        return sb.toString();
    }
}
