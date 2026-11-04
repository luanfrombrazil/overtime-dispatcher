package io.github.luanfrombrazil.overtimer.view.projeto;

import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.view.comum.Console;
import io.github.luanfrombrazil.overtimer.view.comum.Iniciais;
import io.github.luanfrombrazil.overtimer.view.comum.Reordenacao;
import io.github.luanfrombrazil.overtimer.view.comum.SeletorImagem;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tela de um projeto: cabeçalho, as Tasks em cartões e o log do agente. Pode ser redesenhada com
 * {@link #mostrar} sem perder quais Tasks estão abertas nem a posição da rolagem.
 * <p>
 * As Tasks rodam na ordem em que aparecem; a alça de três barras no fim de cada uma muda essa ordem. As Tasks
 * finalizadas (todas as subtarefas concluídas) descem para a seção "Finalizadas", travadas e sem alça.
 */
public class ProjetoPanel extends JPanel {

    /** Ações disparadas pela tela do projeto. */
    public interface Ouvinte {
        void editarProjeto();

        /** Troca o nome do projeto, editado direto no título. */
        void renomearProjeto(String nome);

        /** Troca a imagem do projeto pela do caminho escolhido. */
        void trocarImagemProjeto(String imagem);

        void novaTask();

        void editarTask(Task task);

        /** Move a Task {@code deslocamento} posições na ordem de execução (negativo sobe). */
        void moverTask(Task task, int deslocamento);

        /** Move a subtarefa {@code deslocamento} posições dentro da Task (negativo sobe). */
        void moverSubtarefa(Task task, Subtarefa subtarefa, int deslocamento);

        void novaSubtarefa(Task task);

        /** Se a Task fica entre as abertas mesmo finalizada, porque uma subtarefa nova está sendo escrita nela. */
        boolean seguraAberta(Task task);

        void editarSubtarefa(Task task, Subtarefa subtarefa);

        /** Amarra (o destino passa a depender da origem) ou desamarra duas subtarefas da mesma Task. */
        void amarrar(Task task, Subtarefa origem, Subtarefa destino);

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
    private final Set<String> finalizadas = new HashSet<>();
    private String projetoId;
    private JScrollPane rolagem;
    private Reordenacao reordenacao;

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
            finalizadas.clear();
            projeto.tasks().stream().filter(t -> !finalizada(t)).findFirst().ifPresent(t -> expandidas.add(t.id()));
            projeto.tasks().forEach(t -> conhecidas.add(t.id()));
        }
        for (Task t : projeto.tasks()) {
            if (conhecidas.add(t.id())) {
                expandidas.add(t.id());
            }
            // A Task que acabou de ser finalizada desce fechada para a seção das finalizadas.
            if (!finalizada(t)) {
                finalizadas.remove(t.id());
            } else if (finalizadas.add(t.id())) {
                expandidas.remove(t.id());
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
        JComponent titulo = new TituloEditavel(projeto.nome(), ouvinte::renomearProjeto);
        if (projeto.imagem() != null) {
            titulo = Ui.linha(8, imagem(projeto), titulo);
        }
        return Ui.coluna(10,
                Ui.cabecalho(titulo,
                        Ui.linha(8, editar, novaTask)),
                Ui.legenda(local));
    }

    /** Imagem do projeto ao lado do título; clicar escolhe outra. */
    private JComponent imagem(Projeto projeto) {
        Iniciais icone = new Iniciais(projeto.sigla(), projeto.imagem(), 32);
        icone.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        icone.setToolTipText("Clique para trocar a imagem");
        icone.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SeletorImagem.escolher(ProjetoPanel.this).ifPresent(ouvinte::trocarImagemProjeto);
            }
        });
        return icone;
    }

    /** As Tasks em andamento ou na fila, que podem ser reordenadas, e abaixo a seção das finalizadas, travadas. */
    private JScrollPane tasks(List<Task> todas) {
        List<Task> abertas = todas.stream().filter(t -> !finalizada(t)).toList();
        List<Task> finalizadas = todas.stream().filter(this::finalizada).toList();
        JPanel lista = new JPanel() {
            @Override
            protected void paintChildren(Graphics g) {
                super.paintChildren(g);
                reordenacao.desenhar(g);
            }
        };
        reordenacao = new Reordenacao(lista, (de, para) -> ouvinte.moverTask(abertas.get(de), para - de));
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setOpaque(false);
        if (abertas.isEmpty()) {
            lista.add(Ui.cabecalho(Ui.legenda(todas.isEmpty() ? "Nenhuma task." : "Nenhuma task pendente."), null));
        }
        for (int i = 0; i < abertas.size(); i++) {
            if (i > 0) {
                lista.add(Box.createVerticalStrut(10));
            }
            lista.add(cartao(abertas.get(i), reordenacao.alca("Arraste para mudar a ordem de execução")));
        }

        JPanel coluna = new JPanel();
        coluna.setLayout(new BoxLayout(coluna, BoxLayout.Y_AXIS));
        coluna.setOpaque(false);
        coluna.add(lista);
        if (!finalizadas.isEmpty()) {
            coluna.add(Box.createVerticalStrut(20));
            coluna.add(Ui.cabecalho(Ui.linha(8, Ui.secao("Finalizadas"),
                    Ui.legenda(String.valueOf(finalizadas.size()))), null));
            for (Task task : finalizadas) {
                coluna.add(Box.createVerticalStrut(10));
                coluna.add(cartao(task, null));
            }
        }
        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.add(coluna, BorderLayout.NORTH);

        JScrollPane rolagem = new JScrollPane(envoltorio);
        rolagem.setOpaque(false);
        rolagem.getViewport().setOpaque(false);
        rolagem.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.getVerticalScrollBar().setUnitIncrement(16);
        return rolagem;
    }

    /** Finalizada e não segura entre as abertas por uma subtarefa nova sendo escrita. */
    private boolean finalizada(Task task) {
        return task.finalizada() && !ouvinte.seguraAberta(task);
    }

    private TaskCard cartao(Task task, JComponent alca) {
        return new TaskCard(task, finalizada(task), expandidas.contains(task.id()), aberta -> {
            if (aberta) {
                expandidas.add(task.id());
            } else {
                expandidas.remove(task.id());
            }
        }, ouvinte, alca);
    }
}
