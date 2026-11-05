package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Envolve um componente (o botão do agendador) com uma borda que gira enquanto algo está em andamento: o trilho
 * cinza com um trecho de acento dando a volta, como o antigo círculo de "rodando".
 */
public class AnelGiratorio extends JPanel {

    private static final int PASSO_GRAUS = 30;
    private static final float TRACO = 2f;
    /** Espaço entre o componente e o anel. */
    private static final int FOLGA = 2;
    private static final int MARGEM = (int) Math.ceil(TRACO) + FOLGA;

    private final int arco;
    private boolean girando;
    private int angulo;
    private final Timer giro = new Timer(80, e -> {
        angulo = (angulo + PASSO_GRAUS) % 360;
        repaint();
    });

    /** @param arco arredondamento dos cantos do componente envolvido, em pixels */
    public AnelGiratorio(JComponent componente, int arco) {
        super(new BorderLayout());
        this.arco = arco;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(MARGEM, MARGEM, MARGEM, MARGEM));
        add(componente, BorderLayout.CENTER);
    }

    public void setGirando(boolean girando) {
        this.girando = girando;
        atualizarGiro();
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        atualizarGiro();
    }

    @Override
    public void removeNotify() {
        giro.stop();
        super.removeNotify();
    }

    /** O anel só gira enquanto está ligado e na tela. */
    private void atualizarGiro() {
        if (girando && isDisplayable()) {
            giro.start();
        } else {
            giro.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!girando) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double meio = TRACO / 2.0;
        double curva = arco + 2.0 * (MARGEM - meio);
        RoundRectangle2D trilho = new RoundRectangle2D.Double(meio, meio, getWidth() - TRACO, getHeight() - TRACO,
                curva, curva);
        g2.setStroke(new BasicStroke(TRACO, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Tema.BORDA);
        g2.draw(trilho);
        // O trecho de acento é o trilho recortado por uma fatia de 100° que gira em torno do centro.
        double raio = Math.hypot(getWidth(), getHeight());
        g2.clip(new Arc2D.Double(getWidth() / 2.0 - raio, getHeight() / 2.0 - raio, 2 * raio, 2 * raio,
                -angulo, 100, Arc2D.PIE));
        g2.setColor(Tema.ACENTO);
        g2.draw(trilho);
        g2.dispose();
    }
}
