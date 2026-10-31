package io.github.luanfrombrazil.overtimedispatcher.view.projeto;

import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.Subtarefa;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Cartao;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Dialogo;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Formulario;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Pop-up de criação e edição de subtarefa: instrução, critério de aceite, verificação, commit e push e dependências.
 */
public class SubtarefaDialog extends Dialogo {

    private static final int DEPENDENCIAS_VISIVEIS = 5;

    private final Subtarefa existente;
    private final JTextField titulo;
    private final JScrollPane instrucao;
    private final JScrollPane criterio;
    private final JCheckBox verificacao;
    private final JCheckBox commit;
    private final JCheckBox push;
    private final Map<String, JCheckBox> dependencias = new LinkedHashMap<>();

    /**
     * @param task      Task da subtarefa; as subtarefas acima desta podem ser escolhidas como dependência
     * @param existente subtarefa a editar, ou null para criar uma nova no fim da Task
     * @param aoDecidir recebe o novo estado quando o usuário aprova a verificação ou manda refazer
     */
    public SubtarefaDialog(JFrame dono, Task task, Subtarefa existente, Consumer<Subtarefa> aoSalvar, Runnable aoExcluir,
                           Consumer<Estado> aoDecidir) {
        super(dono, Tema.BORDA);
        this.existente = existente;
        boolean nova = existente == null;

        JPanel corpo = Formulario.corpo();
        corpo.add(Ui.cabecalho(Ui.titulo(nova ? "Nova subtarefa" : "Editar subtarefa"), null));
        corpo.add(Ui.cabecalho(Ui.legenda(task.titulo()), null));
        corpo.add(Box.createVerticalStrut(14));
        if (!nova && existente.estado() != Estado.PENDENTE) {
            corpo.add(situacao(this, existente.estado(), existente.resultado(), aoDecidir));
            corpo.add(Box.createVerticalStrut(14));
        }

        titulo = Formulario.texto(nova ? "" : existente.titulo());
        corpo.add(Formulario.campo("Título", titulo));
        corpo.add(Box.createVerticalStrut(12));
        instrucao = Formulario.area(nova ? "" : existente.instrucao(), 4);
        corpo.add(Formulario.campo("Instrução", instrucao));
        corpo.add(Box.createVerticalStrut(12));
        criterio = Formulario.area(nova ? "" : existente.criterioDeAceite(), 2);
        corpo.add(Formulario.campo("Critério de aceite", criterio));
        corpo.add(Box.createVerticalStrut(10));
        verificacao = Formulario.caixa("Exige verificação manual", !nova && existente.requerVerificacao());
        corpo.add(Ui.cabecalho(verificacao, null));
        commit = Formulario.caixa("Commit ao terminar", nova || existente.commit());
        push = Formulario.caixa("Push depois do commit", !nova && existente.push());
        // Push só faz sentido depois de um commit.
        push.setEnabled(commit.isSelected());
        commit.addItemListener(e -> {
            push.setEnabled(commit.isSelected());
            if (!commit.isSelected()) {
                push.setSelected(false);
            }
        });
        corpo.add(Ui.cabecalho(Ui.linha(16, commit, push), null));

        List<Subtarefa> anteriores = anteriores(task, existente);
        if (!anteriores.isEmpty()) {
            Formulario.secao(corpo, "Depende de");
            JPanel lista = new JPanel();
            lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
            lista.setOpaque(false);
            for (Subtarefa s : anteriores) {
                JCheckBox caixa = Formulario.caixa(s.titulo(), !nova && existente.dependeDe().contains(s.id()));
                dependencias.put(s.id(), caixa);
                lista.add(Ui.cabecalho(caixa, null));
            }
            if (anteriores.size() > DEPENDENCIAS_VISIVEIS) {
                JScrollPane rolagem = new JScrollPane(lista);
                rolagem.setOpaque(false);
                rolagem.getViewport().setOpaque(false);
                rolagem.setBorder(BorderFactory.createEmptyBorder());
                int altura = DEPENDENCIAS_VISIVEIS * lista.getComponent(0).getPreferredSize().height;
                rolagem.setPreferredSize(new Dimension(100, altura));
                corpo.add(Ui.largo(rolagem));
            } else {
                corpo.add(Ui.largo(lista));
            }
        }

        JPanel raiz = (JPanel) getContentPane();
        raiz.add(corpo, BorderLayout.CENTER);
        raiz.add(Formulario.rodape(this, nova ? "Criar subtarefa" : "Salvar", () -> aoSalvar.accept(subtarefa()),
                nova ? null : aoExcluir, "Excluir a subtarefa?"), BorderLayout.SOUTH);
        raiz.setPreferredSize(new Dimension(560, raiz.getPreferredSize().height));
    }

