package io.github.luanfrombrazil.overtimedispatcher.view.projeto;

import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Subtarefa;
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
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tela de um projeto: cabeçalho, as Tasks em cartões e o log do agente. Pode ser redesenhada com
 * {@link #mostrar} sem perder quais Tasks estão abertas nem a posição da rolagem.
 */
public class ProjetoPanel extends JPanel {

    /** Ações disparadas pela tela do projeto. */
    public interface Ouvinte {
        void editarProjeto();

        void novaTask();

        void editarTask(Task task);

        void novaSubtarefa(Task task);

        void editarSubtarefa(Task task, Subtarefa subtarefa);

        /** Amarra ou desamarra duas subtarefas da mesma Task. */
        void amarrar(Task task, Subtarefa uma, Subtarefa outra);

        /** Devolve à fila a subtarefa que falhou. */
        void tentarDeNovo(Task task, Subtarefa subtarefa);

        /** Devolve à fila todas as subtarefas da Task que falharam. */
        void tentarDeNovo(Task task);
    }

    private static final int ALTURA_CONSOLE = 180;

    private final Ouvinte ouvinte;
    private final Set<String> expandidas = new HashSet<>();
    private Console console;
    private final Set<String> conhecidas = new HashSet<>();
    private String projetoId;
    private JScrollPane rolagem;

    public ProjetoPanel(Ouvinte ouvinte) {
        super(new BorderLayout(0, 14));
        this.ouvinte = ouvinte;
        setBackground(Tema.APP);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
    }

    /** Id do projeto mostrado, ou null se nenhum foi mostrado ainda. */
    public String projetoId() {
        return projetoId;
    }

    /**
     * Redesenha a tela com o projeto e o log do agente. Tasks novas aparecem abertas; ao trocar de projeto,
     * só a primeira.
     */
    public void mostrar(Projeto projeto, List<String> log) {
        boolean outro = !projeto.id().equals(projetoId);
        int posicao = outro || rolagem == null ? 0 : rolagem.getVerticalScrollBar().getValue();
        if (outro) {
            projetoId = projeto.id();
            expandidas.clear();
            conhecidas.clear();
            if (!projeto.tasks().isEmpty()) {
                expandidas.add(projeto.tasks().getFirst().id());
            }
            projeto.tasks().forEach(t -> conhecidas.add(t.id()));
        }
        for (Task t : projeto.tasks()) {
            if (conhecidas.add(t.id())) {
                expandidas.add(t.id());
            }
        }

        removeAll();
        add(cabecalho(projeto), BorderLayout.NORTH);
        rolagem = tasks(projeto.tasks());
        add(rolagem, BorderLayout.CENTER);
        console = null;
        log.forEach(this::adicionarLog);
        revalidate();
        repaint();
        JScrollPane atual = rolagem;
        SwingUtilities.invokeLater(() -> atual.getVerticalScrollBar().setValue(posicao));
    }

    /** Acrescenta uma linha ao console, que aparece na primeira linha recebida. */
    public void adicionarLog(String linha) {
        if (console == null) {
            console = new Console(List.of("Log"), List.of(), ALTURA_CONSOLE);
            add(console, BorderLayout.SOUTH);
            revalidate();
        }
        console.adicionar(linha);
    }

    private JPanel cabecalho(Projeto projeto) {
        JButton editar = Ui.botaoSecundario("Editar projeto");
        editar.addActionListener(e -> ouvinte.editarProjeto());
        JButton novaTask = Ui.botaoPrimario("+ Nova Task");
        novaTask.addActionListener(e -> ouvinte.novaTask());
        String local = projeto.usaGit() ? projeto.pasta() + " · " + projeto.branch() : projeto.pasta();
        return Ui.coluna(10,
                Ui.cabecalho(Ui.titulo(projeto.nome()),
                        Ui.linha(8, Etiqueta.solida(projeto.provedor().toString(), Tema.BORDA), editar, novaTask)),
                Ui.legenda(local));
    }

    private JScrollPane tasks(List<Task> tasks) {
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
            Task task = tasks.get(i);
            lista.add(new TaskCard(task, expandidas.contains(task.id()), aberta -> {
                if (aberta) {
                    expandidas.add(task.id());
                } else {
                    expandidas.remove(task.id());
                }
            }, ouvinte));
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
