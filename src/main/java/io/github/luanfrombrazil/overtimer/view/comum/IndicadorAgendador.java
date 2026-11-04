package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Símbolo de agendador pausado: duas barras de pausa. Enquanto roda, quem mostra é a borda girando do botão do
 * agendador ({@link AnelGiratorio}). O texto da situação só aparece ao passar o mouse, como dica, sem a espera
 * habitual.
 */
public class IndicadorAgendador extends JComponent {

    private static final int TAMANHO = 18;

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

    /** @param situacao texto mostrado ao passar o mouse, como "Pausado" */
    public void setSituacao(String situacao) {
        setToolTipText(situacao);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double lado = Math.min(getWidth(), getHeight());
        double x = (getWidth() - lado) / 2;
        double y = (getHeight() - lado) / 2;
        double largura = lado / 4.5;
        double altura = lado * 0.7;
        double topo = y + (lado - altura) / 2;
        double vao = lado / 6;
        double inicio = x + (lado - 2 * largura - vao) / 2;
        g2.setColor(Tema.TEXTO_SECUNDARIO);
        g2.fill(new RoundRectangle2D.Double(inicio, topo, largura, altura, 2, 2));
        g2.fill(new RoundRectangle2D.Double(inicio + largura + vao, topo, largura, altura, 2, 2));
        g2.dispose();
    }
}
