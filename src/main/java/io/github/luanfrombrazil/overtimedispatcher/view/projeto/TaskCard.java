package io.github.luanfrombrazil.overtimedispatcher.view.projeto;

import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Cartao;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.ItemStatus;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Cartão de uma Task. Expandido, mostra as subtarefas e as ações. Tasks do Oráculo aparecem em roxo.
 */
class TaskCard extends Cartao {

    private final Task task;
    private final JPanel corpo;
    private final JLabel seta = new JLabel();
    private boolean expandido;

    TaskCard(Task task, boolean expandido) {
        super(task.estado() == Estado.ORACULO ? Tema.ROXO_FUNDO : Tema.PAINEL,
                task.estado() == Estado.ORACULO ? Tema.ROXO : Tema.BORDA, null);
        this.task = task;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        ItemStatus cabecalho = new ItemStatus(task.estado(), task.titulo(), task.detalhe(), null);
        if (task.estado() == Estado.ORACULO) {
            JButton recusar = Ui.botaoOraculoSecundario("Recusar");
            JButton aceitar = Ui.botaoOraculo("Aceitar");
            cabecalho.comDireita(new JLabel(Ui.iconeOraculo(false, 20)), recusar, aceitar);
        } else if (!task.subtarefas().isEmpty()) {
            cabecalho.comDireita(seta);
            cabecalho.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cabecalho.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setExpandido(!TaskCard.this.expandido);
                }
            });
        }
        add(cabecalho);

        corpo = new JPanel();
        corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
        corpo.setOpaque(false);
        if (!task.subtarefas().isEmpty()) {
            corpo.add(new SubtarefasPanel(task.subtarefas()));
            corpo.add(acoes());
        }
        add(corpo);
        setExpandido(expandido);
    }

    private JComponent acoes() {
        JPanel linha = new JPanel(new BorderLayout());
        linha.setOpaque(false);
        linha.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        linha.add(Ui.linha(8, Ui.botaoSecundario("+ Subtarefa"), Ui.botaoSecundario("Editar")), BorderLayout.WEST);
        if (task.estado() == Estado.ATENCAO) {
            linha.add(Ui.botaoSecundario("Revisar verificação"), BorderLayout.EAST);
        }
        return Ui.largo(linha);
    }

    void setExpandido(boolean expandido) {
        this.expandido = expandido;
        corpo.setVisible(expandido);
        seta.setIcon(Ui.icone(expandido ? "seta-baixo" : "seta-direita", 14, Tema.TEXTO_SECUNDARIO));
        revalidate();
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
