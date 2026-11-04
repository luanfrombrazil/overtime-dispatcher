package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;

/**
 * Desenha o fio vermelho que amarra as subtarefas com textura de corda (fios torcidos em diagonal) e os nós nas
 * pontas.
 */
public final class Corda {

    private static final float LARGURA = 4.5f;
    /** Distância, ao longo da corda, entre duas torções. */
    private static final double PASSO_TORCAO = 4.5;
    private static final Color SOMBRA = misturar(Tema.FIO, Color.BLACK, 0.45);
    private static final Color BRILHO = misturar(Tema.FIO, Color.WHITE, 0.35);

    private Corda() {
    }

    /** Desenha a corda ao longo do caminho. */
    public static void desenhar(Graphics2D g, Shape caminho) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(SOMBRA);
        g2.setStroke(new BasicStroke(LARGURA + 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(caminho);
        g2.setColor(Tema.FIO);
        g2.setStroke(new BasicStroke(LARGURA, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(caminho);
        torcoes(g2, caminho);
        g2.dispose();
    }

    /** Nó da corda centrado em ({@code x}, {@code y}), no lugar da bolinha de uma subtarefa amarrada. */
    public static void no(Graphics2D g, double x, double y, double raio) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(SOMBRA);
        g2.fill(new Ellipse2D.Double(x - raio, y - raio, 2 * raio, 2 * raio));
        // Três voltas da corda cruzadas, como num nó apertado.
        double comprimento = raio * 1.5;
        double espessura = raio * 0.8;
        for (int i = 0; i < 3; i++) {
            AffineTransform antes = g2.getTransform();
            g2.rotate(Math.toRadians(30 + i * 60), x, y);
            Ellipse2D volta = new Ellipse2D.Double(x - comprimento / 2, y - espessura / 2, comprimento, espessura);
            g2.setColor(Tema.FIO);
            g2.fill(volta);
            g2.setColor(SOMBRA);
            g2.setStroke(new BasicStroke(0.9f));
            g2.draw(volta);
            g2.setColor(BRILHO);
            g2.draw(new Line2D.Double(x - comprimento / 4, y - espessura / 5, x + comprimento / 4, y - espessura / 5));
            g2.setTransform(antes);
        }
        g2.setColor(SOMBRA);
        g2.fill(new Ellipse2D.Double(x - raio * 0.22, y - raio * 0.22, raio * 0.44, raio * 0.44));
        g2.dispose();
    }

    /** Traços diagonais ao longo do caminho, alternando sombra e brilho, que imitam os fios torcidos. */
    private static void torcoes(Graphics2D g2, Shape caminho) {
        g2.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double meia = LARGURA / 2;
        double[] p = new double[6];
        double xAnterior = 0;
        double yAnterior = 0;
        double resto = PASSO_TORCAO / 2;
        for (PathIterator it = caminho.getPathIterator(null, 0.5); !it.isDone(); it.next()) {
            int tipo = it.currentSegment(p);
            if (tipo == PathIterator.SEG_MOVETO) {
                xAnterior = p[0];
                yAnterior = p[1];
                continue;
            }
            if (tipo != PathIterator.SEG_LINETO) {
                continue;
            }
            double dx = p[0] - xAnterior;
            double dy = p[1] - yAnterior;
            double trecho = Math.hypot(dx, dy);
            if (trecho > 0) {
                double tx = dx / trecho;
                double ty = dy / trecho;
                double nx = -ty;
                double ny = tx;
                for (double d = resto; d < trecho; d += PASSO_TORCAO) {
                    double cx = xAnterior + tx * d;
                    double cy = yAnterior + ty * d;
                    // Inclinado no sentido da corda: de um lado e um pouco atrás até o outro e um pouco à frente.
                    g2.setColor(SOMBRA);
                    g2.draw(new Line2D.Double(cx - nx * meia - tx * 1.6, cy - ny * meia - ty * 1.6,
                            cx + nx * meia + tx * 1.6, cy + ny * meia + ty * 1.6));
                    g2.setColor(BRILHO);
                    g2.draw(new Line2D.Double(cx - nx * meia * 0.5 + tx * 0.6, cy - ny * meia * 0.5 + ty * 0.6,
                            cx + nx * meia * 0.2 + tx * 1.6, cy + ny * meia * 0.2 + ty * 1.6));
                }
                // Quanto falta, no próximo trecho, para a próxima torção.
                resto = trecho < resto ? resto - trecho : PASSO_TORCAO - ((trecho - resto) % PASSO_TORCAO);
            }
            xAnterior = p[0];
            yAnterior = p[1];
        }
    }

    private static Color misturar(Color base, Color outra, double fracao) {
        return new Color(
                (int) Math.round(base.getRed() * (1 - fracao) + outra.getRed() * fracao),
                (int) Math.round(base.getGreen() * (1 - fracao) + outra.getGreen() * fracao),
                (int) Math.round(base.getBlue() * (1 - fracao) + outra.getBlue() * fracao));
    }
}
