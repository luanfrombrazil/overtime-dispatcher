package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Reordena as linhas de um painel vertical arrastando pela alça de três barras no fim de cada linha.
 * Durante o arrasto, um traço na cor de destaque mostra onde a linha vai cair. O painel deve chamar
 * {@link #desenhar} no seu {@code paintChildren}.
 */
public final class Reordenacao {

    /** Recebe a posição de origem e a posição final da linha solta. */
    @FunctionalInterface
    public interface AoMover {
        void mover(int origem, int destino);
    }

    private final JComponent painel;
    private final AoMover aoMover;
    private final List<JComponent> alcas = new ArrayList<>();
    private int origem = -1;
    private int vaga = -1;

    public Reordenacao(JComponent painel, AoMover aoMover) {
        this.painel = painel;
        this.aoMover = aoMover;
    }

    /**
     * Alça da próxima linha. Ela deve ficar dentro da linha, e a linha deve ser filha direta do painel.
     *
     * @param dica texto mostrado ao passar o mouse na alça
     */
    public BotaoIcone alca(String dica) {
        int indice = alcas.size();
        BotaoIcone alca = new BotaoIcone(Ui.icone("arrastar", 14, Tema.TEXTO_SECUNDARIO), dica, 24);
        alca.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        MouseAdapter arrasto = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                origem = indice;
                vaga = -1;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (origem < 0) {
                    return;
                }
                Point p = SwingUtilities.convertPoint(alca, e.getPoint(), painel);
                vaga = vagaEm(p.y);
                // Rola a lista quando o arrasto chega perto da borda.
                painel.scrollRectToVisible(new Rectangle(p.x, p.y - 20, 1, 40));
                painel.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int de = origem;
                int para = vaga > de ? vaga - 1 : vaga;
                origem = -1;
                vaga = -1;
                painel.repaint();
                if (de >= 0 && para >= 0 && para != de) {
                    aoMover.mover(de, para);
                }
            }
        };
        alca.addMouseListener(arrasto);
        alca.addMouseMotionListener(arrasto);
        alcas.add(alca);
        return alca;
    }

    /** Desenha onde a linha arrastada vai cair, se houver um arrasto em andamento. */
    public void desenhar(Graphics g) {
        if (origem < 0 || vaga < 0 || vaga == origem || vaga == origem + 1) {
            return;
        }
        List<JComponent> linhas = linhas();
        int y;
        if (vaga == 0) {
            y = linhas.getFirst().getY() - 2;
        } else if (vaga == linhas.size()) {
            JComponent ultima = linhas.getLast();
            y = ultima.getY() + ultima.getHeight() + 2;
        } else {
            JComponent acima = linhas.get(vaga - 1);
            y = (acima.getY() + acima.getHeight() + linhas.get(vaga).getY()) / 2;
        }
        JComponent referencia = linhas.get(Math.min(vaga, linhas.size() - 1));
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Tema.ACENTO);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(referencia.getX() + 2, y, referencia.getX() + referencia.getWidth() - 2, y);
        g2.dispose();
    }

    /** Vaga entre linhas (0 = antes da primeira, n = depois da última) mais próxima da altura {@code y}. */
    private int vagaEm(int y) {
        int vaga = 0;
        List<JComponent> linhas = linhas();
        for (JComponent linha : linhas) {
            if (y > linha.getY() + linha.getHeight() / 2) {
                vaga++;
            }
        }
        return vaga;
    }

    /** Linhas do painel, na ordem das alças: a filha direta do painel que contém cada alça. */
    private List<JComponent> linhas() {
        List<JComponent> linhas = new ArrayList<>();
        for (JComponent alca : alcas) {
            Component c = alca;
            while (c.getParent() != painel) {
                c = c.getParent();
            }
            linhas.add((JComponent) c);
        }
        return linhas;
    }
}
