package io.github.luanfrombrazil.overtimedispatcher.view.projeto;

import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Console;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Etiqueta;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Tela de um projeto: cabeçalho, Tasks em cartões e o console do projeto.
 * As Tasks rodam na ordem em que aparecem; o botão "Ordenar" permite mudar essa ordem.
 */
public class ProjetoPanel extends JPanel {

    /** Move a Task da posição {@code indice} em {@code deslocamento} posições (-1 sobe, +1 desce). */
    @FunctionalInterface
    public interface AoMoverTask {
        void mover(int indice, int deslocamento);
    }

    private final Projeto projeto;
    private final AoMoverTask aoMover;
    private final JPanel lista = new JPanel();
    private final JButton ordenar = Ui.botaoSecundario("Ordenar");
    private boolean ordenando;

    public ProjetoPanel(Projeto projeto, Runnable aoEditar, Runnable aoNovaTask, AoMoverTask aoMover) {
        super(new BorderLayout(0, 14));
        this.projeto = projeto;
        this.aoMover = aoMover;
        setBackground(Tema.APP);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JButton editar = Ui.botaoSecundario("Editar projeto");
        editar.addActionListener(e -> aoEditar.run());
        JButton novaTask = Ui.botaoPrimario("+ Nova Task");
        novaTask.addActionListener(e -> aoNovaTask.run());
        ordenar.setToolTipText("Mudar a ordem de execução das Tasks");
        ordenar.setEnabled(projeto.tasks().size() > 1);
        ordenar.addActionListener(e -> {
            ordenando = !ordenando;
            ordenar.setText(ordenando ? "Concluir ordem" : "Ordenar");
            atualizarLista();
        });
        String local = projeto.branch() == null ? projeto.pasta() : projeto.pasta() + " · " + projeto.branch();
        add(Ui.coluna(10,
                Ui.cabecalho(Ui.titulo(projeto.nome()),
                        Ui.linha(8, Etiqueta.solida(projeto.provedor(), Tema.BORDA), ordenar, editar, novaTask)),
                Ui.legenda(local)), BorderLayout.NORTH);

        add(rolagem(), BorderLayout.CENTER);
        atualizarLista();

        if (!projeto.console().isEmpty()) {
            add(new Console(List.of("Log", "Histórico", "Pendências (1)"), projeto.console(), 180), BorderLayout.SOUTH);
        }
    }

    private JScrollPane rolagem() {
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setOpaque(false);
        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.add(lista, BorderLayout.NORTH);

        JScrollPane rolagem = new JScrollPane(envoltorio);
        rolagem.setOpaque(false);
        rolagem.getViewport().setOpaque(false);
        rolagem.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.getVerticalScrollBar().setUnitIncrement(16);
        return rolagem;
    }

    private void atualizarLista() {
        List<Task> tasks = projeto.tasks();
        lista.removeAll();
        if (tasks.isEmpty()) {
            lista.add(Ui.cabecalho(Ui.legenda("Nenhuma task."), null));
        }
        if (ordenando) {
            lista.add(Ui.cabecalho(Ui.legenda("As Tasks rodam de cima para baixo. Use as setas para mudar a ordem."), null));
            lista.add(Box.createVerticalStrut(10));
        }
        for (int i = 0; i < tasks.size(); i++) {
            if (i > 0) {
                lista.add(Box.createVerticalStrut(10));
            }
            lista.add(ordenando ? cartaoOrdenavel(tasks, i) : new TaskCard(tasks.get(i), i == 0));
        }
        lista.revalidate();
        lista.repaint();
    }

    private TaskCard cartaoOrdenavel(List<Task> tasks, int indice) {
        Runnable subir = indice > 0 ? () -> mover(indice, -1) : null;
        Runnable descer = indice < tasks.size() - 1 ? () -> mover(indice, 1) : null;
        return TaskCard.ordenavel(tasks.get(indice), indice + 1, subir, descer);
    }

    private void mover(int indice, int deslocamento) {
        aoMover.mover(indice, deslocamento);
        atualizarLista();
    }
}
