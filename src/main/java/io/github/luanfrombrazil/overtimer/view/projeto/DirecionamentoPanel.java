package io.github.luanfrombrazil.overtimer.view.projeto;

import io.github.luanfrombrazil.overtimer.model.Direcionamento;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.view.comum.Formulario;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.View;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Perguntas que o agente deixou ao parar, sob a subtarefa expandida ou no cartão da Task executada inteira. Cada
 * pergunta tem um botão de rádio por opção sugerida e, por último, "Outro", com um campo para uma resposta
 * própria. "Responder" só libera quando todas as perguntas têm resposta.
 * <p>
 * O que você já escolheu fica guardado enquanto a app está aberta: a tela é refeita quando o agendador muda o
 * projeto, e a escolha não pode se perder no meio.
 */
class DirecionamentoPanel extends JPanel {

    private static final int LARGURA_TRACO = 3;
    private static final int LIMITE_RESUMO = 400;

    /** Escolha em andamento de uma pergunta: a opção marcada (-1 nenhuma, {@code opcoes.size()} é "Outro"). */
    private record Escolha(int opcao, String outro) {
    }

    private static final Map<String, Escolha> ESCOLHAS = new ConcurrentHashMap<>();

    private final List<Pergunta> perguntas = new ArrayList<>();
    private final JButton responder = Ui.botaoPrimario("Responder");

