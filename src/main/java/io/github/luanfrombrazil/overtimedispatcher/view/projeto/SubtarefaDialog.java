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
import java.util.List;
import java.util.function.Consumer;

/**
 * Pop-up de criação e edição de subtarefa: instrução, critério de aceite, verificação, commit e push. As
 * dependências não ficam aqui: são os fios arrastados entre as subtarefas no cartão da Task. A subtarefa finalizada
 * abre travada, só para leitura.
 */
public class SubtarefaDialog extends Dialogo {

    private final Subtarefa existente;
    private final JTextField titulo;
    private final JScrollPane instrucao;
    private final JScrollPane criterio;
    private final JCheckBox verificacao;
    private final JCheckBox commit;
    private final JCheckBox push;

    /**
     * @param task      Task da subtarefa
     * @param existente subtarefa a editar, ou null para criar uma nova no fim da Task
     * @param aoDecidir  recebe o novo estado quando o usuário aprova a verificação ou manda refazer
     * @param aoRascunho recebe a subtarefa como está quando o usuário clica fora do pop-up de uma subtarefa nova ou
     *                   de um rascunho, que então fecha; null mantém o pop-up aberto
     */
    public SubtarefaDialog(JFrame dono, Task task, Subtarefa existente, Consumer<Subtarefa> aoSalvar, Runnable aoExcluir,
                           Consumer<Estado> aoDecidir, Consumer<Subtarefa> aoRascunho) {
        super(dono, Tema.BORDA);
        this.existente = existente;
        boolean nova = existente == null;
        boolean rascunho = !nova && existente.rascunho();
        boolean travada = !nova && existente.finalizada();

        JPanel corpo = Formulario.corpo();
        corpo.add(Ui.cabecalho(Ui.titulo(nova ? "Nova subtarefa" : rascunho ? "Rascunho de subtarefa"
                : travada ? "Subtarefa finalizada" : "Editar subtarefa"), null));
        corpo.add(Ui.cabecalho(Ui.legenda(task.titulo()), null));
        corpo.add(Box.createVerticalStrut(14));
        if (!nova && !rascunho && existente.estado() != Estado.PENDENTE) {
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

        JPanel raiz = (JPanel) getContentPane();
        raiz.add(corpo, BorderLayout.CENTER);
        if (travada) {
            // A subtarefa finalizada só pode ser vista; "Refazer", na situação, a devolve à fila.
            Formulario.travar(titulo, instrucao, criterio, verificacao, commit, push);
            raiz.add(Formulario.rodapeSomenteLeitura(this, null, null), BorderLayout.SOUTH);
        } else {
            raiz.add(Formulario.rodape(this, nova || rascunho ? "Criar subtarefa" : "Salvar",
                    () -> aoSalvar.accept(subtarefa()), nova ? null : aoExcluir, "Excluir a subtarefa?"),
                    BorderLayout.SOUTH);
        }
        if ((nova || rascunho) && aoRascunho != null) {
            aoClicarFora(() -> {
                Subtarefa atual = nova && vazia() ? null : comTituloDeRascunho(subtarefa());
                fechar();
                if (atual != null) {
                    Formulario.rascunhar(dono, () -> aoRascunho.accept(atual));
                }
            });
        }
        raiz.setPreferredSize(new Dimension(560, raiz.getPreferredSize().height));
    }

    /** Se nada foi escrito; uma subtarefa nova vazia é descartada em vez de virar rascunho. */
    private boolean vazia() {
        return titulo.getText().isBlank() && Formulario.textoDa(instrucao).isBlank()
                && Formulario.textoDa(criterio).isBlank();
    }

    private static Subtarefa comTituloDeRascunho(Subtarefa s) {
        if (!s.titulo().isBlank()) {
            return s;
        }
        return new Subtarefa(s.id(), "Rascunho", s.instrucao(), s.criterioDeAceite(), s.requerVerificacao(),
                s.estado(), s.dependeDe(), s.resultado(), s.commit(), s.push());
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

    /** As dependências não são escolhidas aqui: a nova nasce solta, e a editada mantém os fios que já tinha. */
    private Subtarefa subtarefa() {
        if (existente == null) {
            return Subtarefa.nova(titulo.getText().strip(), Formulario.textoDa(instrucao), Formulario.textoDa(criterio),
                    verificacao.isSelected(), List.of()).comGit(commit.isSelected(), push.isSelected());
        }
        return new Subtarefa(existente.id(), titulo.getText().strip(), Formulario.textoDa(instrucao),
                Formulario.textoDa(criterio), verificacao.isSelected(), existente.estado(), existente.dependeDe(),
                existente.resultado(), commit.isSelected(), push.isSelected());
    }
}
