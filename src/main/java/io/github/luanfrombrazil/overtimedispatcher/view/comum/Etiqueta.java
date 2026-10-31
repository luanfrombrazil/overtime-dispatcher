package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Etiqueta pequena com fundo sólido (provedor, esforço) ou só contorno (tipo de item do Oráculo).
 */
public class Etiqueta extends JLabel {

    private final Color fundo;
    private final Color contorno;

    private Etiqueta(String texto, Color fundo, Color contorno, Color cor) {
        super(texto);
        this.fundo = fundo;
        this.contorno = contorno;
        setFont(Tema.legenda());
        setForeground(cor);
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        Ui.fixo(this);
    }

    public static Etiqueta solida(String texto, Color fundo) {
        return new Etiqueta(texto, fundo, null, Tema.TEXTO);
    }

    public static Etiqueta contorno(String texto, Color cor) {
        return new Etiqueta(texto, null, cor, cor);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (fundo != null) {
            g2.setColor(fundo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        }
        if (contorno != null) {
            g2.setColor(contorno);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
