package io.github.luanfrombrazil.overtimer.view.projeto;

import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.view.comum.BotaoIcone;
import io.github.luanfrombrazil.overtimer.view.comum.Cartao;
import io.github.luanfrombrazil.overtimer.view.comum.ItemStatus;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Cartão de uma Task. Expandido, mostra as subtarefas e as ações. Tasks do Oráculo aparecem em roxo. A Task
 * finalizada fica travada: a alça vira o botão verde da conclusão, sem nova subtarefa e com os detalhes só para
 * leitura.
 */
class TaskCard extends Cartao {

    private final Task task;
    private final boolean finalizada;
    private final JPanel corpo;
    private final JLabel seta = new JLabel();
    private final Consumer<Boolean> aoExpandir;
    private boolean expandido;

    /**
     * @param finalizada se o cartão aparece travado, entre as finalizadas; uma Task finalizada pode continuar
     *                   aberta enquanto uma subtarefa nova é escrita nela
     * @param aoExpandir avisado quando o usuário abre (true) ou fecha (false) o cartão
     * @param alca       alça de três barras, no fim do cabeçalho, que arrasta o cartão para mudar a ordem; null
     *                   na Task finalizada, que não muda de ordem
     * @param recolhidas subtarefas com perguntas que você recolheu, alterado pela seta de cada uma
     */
    TaskCard(Task task, boolean finalizada, boolean expandido, Consumer<Boolean> aoExpandir,
             ProjetoPanel.Ouvinte ouvinte, JComponent alca, Set<String> recolhidas) {
        super(task.estado() == Estado.ORACULO ? Tema.ROXO_FUNDO : Tema.PAINEL,
                task.estado() == Estado.ORACULO ? Tema.ROXO : Tema.BORDA, null);
        this.task = task;
        this.finalizada = finalizada;
        this.aoExpandir = aoExpandir;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        ItemStatus cabecalho = new ItemStatus(task.estado(), task.titulo(), task.detalhe(), null);
        corpo = new JPanel();
        corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
        corpo.setOpaque(false);
        if (task.estado() == Estado.ORACULO) {
            JButton recusar = Ui.botaoOraculoSecundario("Recusar");
            JButton aceitar = Ui.botaoOraculo("Aceitar");
            cabecalho.comDireita(new JLabel(Ui.iconeOraculo(false, 20)), recusar, aceitar, alca);
            expandido = false;
        } else {
            long falhas = task.inteira()
                    ? (task.execucao() == Estado.ERRO ? 1 : 0)
                    : task.subtarefas().stream().filter(s -> s.estado() == Estado.ERRO).count();
            if (falhas > 0) {
                cabecalho.comDireita(botaoTentarDeNovo(task.inteira() ? "Tentar de novo a task"
                        : falhas == 1 ? "Tentar de novo a subtarefa que falhou"
                        : "Tentar de novo as " + falhas + " subtarefas que falharam", () -> ouvinte.tentarDeNovo(task)),
                        seta, alca);
            } else if (finalizada) {
                // No lugar da alça, que não move mais a Task, a mesma barra em verde mostra a conclusão.
                cabecalho.comDireita(seta, botaoConclusao(task.titulo(), task.conclusao()));
            } else if (alca == null) {
                cabecalho.comDireita(seta);
            } else {
                cabecalho.comDireita(seta, alca);
            }
            // Sem subtarefas não há o que recolher: o cartão fica sempre aberto, só com as ações, e sem a seta.
            if (!task.inteira()) {
                cabecalho.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                cabecalho.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        setExpandido(!TaskCard.this.expandido);
                        TaskCard.this.aoExpandir.accept(TaskCard.this.expandido);
                    }
                });
                corpo.add(new SubtarefasPanel(task.subtarefas(),
                        (uma, outra) -> ouvinte.amarrar(task, uma, outra),
                        (s, deslocamento) -> ouvinte.moverSubtarefa(task, s, deslocamento),
                        s -> ouvinte.editarSubtarefa(task, s),
                        s -> ouvinte.tentarDeNovo(task, s),
                        recolhidas,
                        (s, respostas) -> ouvinte.responder(task, s, respostas)));
            } else if (task.aguardaDirecionamento()) {
                // Sem subtarefas, as perguntas do agente ficam direto no cartão, que está sempre aberto.
                JPanel perguntas = new JPanel(new BorderLayout());
                perguntas.setOpaque(false);
                perguntas.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                perguntas.add(new DirecionamentoPanel(task.id(), task.direcionamento(), ItemStatus.LARGURA_BORDA + 12,
                        respostas -> ouvinte.responder(task, null, respostas)), BorderLayout.CENTER);
                corpo.add(Ui.largo(perguntas));
            }
            corpo.add(acoes(ouvinte));
        }
        add(cabecalho);
        add(corpo);
        setExpandido(expandido);
    }

    /** Botão pequeno com a seta circular, ao lado do que falhou. */
    static BotaoIcone botaoTentarDeNovo(String dica, Runnable acao) {
        BotaoIcone botao = new BotaoIcone(Ui.icone("tentar-de-novo", 14, Tema.TEXTO), dica, 24);
        botao.addActionListener(e -> acao.run());
        return botao;
    }

    /**
     * Botão de três barras com borda verde, no lugar da alça de um item finalizado: mostra a mensagem de
     * conclusão do agente.
     */
    static BotaoIcone botaoConclusao(String titulo, String mensagem) {
        BotaoIcone botao = new BotaoIcone(Ui.icone("arrastar", 14, Tema.VERDE), "Finalizada; clique para ver a conclusão",
                24);
        botao.setBorda(Tema.VERDE);
        botao.addActionListener(e -> {
            JTextArea texto = new JTextArea(mensagem == null || mensagem.isBlank()
                    ? "O agente não deixou mensagem de conclusão." : mensagem.strip(), 18, 72);
            texto.setEditable(false);
            texto.setLineWrap(true);
            texto.setWrapStyleWord(true);
            texto.setFont(Tema.corpo());
            texto.setCaretPosition(0);
            JScrollPane rolagem = new JScrollPane(texto);
            rolagem.setBorder(BorderFactory.createLineBorder(Tema.VERDE));
            JDialog dialogo = new JOptionPane(rolagem, JOptionPane.PLAIN_MESSAGE)
                    .createDialog(SwingUtilities.getWindowAncestor(botao), "Conclusão: " + titulo);
            // Pode crescer para ler conclusões longas, mas não fica menor que o tamanho inicial.
            dialogo.setResizable(true);
            dialogo.setMinimumSize(dialogo.getSize());
            dialogo.setVisible(true);
            dialogo.dispose();
        });
        return botao;
    }

    private JComponent acoes(ProjetoPanel.Ouvinte ouvinte) {
        JPanel linha = new JPanel(new BorderLayout());
        linha.setOpaque(false);
        linha.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        if (finalizada) {
            // Finalizada: travada, só dá para ver os detalhes.
            JButton ver = Ui.botaoSecundario("Ver detalhes");
            ver.addActionListener(e -> ouvinte.editarTask(task));
            linha.add(ver, BorderLayout.WEST);
            return Ui.largo(linha);
        }
        JButton novaSubtarefa = Ui.botaoSecundario("+ Subtarefa");
        novaSubtarefa.addActionListener(e -> ouvinte.novaSubtarefa(task));
        JButton editar = Ui.botaoSecundario("Editar");
        editar.addActionListener(e -> ouvinte.editarTask(task));
        linha.add(Ui.linha(8, novaSubtarefa, editar), BorderLayout.WEST);
        // O direcionamento se responde nas perguntas sob a subtarefa; este botão é só da verificação manual.
        task.subtarefas().stream().filter(s -> s.estado() == Estado.ATENCAO && !s.aguardaDirecionamento())
                .findFirst().ifPresent(s -> {
            JButton revisar = Ui.botaoSecundario("Revisar verificação");
            revisar.addActionListener(e -> ouvinte.editarSubtarefa(task, s));
            linha.add(revisar, BorderLayout.EAST);
        });
        return Ui.largo(linha);
    }

    void setExpandido(boolean expandido) {
        this.expandido = expandido;
        boolean recolhivel = task.estado() == Estado.ORACULO || !task.inteira();
        corpo.setVisible(expandido || !recolhivel);
        seta.setVisible(recolhivel);
        seta.setIcon(Ui.icone(expandido ? "seta-baixo" : "seta-direita", 14, Tema.TEXTO_SECUNDARIO));
        revalidate();
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