    /**
     * Como a última execução (da subtarefa ou da Task inteira) terminou, com a resposta do agente e as decisões
     * possíveis. Decidir fecha o {@code dialogo}.
     */
    static JComponent situacao(Dialogo dialogo, Estado estado, String resultado, Consumer<Estado> aoDecidir) {
        String titulo = switch (estado) {
            case ATENCAO -> "Aguardando sua verificação";
            case ERRO -> "Falhou";
            case CONCLUIDO -> "Concluída";
            case EM_ANDAMENTO -> "Em andamento";
            default -> "";
        };
        List<JButton> botoes = new ArrayList<>();
        if (estado == Estado.ATENCAO) {
            botoes.add(decisao(dialogo, Ui.botaoSecundario("Refazer"), Estado.PENDENTE, aoDecidir));
            botoes.add(decisao(dialogo, Ui.botaoPrimario("Aprovar"), Estado.CONCLUIDO, aoDecidir));
        } else if (estado == Estado.ERRO) {
            botoes.add(decisao(dialogo, Ui.botaoPrimario("Tentar de novo"), Estado.PENDENTE, aoDecidir));
        } else if (estado == Estado.CONCLUIDO) {
            botoes.add(decisao(dialogo, Ui.botaoSecundario("Refazer"), Estado.PENDENTE, aoDecidir));
        }

        Cartao cartao = new Cartao(Tema.SUTIL, Tema.cor(estado), new BorderLayout(0, 8));
        cartao.add(Ui.cabecalho(Ui.secao(titulo), Ui.linha(8, botoes.toArray(new JButton[0]))), BorderLayout.NORTH);
        if (resultado != null && !resultado.isBlank()) {
            JTextArea resposta = new JTextArea(resultado.strip(), 5, 20);
            resposta.setEditable(false);
            resposta.setLineWrap(true);
            resposta.setWrapStyleWord(true);
            resposta.setFont(Tema.corpo());
            resposta.setCaretPosition(0);
            JScrollPane rolagem = new JScrollPane(resposta);
            rolagem.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
            cartao.add(rolagem, BorderLayout.CENTER);
        }
        return Ui.largo(cartao);
    }

    private static JButton decisao(Dialogo dialogo, JButton botao, Estado novo, Consumer<Estado> aoDecidir) {
        botao.addActionListener(e -> {
            try {
                aoDecidir.accept(novo);
                dialogo.fechar();
            } catch (IllegalArgumentException erro) {
                JOptionPane.showMessageDialog(dialogo, erro.getMessage(), botao.getText(), JOptionPane.WARNING_MESSAGE);
            }
        });
        return botao;
    }

    /** Subtarefas acima desta na lista (todas, para uma nova). */
    private static List<Subtarefa> anteriores(Task task, Subtarefa existente) {
        List<Subtarefa> todas = task.subtarefas();
        if (existente == null) {
            return todas;
        }
        for (int i = 0; i < todas.size(); i++) {
            if (todas.get(i).id().equals(existente.id())) {
                return todas.subList(0, i);
            }
        }
        return todas;
    }

    private Subtarefa subtarefa() {
        List<String> escolhidas = dependencias.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .toList();
        if (existente == null) {
            return Subtarefa.nova(titulo.getText().strip(), Formulario.textoDa(instrucao), Formulario.textoDa(criterio),
                    verificacao.isSelected(), escolhidas).comGit(commit.isSelected(), push.isSelected());
        }
        return new Subtarefa(existente.id(), titulo.getText().strip(), Formulario.textoDa(instrucao),
                Formulario.textoDa(criterio), verificacao.isSelected(), existente.estado(), escolhidas,
                existente.resultado(), commit.isSelected(), push.isSelected());
    }
}
