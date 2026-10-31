package io.github.luanfrombrazil.overtimedispatcher.view.trilha;

import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.TarefaPessoal;
import io.github.luanfrombrazil.overtimedispatcher.model.Trilha;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Cartao;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.ItemStatus;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Tela de uma trilha: tarefas suas, com o tempo planejado em blocos.
 */
public class TrilhaPanel extends JPanel {

    public TrilhaPanel(Trilha trilha) {
        super(new BorderLayout(0, 12));
        setBackground(Tema.APP);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(Ui.coluna(10,
                Ui.cabecalho(Ui.titulo(trilha.nome()),
                        Ui.linha(8, Ui.botaoSecundario("Abrir no Obsidian"), Ui.botaoPrimario("+ Tarefa"))),
                Ui.legenda(trilha.arquivo())), BorderLayout.NORTH);

        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setOpaque(false);
        for (TarefaPessoal tarefa : trilha.tarefas()) {
            lista.add(tarefa(tarefa));
            lista.add(Box.createVerticalStrut(10));
        }
        if (trilha.tarefas().isEmpty()) {
            lista.add(Ui.cabecalho(Ui.legenda("Nenhuma tarefa."), null));
        } else {
            lista.add(legenda());
        }
        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.add(lista, BorderLayout.NORTH);
        add(envoltorio, BorderLayout.CENTER);
    }

    private static JComponent tarefa(TarefaPessoal tarefa) {
        boolean doOraculo = tarefa.estado() == Estado.ORACULO;
        Cartao cartao = new Cartao(doOraculo ? Tema.ROXO_FUNDO : Tema.PAINEL, doOraculo ? Tema.ROXO : Tema.BORDA,
                new BorderLayout());
        cartao.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        ItemStatus item = new ItemStatus(tarefa.estado(), tarefa.titulo(), tarefa.detalhe(), null);
        if (doOraculo) {
            item.comDireita(new JLabel(Ui.iconeOraculo(false, 20)),
                    Ui.botaoOraculoSecundario("Recusar"), Ui.botaoOraculo("Aceitar"));
        } else if (tarefa.blocosTotal() > 0) {
            JPanel andamento = Ui.coluna(4, new Blocos(tarefa.blocosFeitos(), tarefa.blocosNaAgenda(), tarefa.blocosTotal()),
                    Ui.legenda(tarefa.andamento()));
            for (java.awt.Component c : andamento.getComponents()) {
                ((JComponent) c).setAlignmentX(RIGHT_ALIGNMENT);
            }
            item.comDireita(andamento);
        }
        cartao.add(item, BorderLayout.CENTER);
        return Ui.largo(cartao);
    }

    private static JComponent legenda() {
        return Ui.cabecalho(Ui.linha(14,
                amostra("feito", Tema.VERDE, true),
                amostra("na agenda", Tema.ACENTO, false),
                amostra("a agendar", Tema.ATIVIDADE[0], true)), null);
    }

    private static JComponent amostra(String texto, Color cor, boolean cheio) {
        JComponent bloco = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cor);
                if (cheio) {
                    g2.fillRoundRect(0, 1, 14, 7, 4, 4);
                } else {
                    g2.drawRoundRect(0, 1, 13, 6, 4, 4);
                }
                g2.dispose();
            }
        };
        java.awt.Dimension d = new java.awt.Dimension(14, 9);
        bloco.setPreferredSize(d);
        bloco.setMaximumSize(d);
        return Ui.linha(5, bloco, Ui.legenda(texto));
    }
}
