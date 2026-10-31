package io.github.luanfrombrazil.overtimedispatcher.view.projeto;

import io.github.luanfrombrazil.overtimedispatcher.model.Subtarefa;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.ItemStatus;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.CubicCurve2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Lista de subtarefas com o círculo de amarrar e o fio vermelho entre as dependentes.
 */
class SubtarefasPanel extends JPanel {

    private final List<Subtarefa> subtarefas;
    private final List<No> nos = new ArrayList<>();

    SubtarefasPanel(List<Subtarefa> subtarefas) {
        this.subtarefas = subtarefas;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));

        for (int i = 0; i < subtarefas.size(); i++) {
            Subtarefa s = subtarefas.get(i);
            boolean amarrada = s.dependeDe() >= 0 || dependentes(i);
            No no = new No(amarrada);
            nos.add(no);

            JPanel linha = new JPanel(new BorderLayout(10, 0));
            linha.setOpaque(false);
            JPanel envoltorioNo = new JPanel(new java.awt.GridBagLayout());
            envoltorioNo.setOpaque(false);
            envoltorioNo.add(no);
            linha.add(envoltorioNo, BorderLayout.WEST);
            linha.add(new ItemStatus(s.estado(), s.titulo(), s.detalhe(), Tema.SUTIL), BorderLayout.CENTER);
            linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, linha.getPreferredSize().height));
            if (i > 0) {
                add(Box.createVerticalStrut(4));
            }
            add(linha);
        }
    }

    private boolean dependentes(int indice) {
        return subtarefas.stream().anyMatch(s -> s.dependeDe() == indice);
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Tema.FIO);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < subtarefas.size(); i++) {
            int origem = subtarefas.get(i).dependeDe();
            if (origem < 0) {
                continue;
            }
            Point a = centro(nos.get(origem));
            Point b = centro(nos.get(i));
            double curva = 12 + Math.abs(b.y - a.y) * 0.08;
            g2.draw(new CubicCurve2D.Double(a.x, a.y, a.x - curva, a.y + (b.y - a.y) * 0.35,
                    b.x - curva, a.y + (b.y - a.y) * 0.65, b.x, b.y));
        }
        g2.dispose();
        for (No no : nos) {
            if (no.amarrado) {
                Point p = centro(no);
                Graphics2D gn = (Graphics2D) g.create();
                gn.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gn.setColor(Tema.FIO);
                gn.fillOval(p.x - 7, p.y - 7, 14, 14);
                gn.dispose();
            }
        }
    }

    private Point centro(JComponent componente) {
        return SwingUtilities.convertPoint(componente, componente.getWidth() / 2, componente.getHeight() / 2, this);
    }

    /** Círculo onde se amarra o fio. */
    private static class No extends JComponent {

        private final boolean amarrado;

        No(boolean amarrado) {
            this.amarrado = amarrado;
            Dimension d = new Dimension(14, 14);
            setPreferredSize(d);
            setMinimumSize(d);
            setToolTipText("Arraste até outra subtarefa para amarrar");
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (amarrado) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.SUTIL);
            g2.fillOval(1, 1, 12, 12);
            g2.setColor(Tema.TEXTO_SECUNDARIO);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(1, 1, 12, 12);
            g2.dispose();
        }
    }
}
