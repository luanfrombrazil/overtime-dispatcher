package io.github.luanfrombrazil.overtimedispatcher.view.config;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

/**
 * Controle com duas alças que divide a janela de uso de um provedor em:
 * automação (azul), Oráculo (roxo) e reserva do usuário (cinza).
 */
class ReservaSlider extends JComponent {

    private static final int RAIO = 7;

    private int automacao;
    private int oraculo;
    private int arrastando = -1;
    private BiConsumer<Integer, Integer> aoMudar = (o, v) -> {
    };

    ReservaSlider(int automacao, int oraculo) {
        this.automacao = automacao;
        this.oraculo = oraculo;
        setPreferredSize(new Dimension(300, 18));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x1 = posicao(ReservaSlider.this.automacao);
                int x2 = posicao(ReservaSlider.this.automacao + ReservaSlider.this.oraculo);
                arrastando = Math.abs(e.getX() - x1) <= Math.abs(e.getX() - x2) ? 0 : 1;
                mover(e.getX());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mover(e.getX());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                arrastando = -1;
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** Recebe (oráculo, você) em porcentagem sempre que uma alça se move. */
    void aoMudar(BiConsumer<Integer, Integer> ouvinte) {
        this.aoMudar = ouvinte;
        ouvinte.accept(oraculo, voce());
    }

    int voce() {
        return 100 - automacao - oraculo;
    }

    private void mover(int x) {
        int valor = Math.max(0, Math.min(100, Math.round((x - RAIO) * 100f / (getWidth() - 2 * RAIO))));
        if (arrastando == 0) {
            int fim = automacao + oraculo;
            automacao = Math.min(valor, fim);
            oraculo = fim - automacao;
        } else if (arrastando == 1) {
            oraculo = Math.max(0, valor - automacao);
        }
        aoMudar.accept(oraculo, voce());
        repaint();
    }

    private int posicao(int porcentagem) {
        return RAIO + Math.round((getWidth() - 2 * RAIO) * porcentagem / 100f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int y = getHeight() / 2 - 3;
        int inicio = RAIO;
        int x1 = posicao(automacao);
        int x2 = posicao(automacao + oraculo);
        int fim = getWidth() - RAIO;

        g2.setColor(Tema.SUTIL);
        g2.fillRoundRect(inicio, y, fim - inicio, 6, 6, 6);
        g2.setColor(Tema.ACENTO);
        g2.fillRoundRect(inicio, y, x1 - inicio, 6, 6, 6);
        g2.setColor(Tema.ROXO);
        g2.fillRect(x1, y, x2 - x1, 6);

        int cy = getHeight() / 2;
        g2.setColor(Tema.TEXTO);
        g2.fillOval(x1 - RAIO, cy - RAIO, 2 * RAIO, 2 * RAIO);
        g2.setColor(Tema.ROXO_CLARO);
        g2.fillOval(x2 - RAIO, cy - RAIO, 2 * RAIO, 2 * RAIO);
        g2.setColor(Tema.ROXO);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(x2 - RAIO, cy - RAIO, 2 * RAIO, 2 * RAIO);
        g2.dispose();
    }
}
