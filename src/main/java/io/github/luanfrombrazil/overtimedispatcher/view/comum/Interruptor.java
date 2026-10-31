package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Chave liga/desliga.
 */
public class Interruptor extends JComponent {

    private boolean ligado;

    public Interruptor(boolean ligado) {
        this.ligado = ligado;
        Dimension d = new Dimension(34, 18);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setLigado(!Interruptor.this.ligado);
            }
        });
    }

    public boolean isLigado() {
        return ligado;
    }

    public void setLigado(boolean ligado) {
        boolean antes = this.ligado;
        this.ligado = ligado;
        repaint();
        firePropertyChange("ligado", antes, ligado);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ligado ? Tema.ACENTO : Tema.SUTIL);
        g2.fillRoundRect(0, 0, 34, 18, 18, 18);
        if (!ligado) {
            g2.setColor(Tema.BORDA);
            g2.drawRoundRect(0, 0, 33, 17, 18, 18);
        }
        g2.setColor(Tema.TEXTO);
        g2.fillOval(ligado ? 19 : 3, 3, 12, 12);
        g2.dispose();
    }
}
