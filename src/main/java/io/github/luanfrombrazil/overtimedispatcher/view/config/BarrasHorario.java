package io.github.luanfrombrazil.overtimedispatcher.view.config;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Horário de funcionamento: uma barra vertical por dia, com 48 segmentos de 30 minutos.
 * Clicar e arrastar seleciona; se o primeiro segmento já estava selecionado, o arraste desmarca.
 */
class BarrasHorario extends JComponent {

    static final String[] DIAS = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
    private static final int SEGMENTOS = 48;
    private static final int ALTURA_SEGMENTO = 4;
    private static final int ESPACO_SEGMENTO = 1;
    private static final int LARGURA_DIA = 64;
    private static final int ESPACO_DIA = 10;
    private static final int EIXO = 34;
    private static final int TOPO = 24;

    private final boolean[][] selecionados = new boolean[DIAS.length][SEGMENTOS];
    private boolean modoArraste;
    private Runnable aoEditar = () -> {
    };

    BarrasHorario() {
        int largura = EIXO + DIAS.length * (LARGURA_DIA + ESPACO_DIA);
        int altura = TOPO + SEGMENTOS * (ALTURA_SEGMENTO + ESPACO_SEGMENTO) + 10;
        setPreferredSize(new Dimension(largura, altura));
        setMaximumSize(getPreferredSize());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int[] celula = celula(e.getX(), e.getY());
                if (celula != null) {
                    modoArraste = !selecionados[celula[0]][celula[1]];
                    marcar(celula);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int[] celula = celula(e.getX(), e.getY());
                if (celula != null) {
                    marcar(celula);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void aoEditar(Runnable acao) {
        this.aoEditar = acao;
    }

    /** Marca as faixas [início, fim) em horas, iguais para os dias indicados. */
    void definir(int dia, int horaInicio, int horaFim) {
        for (int s = horaInicio * 2; s < horaFim * 2; s++) {
            selecionados[dia][s] = true;
        }
        repaint();
    }

    void limpar() {
        for (boolean[] dia : selecionados) {
            java.util.Arrays.fill(dia, false);
        }
        repaint();
    }

    private void marcar(int[] celula) {
        selecionados[celula[0]][celula[1]] = modoArraste;
        aoEditar.run();
        repaint();
    }

    private int[] celula(int x, int y) {
        int dia = (x - EIXO) / (LARGURA_DIA + ESPACO_DIA);
        int dentro = (x - EIXO) % (LARGURA_DIA + ESPACO_DIA);
        int segmento = (y - TOPO) / (ALTURA_SEGMENTO + ESPACO_SEGMENTO);
        if (x < EIXO || dia >= DIAS.length || dentro > LARGURA_DIA || y < TOPO || segmento >= SEGMENTOS) {
            return null;
        }
        return new int[]{dia, segmento};
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(Tema.legenda());
        g2.setColor(Tema.TEXTO_SECUNDARIO);
        FontMetrics fm = g2.getFontMetrics();
        for (int h = 0; h <= 24; h += 6) {
            int y = TOPO + h * 2 * (ALTURA_SEGMENTO + ESPACO_SEGMENTO);
            g2.drawString(h + "h", 0, Math.min(getHeight() - 2, y + fm.getAscent() / 2));
        }

        g2.setFont(Tema.secao());
        FontMetrics fmDia = g2.getFontMetrics();
        for (int d = 0; d < DIAS.length; d++) {
            int x = EIXO + d * (LARGURA_DIA + ESPACO_DIA);
            g2.setColor(Tema.TEXTO);
            g2.drawString(DIAS[d], x + (LARGURA_DIA - fmDia.stringWidth(DIAS[d])) / 2, fmDia.getAscent());
            for (int s = 0; s < SEGMENTOS; s++) {
                g2.setColor(selecionados[d][s] ? Tema.ATIVIDADE[3] : Tema.ATIVIDADE[0]);
                g2.fillRoundRect(x, TOPO + s * (ALTURA_SEGMENTO + ESPACO_SEGMENTO), LARGURA_DIA, ALTURA_SEGMENTO, 3, 3);
            }
        }
        g2.dispose();
    }
}
