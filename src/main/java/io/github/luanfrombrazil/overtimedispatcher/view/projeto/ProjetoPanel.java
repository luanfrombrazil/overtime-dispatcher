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
 */
public class ProjetoPanel extends JPanel {

    public ProjetoPanel(Projeto projeto, Runnable aoEditar, Runnable aoNovaTask) {
        super(new BorderLayout(0, 14));
        setBackground(Tema.APP);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JButton editar = Ui.botaoSecundario("Editar projeto");
        editar.addActionListener(e -> aoEditar.run());
        JButton novaTask = Ui.botaoPrimario("+ Nova Task");
        novaTask.addActionListener(e -> aoNovaTask.run());
        String local = projeto.branch() == null ? projeto.pasta() : projeto.pasta() + " · " + projeto.branch();
        add(Ui.coluna(10,
                Ui.cabecalho(Ui.titulo(projeto.nome()),
                        Ui.linha(8, Etiqueta.solida(projeto.provedor(), Tema.BORDA), editar, novaTask)),
                Ui.legenda(local)), BorderLayout.NORTH);

        add(tasks(projeto.tasks()), BorderLayout.CENTER);

        if (!projeto.console().isEmpty()) {
            add(new Console(List.of("Log", "Histórico", "Pendências (1)"), projeto.console(), 180), BorderLayout.SOUTH);
        }
    }

    private static JScrollPane tasks(List<Task> tasks) {
        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setOpaque(false);
        if (tasks.isEmpty()) {
            lista.add(Ui.cabecalho(Ui.legenda("Nenhuma task."), null));
        }
        for (int i = 0; i < tasks.size(); i++) {
            if (i > 0) {
                lista.add(Box.createVerticalStrut(10));
            }
            lista.add(new TaskCard(tasks.get(i), i == 0));
        }
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
}
