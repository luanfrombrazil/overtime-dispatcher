package io.github.luanfrombrazil.overtimer.view.projeto;

import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.view.comum.BotaoIcone;
import io.github.luanfrombrazil.overtimer.view.comum.Corda;
import io.github.luanfrombrazil.overtimer.view.comum.ItemStatus;
import io.github.luanfrombrazil.overtimer.view.comum.Reordenacao;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Lista de subtarefas com o círculo de amarrar e a corda vermelha entre as dependentes, com um nó em cada
 * ponta amarrada.
 * Arrastar de um círculo (origem) até outra subtarefa (destino) faz o destino depender da origem, ou desamarra, se
 * já estavam amarradas; arrastar pela alça de três barras, no fim da linha, muda a ordem.
 */
class SubtarefasPanel extends JPanel {

    /** Alinha as perguntas sob o item da subtarefa, um pouco mais para dentro, como as subtarefas sob a Task. */
    private static final int RECUO_PERGUNTAS = 48;

    private final List<Subtarefa> subtarefas;
    /** Perguntas de cada subtarefa que espera direcionamento, pelo id. */
    private final Map<String, DirecionamentoPanel> perguntas = new HashMap<>();
    private final Map<String, Integer> indices = new HashMap<>();
    private final List<No> nos = new ArrayList<>();
    private final List<JPanel> linhas = new ArrayList<>();
    private final BiConsumer<Subtarefa, Subtarefa> aoAmarrar;
    private final Reordenacao reordenacao;

    private int origemArrasto = -1;
    private Point ponteiro;

