package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Rótulo arredondado, opcionalmente selecionável (filtros, horários, origens).
 */
public class Chip extends JLabel {

    private boolean selecionado;
    private final Color corSelecionado;
    private final Color fundoSelecionado;

    public Chip(String texto, boolean selecionado) {
        this(texto, selecionado, Tema.ACENTO, Tema.SELECIONADO);
    }

    public Chip(String texto, boolean selecionado, Color corSelecionado, Color fundoSelecionado) {
        super(texto);
        this.selecionado = selecionado;
        this.corSelecionado = corSelecionado;
        this.fundoSelecionado = fundoSelecionado;
        setFont(Tema.legenda());
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        atualizarCor();
    }

    /** Torna o chip clicável. */
    public Chip aoClicar(Runnable acao) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                acao.run();
            }
        });
        return this;
    }

    public boolean isSelecionado() {
        return selecionado;
    }

    public void setSelecionado(boolean selecionado) {
        this.selecionado = selecionado;
        atualizarCor();
        repaint();
    }

    private void atualizarCor() {
        setForeground(selecionado ? Tema.TEXTO : Tema.TEXTO_SECUNDARIO);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arco = getHeight();
        g2.setColor(selecionado ? fundoSelecionado : Tema.SUTIL);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arco, arco);
        g2.setColor(selecionado ? corSelecionado : Tema.BORDA);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arco, arco);
        g2.dispose();
        super.paintComponent(g);
    }
}
