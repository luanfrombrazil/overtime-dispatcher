package io.github.luanfrombrazil.overtimer.view.projeto;

import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Modelo;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.view.comum.Cartao;
import io.github.luanfrombrazil.overtimer.view.comum.Dialogo;
import io.github.luanfrombrazil.overtimer.view.comum.Formulario;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pop-up de criação e edição de subtarefa: instrução, critério de aceite, esforço e modelo do agente, commit e
 * push. A verificação manual não é mais escolhida aqui: a nova nasce sem ela e a editada mantém a que tinha. As
 * dependências não ficam aqui: são os fios arrastados entre as subtarefas no cartão da Task. A subtarefa finalizada
 * abre travada, só para leitura.
 */
public class SubtarefaDialog extends Dialogo {

    private final Subtarefa existente;
    private final JTextField titulo;
    private final JScrollPane instrucao;
    private final JScrollPane criterio;
    private final JSlider esforco;
    private final JComboBox<Modelo> modelo;
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
        // A subtarefa nova abre direto no formulário, sem título nem o nome da Task.
        if (!nova) {
            corpo.add(Ui.cabecalho(Ui.titulo(rascunho ? "Rascunho de subtarefa"
                    : travada ? "Subtarefa finalizada" : "Editar subtarefa"), null));
            corpo.add(Ui.cabecalho(Ui.legenda(task.titulo()), null));
            corpo.add(Box.createVerticalStrut(14));
        }
        if (!nova && !rascunho && existente.estado() != Estado.PENDENTE) {
            corpo.add(situacao(this, existente.estado(), existente.resultado(), existente.aguardaDirecionamento(),
                    aoDecidir));
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
        corpo.add(Box.createVerticalStrut(12));
        esforco = esforco(nova ? Esforco.MEDIUM : existente.esforcoOu(task.esforco()));
        modelo = Formulario.opcoes(140, Modelo.values());
        modelo.setSelectedItem(nova ? Modelo.PADRAO : existente.modelo());
        corpo.add(Formulario.campo("Esforço · modelo", esforcoEModelo(esforco, modelo)));
        corpo.add(Box.createVerticalStrut(10));
        commit = Formulario.caixa("Commit", nova || existente.commit());
        push = Formulario.caixa("Push", !nova && existente.push());
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
            Formulario.travar(titulo, instrucao, criterio, esforco, modelo, commit, push);
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
                s.estado(), s.dependeDe(), s.resultado(), s.commit(), s.push(), s.esforco(), s.modelo(),
                s.direcionamento());
    }

    /** Barra de esforço com uma marca por nível, de Baixo a Máximo. */
    private static JSlider esforco(Esforco inicial) {
        Esforco[] niveis = Esforco.values();
        JSlider barra = new JSlider(0, niveis.length - 1, inicial.ordinal());
        barra.setOpaque(false);
        barra.setMajorTickSpacing(1);
        barra.setSnapToTicks(true);
        barra.setPaintTicks(true);
        Hashtable<Integer, JComponent> rotulos = new Hashtable<>();
        for (Esforco nivel : niveis) {
            rotulos.put(nivel.ordinal(), Ui.legenda(nivel.toString()));
        }
        barra.setLabelTable(rotulos);
        barra.setPaintLabels(true);
        return barra;
    }

    /** A barra de esforço ocupa a largura e a lista de modelos fica ao lado, à direita. */
    private static JComponent esforcoEModelo(JSlider esforco, JComboBox<Modelo> modelo) {
        JPanel lado = new JPanel(new BorderLayout());
        lado.setOpaque(false);
        lado.add(modelo, BorderLayout.NORTH);
        JPanel linha = new JPanel(new BorderLayout(16, 0));
        linha.setOpaque(false);
        linha.add(esforco, BorderLayout.CENTER);
        linha.add(lado, BorderLayout.EAST);
        return Ui.largo(linha);
    }

    /**
     * Como a última execução (da subtarefa ou da Task inteira) terminou, com a resposta do agente e as decisões
     * possíveis. Decidir fecha o {@code dialogo}. Quem espera direcionamento não decide aqui: as perguntas ficam no
     * cartão da Task, sob a subtarefa.
     *
     * @param direcionamento se o item parou com perguntas sem resposta, em vez de esperar a verificação
     */
    static JComponent situacao(Dialogo dialogo, Estado estado, String resultado, boolean direcionamento,
                               Consumer<Estado> aoDecidir) {
        String titulo = switch (estado) {
            case ATENCAO -> direcionamento ? "Aguardando seu direcionamento" : "Aguardando sua verificação";
            case ERRO -> "Falhou";
            case CONCLUIDO -> "Concluída";
            case EM_ANDAMENTO -> "Em andamento";
            default -> "";
        };
        List<JButton> botoes = new ArrayList<>();
        if (estado == Estado.ATENCAO && !direcionamento) {
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
                    false, List.of()).comGit(commit.isSelected(), push.isSelected())
                    .comModelo(esforcoEscolhido(), (Modelo) modelo.getSelectedItem());
        }
        return new Subtarefa(existente.id(), titulo.getText().strip(), Formulario.textoDa(instrucao),
                Formulario.textoDa(criterio), existente.requerVerificacao(), existente.estado(), existente.dependeDe(),
                existente.resultado(), commit.isSelected(), push.isSelected(), esforcoEscolhido(),
                (Modelo) modelo.getSelectedItem(), existente.direcionamento());
    }

    private Esforco esforcoEscolhido() {
        return Esforco.values()[esforco.getValue()];
    }
}