    /**
     * @param aoMover        recebe a subtarefa solta e quantas posições ela andou (negativo sobe)
     * @param aoTentarDeNovo chamado pelo botão de seta circular das subtarefas que falharam
     * @param recolhidas     ids das subtarefas com perguntas que você recolheu; as demais com perguntas abrem
     *                       expandidas. O conjunto é alterado aqui, pela seta de cada uma
     * @param aoResponder    recebe a subtarefa e as suas respostas às perguntas dela
     */
    SubtarefasPanel(List<Subtarefa> subtarefas, BiConsumer<Subtarefa, Subtarefa> aoAmarrar,
                    BiConsumer<Subtarefa, Integer> aoMover, Consumer<Subtarefa> aoClicar,
                    Consumer<Subtarefa> aoTentarDeNovo, Set<String> recolhidas,
                    BiConsumer<Subtarefa, List<String>> aoResponder) {
        this.subtarefas = subtarefas;
        this.aoAmarrar = aoAmarrar;
        // Só as subtarefas não finalizadas têm alça; as posições da reordenação contam apenas elas.
        List<Subtarefa> moveis = subtarefas.stream().filter(s -> !s.finalizada()).toList();
        this.reordenacao = new Reordenacao(this, (de, para) -> aoMover.accept(moveis.get(de),
                indices.get(moveis.get(para).id()) - indices.get(moveis.get(de).id())));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
        for (int i = 0; i < subtarefas.size(); i++) {
            indices.put(subtarefas.get(i).id(), i);
        }

        for (int i = 0; i < subtarefas.size(); i++) {
            Subtarefa s = subtarefas.get(i);
            boolean amarrada = !s.dependeDe().isEmpty() || temDependentes(s);
            No no = new No(amarrada);
            arrastavel(no, i);
            nos.add(no);

            JPanel linha = new JPanel(new BorderLayout(10, 0));
            linha.setOpaque(false);
            JPanel envoltorioNo = new JPanel(new java.awt.GridBagLayout());
            envoltorioNo.setOpaque(false);
            envoltorioNo.add(no);
            linha.add(envoltorioNo, BorderLayout.WEST);
            ItemStatus item = new ItemStatus(s.estado(), s.titulo(), s.detalhe(), Tema.SUTIL);
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            item.setToolTipText(s.finalizada() ? "Finalizada; clique para ver" : "Clique para editar");
            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    aoClicar.accept(s);
                }
            });
            // A finalizada fica travada: no lugar da alça, a barra verde mostra a conclusão.
            if (s.estado() == Estado.ERRO) {
                item.comDireita(TaskCard.botaoTentarDeNovo("Tentar de novo", () -> aoTentarDeNovo.accept(s)),
                        reordenacao.alca("Arraste para mudar a ordem"));
            } else if (s.finalizada()) {
                item.comDireita(TaskCard.botaoConclusao(s.titulo(), s.resultado()));
            } else if (s.aguardaDirecionamento()) {
                item.comDireita(seta(s, recolhidas), reordenacao.alca("Arraste para mudar a ordem"));
            } else {
                item.comDireita(reordenacao.alca("Arraste para mudar a ordem"));
            }
            linha.add(item, BorderLayout.CENTER);
            linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, linha.getPreferredSize().height));
            if (i > 0) {
                add(Box.createVerticalStrut(4));
            }
            linhas.add(linha);
            add(linha);
            if (s.aguardaDirecionamento()) {
                // Como a Task mostra as subtarefas, a subtarefa expandida mostra as perguntas do agente. Elas ficam
                // fora da linha, para a corda e a reordenação continuarem medindo só as linhas.
                DirecionamentoPanel painel = new DirecionamentoPanel(s.id(), s.direcionamento(), RECUO_PERGUNTAS,
                        respostas -> aoResponder.accept(s, respostas));
                painel.setVisible(!recolhidas.contains(s.id()));
                perguntas.put(s.id(), painel);
                add(painel);
            }
        }
    }

    /** Seta que expande ou recolhe as perguntas da subtarefa. */
    private BotaoIcone seta(Subtarefa s, Set<String> recolhidas) {
        BotaoIcone seta = new BotaoIcone(iconeSeta(!recolhidas.contains(s.id())), "Mostrar ou esconder as perguntas",
                24);
        seta.addActionListener(e -> {
            DirecionamentoPanel painel = perguntas.get(s.id());
            boolean aberta = !painel.isVisible();
            painel.setVisible(aberta);
            if (aberta) {
                recolhidas.remove(s.id());
            } else {
                recolhidas.add(s.id());
            }
            seta.setIcon(iconeSeta(aberta));
            revalidate();
            repaint();
        });
        return seta;
    }

    private static Icon iconeSeta(boolean aberta) {
        return Ui.icone(aberta ? "seta-baixo" : "seta-direita", 14, Tema.TEXTO_SECUNDARIO);
    }

    private boolean temDependentes(Subtarefa subtarefa) {
        return subtarefas.stream().anyMatch(s -> s.dependeDe().contains(subtarefa.id()));
    }

    private void arrastavel(No no, int indice) {
        MouseAdapter arrasto = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                origemArrasto = indice;
                ponteiro = SwingUtilities.convertPoint(no, e.getPoint(), SubtarefasPanel.this);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                ponteiro = SwingUtilities.convertPoint(no, e.getPoint(), SubtarefasPanel.this);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int origem = origemArrasto;
                int alvo = linhaEm(SwingUtilities.convertPoint(no, e.getPoint(), SubtarefasPanel.this));
                origemArrasto = -1;
                ponteiro = null;
                repaint();
                if (origem >= 0 && alvo >= 0 && alvo != origem) {
                    aoAmarrar.accept(subtarefas.get(origem), subtarefas.get(alvo));
                }
            }
        };
        no.addMouseListener(arrasto);
        no.addMouseMotionListener(arrasto);
    }

    /** Índice da subtarefa cuja linha está sob o ponto, ou -1. */
    private int linhaEm(Point p) {
        for (int i = 0; i < linhas.size(); i++) {
            JPanel linha = linhas.get(i);
            if (p.y >= linha.getY() && p.y < linha.getY() + linha.getHeight()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        Graphics2D g2 = (Graphics2D) g.create();
        for (int i = 0; i < subtarefas.size(); i++) {
            for (String dependencia : subtarefas.get(i).dependeDe()) {
                Integer origem = indices.get(dependencia);
                if (origem == null) {
                    continue;
                }
                Point a = centro(nos.get(origem));
                Point b = centro(nos.get(i));
                double curva = 12 + Math.abs(b.y - a.y) * 0.08;
                Corda.desenhar(g2, new CubicCurve2D.Double(a.x, a.y, a.x - curva, a.y + (b.y - a.y) * 0.35,
                        b.x - curva, a.y + (b.y - a.y) * 0.65, b.x, b.y));
            }
        }
        if (origemArrasto >= 0 && ponteiro != null) {
            Point a = centro(nos.get(origemArrasto));
            Corda.desenhar(g2, new Line2D.Double(a.x, a.y, ponteiro.x, ponteiro.y));
        }
        // As pontas amarradas viram nós da corda.
        for (No no : nos) {
            if (no.amarrado || (origemArrasto >= 0 && no == nos.get(origemArrasto))) {
                Point p = centro(no);
                Corda.no(g2, p.x, p.y, 7);
            }
        }
        g2.dispose();
        reordenacao.desenhar(g);
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
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(amarrado
                    ? "Arraste até outra subtarefa para amarrar; até uma já amarrada, para soltar"
                    : "Arraste até outra subtarefa para amarrar");
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
