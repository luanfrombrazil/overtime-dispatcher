package io.github.luanfrombrazil.overtimer.view.comum;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Botão quadrado só com ícone, com contador opcional (ex.: notificações).
 */
public class BotaoIcone extends JButton {

    private int contador;
    private boolean ativo;
    private Color borda = Tema.BORDA;

    public BotaoIcone(Icon icone, String dica) {
        this(icone, dica, 32);
    }

    /** @param tamanho lado do botão, em pixels */
    public BotaoIcone(Icon icone, String dica, int tamanho) {
        super(icone);
        setToolTipText(dica);
        setFocusable(false);
        Dimension d = new Dimension(tamanho, tamanho);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        aplicarEstilo();
    }

    public void setContador(int contador) {
        this.contador = contador;
        repaint();
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
        aplicarEstilo();
    }

    /** Troca a cor da borda (a padrão é {@link Tema#BORDA}). */
    public void setBorda(Color borda) {
        this.borda = borda;
        aplicarEstilo();
    }

    private void aplicarEstilo() {
        putClientProperty(FlatClientProperties.STYLE,
                "arc:8; margin:0,0,0,0; focusWidth:0; innerFocusWidth:0; borderWidth:1; borderColor:" + hex(borda)
                        + "; background:" + (ativo ? "#2E436E" : "#2B2D30"));
    }

    private static String hex(Color cor) {
        return String.format("#%06X", cor.getRGB() & 0xFFFFFF);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (contador <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(Tema.legenda().deriveFont(10f));
        FontMetrics fm = g2.getFontMetrics();
        String texto = String.valueOf(contador);
        int w = Math.max(14, fm.stringWidth(texto) + 6);
        int x = getWidth() - w + 2;
        g2.setColor(Tema.VERMELHO);
        g2.fillRoundRect(x, 0, w, 14, 14, 14);
        g2.setColor(Tema.TEXTO);
        g2.drawString(texto, x + (w - fm.stringWidth(texto)) / 2, (14 - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
