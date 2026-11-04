package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Símbolo do estado do agendador: duas barras de pausa quando parado, um círculo girando enquanto roda. O texto
 * da situação só aparece ao passar o mouse, como dica, sem a espera habitual.
 */
public class IndicadorAgendador extends JComponent {

    private static final int TAMANHO = 18;
    private static final int PASSO_GRAUS = 30;

    private boolean rodando;
    private int angulo;
    private final Timer giro = new Timer(80, e -> {
        angulo = (angulo + PASSO_GRAUS) % 360;
        repaint();
    });

    public IndicadorAgendador() {
        Dimension d = new Dimension(TAMANHO, TAMANHO);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        addMouseListener(new MouseAdapter() {
            private int esperaPadrao;

            @Override
            public void mouseEntered(MouseEvent e) {
                esperaPadrao = ToolTipManager.sharedInstance().getInitialDelay();
                ToolTipManager.sharedInstance().setInitialDelay(0);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ToolTipManager.sharedInstance().setInitialDelay(esperaPadrao);
            }
        });
    }

    /** @param situacao texto mostrado ao passar o mouse, como "Rodando · Task" ou "Pausado" */
    public void setEstado(boolean rodando, String situacao) {
        this.rodando = rodando;
        setToolTipText(situacao);
        atualizarGiro();
        repaint();
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

    /** O círculo só gira enquanto o agendador roda e o indicador está na tela. */
    private void atualizarGiro() {
        if (rodando && isDisplayable()) {
            giro.start();
        } else {
            giro.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double lado = Math.min(getWidth(), getHeight());
        double x = (getWidth() - lado) / 2;
        double y = (getHeight() - lado) / 2;
        if (rodando) {
            float traco = (float) (lado / 7);
            double margem = traco / 2 + 1;
            Ellipse2D trilho = new Ellipse2D.Double(x + margem, y + margem, lado - 2 * margem, lado - 2 * margem);
            g2.setStroke(new BasicStroke(traco, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Tema.BORDA);
            g2.draw(trilho);
            g2.setColor(Tema.ACENTO);
            g2.draw(new Arc2D.Double(trilho.getBounds2D(), -angulo, 100, Arc2D.OPEN));
        } else {
            double largura = lado / 4.5;
            double altura = lado * 0.7;
            double topo = y + (lado - altura) / 2;
            double vao = lado / 6;
            double inicio = x + (lado - 2 * largura - vao) / 2;
            g2.setColor(Tema.TEXTO_SECUNDARIO);
            g2.fill(new RoundRectangle2D.Double(inicio, topo, largura, altura, 2, 2));
            g2.fill(new RoundRectangle2D.Double(inicio + largura + vao, topo, largura, altura, 2, 2));
        }
        g2.dispose();
    }
}