    /**
     * @param chave       identifica o item (id da subtarefa ou da Task), para guardar a escolha em andamento
     * @param recuo       espaço à esquerda, para alinhar as perguntas sob o item que perguntou
     * @param aoResponder recebe as respostas, uma por pergunta e na ordem delas
     */
    DirecionamentoPanel(String chave, Direcionamento direcionamento, int recuo, Consumer<List<String>> aoResponder) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, recuo, 8, 0));
        // Sem isso, o alinhamento à esquerda dos filhos vaza para a lista de subtarefas e empurra o painel.
        setAlignmentX(CENTER_ALIGNMENT);

        String resumo = direcionamento.resumo();
        if (resumo != null && !resumo.isBlank()) {
            String curto = resumo.strip();
            TextoQuebrado texto = new TextoQuebrado(curto.length() > LIMITE_RESUMO
                    ? curto.substring(0, LIMITE_RESUMO - 1) + "…" : curto, Tema.legenda(), Tema.TEXTO_SECUNDARIO);
            if (curto.length() > LIMITE_RESUMO) {
                texto.setToolTipText("<html><div style='width:420px'>" + html(curto) + "</div></html>");
            }
            add(alinhado(texto));
            add(Box.createVerticalStrut(8));
        }
        List<Direcionamento.Pergunta> lista = direcionamento.perguntas();
        for (int i = 0; i < lista.size(); i++) {
            Pergunta pergunta = new Pergunta(chave + "#" + i, lista.get(i));
            perguntas.add(pergunta);
            add(alinhado(pergunta));
            add(Box.createVerticalStrut(8));
        }

        responder.addActionListener(e -> {
            try {
                aoResponder.accept(perguntas.stream().map(Pergunta::resposta).toList());
                perguntas.forEach(p -> ESCOLHAS.remove(p.chave));
            } catch (IllegalArgumentException erro) {
                JOptionPane.showMessageDialog(this, erro.getMessage(), "Responder", JOptionPane.WARNING_MESSAGE);
            }
        });
        JPanel acoes = new JPanel(new BorderLayout());
        acoes.setOpaque(false);
        acoes.add(responder, BorderLayout.EAST);
        add(alinhado(acoes));
        atualizar();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /** Libera "Responder" quando todas as perguntas têm resposta. */
    private void atualizar() {
        responder.setEnabled(perguntas.stream().allMatch(p -> !p.resposta().isBlank()));
    }

    private static <T extends JComponent> T alinhado(T componente) {
        componente.setAlignmentX(LEFT_ALIGNMENT);
        return componente;
    }

    private static String html(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    /** Uma pergunta, com o traço amarelo à esquerda, as opções e o "Outro". */
    private class Pergunta extends JPanel {

        private final String chave;
        private final List<String> opcoes;
        private final List<JRadioButton> radios = new ArrayList<>();
        private final JRadioButton outro = radio();
        private final JTextField textoOutro = Formulario.texto("");

        Pergunta(String chave, Direcionamento.Pergunta pergunta) {
            this.chave = chave;
            this.opcoes = pergunta.opcoes();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, LARGURA_TRACO + 12, 10, 12));

            add(alinhado(new TextoQuebrado(pergunta.texto(), Tema.corpo(), Tema.TEXTO)));
            ButtonGroup grupo = new ButtonGroup();
            for (String opcao : opcoes) {
                JRadioButton radio = radio();
                grupo.add(radio);
                radios.add(radio);
                TextoQuebrado rotulo = new TextoQuebrado(opcao, Tema.corpo(), Tema.TEXTO);
                rotulo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                rotulo.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        radio.setSelected(true);
                    }
                });
                add(Box.createVerticalStrut(6));
                add(alinhado(linha(radio, rotulo)));
            }
            grupo.add(outro);
            textoOutro.putClientProperty("JTextField.placeholderText", "Escreva outra resposta…");
            JPanel campo = new JPanel(new BorderLayout(8, 0));
            campo.setOpaque(false);
            JTextArea rotuloOutro = new TextoQuebrado("Outro", Tema.corpo(), Tema.TEXTO);
            rotuloOutro.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            rotuloOutro.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    outro.setSelected(true);
                    textoOutro.requestFocusInWindow();
                }
            });
            JPanel caixaRotulo = new JPanel(new BorderLayout());
            caixaRotulo.setOpaque(false);
            caixaRotulo.add(rotuloOutro, BorderLayout.CENTER);
            caixaRotulo.setPreferredSize(new Dimension(44, textoOutro.getPreferredSize().height));
            campo.add(caixaRotulo, BorderLayout.WEST);
            campo.add(textoOutro, BorderLayout.CENTER);
            add(Box.createVerticalStrut(6));
            add(alinhado(linha(outro, campo)));

            restaurar();
            // Escrever no campo escolhe o "Outro".
            textoOutro.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    outro.setSelected(true);
                }
            });
            textoOutro.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    escreveu();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    escreveu();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    escreveu();
                }
            });
            radios.forEach(r -> r.addItemListener(e -> mudou()));
            outro.addItemListener(e -> mudou());
        }

        /** A resposta escolhida: a opção marcada ou o texto do "Outro"; vazia se ainda não respondida. */
        String resposta() {
            for (int i = 0; i < radios.size(); i++) {
                if (radios.get(i).isSelected()) {
                    return opcoes.get(i);
                }
            }
            return outro.isSelected() ? textoOutro.getText().strip() : "";
        }

        /** Escrever no campo escolhe o "Outro"; escolher outra opção depois não apaga o que foi escrito. */
        private void escreveu() {
            if (!textoOutro.getText().isBlank() && !outro.isSelected()) {
                outro.setSelected(true);
            }
            mudou();
        }

        private void mudou() {
            int marcada = outro.isSelected() ? opcoes.size() : radios.indexOf(radios.stream()
                    .filter(JRadioButton::isSelected).findFirst().orElse(null));
            ESCOLHAS.put(chave, new Escolha(marcada, textoOutro.getText()));
            atualizar();
        }

        private void restaurar() {
            Escolha escolha = ESCOLHAS.get(chave);
            if (escolha == null) {
                return;
            }
            textoOutro.setText(escolha.outro());
            if (escolha.opcao() >= 0 && escolha.opcao() < radios.size()) {
                radios.get(escolha.opcao()).setSelected(true);
            } else if (escolha.opcao() == opcoes.size()) {
                outro.setSelected(true);
            }
        }

        private JRadioButton radio() {
            JRadioButton radio = new JRadioButton();
            radio.setOpaque(false);
            radio.setFocusPainted(false);
            radio.setBorder(BorderFactory.createEmptyBorder());
            return radio;
        }

        private JPanel linha(JRadioButton radio, JComponent rotulo) {
            JPanel linha = new JPanel(new BorderLayout(8, 0));
            linha.setOpaque(false);
            JPanel envoltorio = new JPanel(new BorderLayout());
            envoltorio.setOpaque(false);
            envoltorio.add(radio, BorderLayout.NORTH);
            linha.add(envoltorio, BorderLayout.WEST);
            linha.add(rotulo, BorderLayout.CENTER);
            return linha;
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Tema.SUTIL);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Tema.cor(Estado.ATENCAO));
            g.fillRect(0, 0, LARGURA_TRACO, getHeight());
        }
    }

    /**
     * Texto que quebra linhas na largura que recebe. A altura preferida acompanha a largura atual, e a largura
     * preferida é pequena, para o texto nunca alargar o cartão.
     */
    private static class TextoQuebrado extends JTextArea {

        private int larguraAnterior = -1;

        TextoQuebrado(String texto, Font fonte, Color cor) {
            super(texto);
            setFont(fonte);
            setForeground(cor);
            setOpaque(false);
            setEditable(false);
            setFocusable(false);
            setLineWrap(true);
            setWrapStyleWord(true);
            setBorder(BorderFactory.createEmptyBorder());
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (getWidth() != larguraAnterior) {
                        larguraAnterior = getWidth();
                        revalidate();
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            int largura = getWidth() > 0 ? getWidth() : 600;
            Insets margens = getInsets();
            View raiz = getUI().getRootView(this);
            raiz.setSize(Math.max(1, largura - margens.left - margens.right), Integer.MAX_VALUE);
            int altura = (int) Math.ceil(raiz.getPreferredSpan(View.Y_AXIS)) + margens.top + margens.bottom;
            return new Dimension(40, altura);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }
}
