package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Peças comuns dos formulários (projeto, task, subtarefa, protocolo).
 */
public final class Formulario {

    private Formulario() {
    }

    /** Painel vertical com o espaçamento dos diálogos. */
    public static JPanel corpo() {
        JPanel corpo = new JPanel();
        corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
        corpo.setOpaque(false);
        corpo.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        return corpo;
    }

    public static void secao(JPanel corpo, String titulo) {
        corpo.add(Box.createVerticalStrut(18));
        corpo.add(Ui.cabecalho(Ui.secao(titulo), null));
        corpo.add(Box.createVerticalStrut(8));
    }

    public static JComponent campo(String rotulo, JComponent entrada) {
        // O BorderLayout isola o alinhamento interno da coluna, para não deslocar o campo no BoxLayout de fora.
        JPanel campo = new JPanel(new java.awt.BorderLayout());
        campo.setOpaque(false);
        campo.add(Ui.coluna(4, Ui.legenda(rotulo), entrada), java.awt.BorderLayout.CENTER);
        return Ui.largo(campo);
    }

    /** Campo "Nome" com o ícone de iniciais ao lado, atualizado conforme o nome é digitado. */
    public static JComponent identidade() {
        return identidade(texto(""));
    }

    public static JComponent identidade(JTextField nome) {
        return identidade(nome, new Iniciais(Iniciais.de(nome.getText()), 36), null);
    }

    /**
     * Ícone e nome lado a lado; as iniciais acompanham o que se digita.
     *
     * @param aoLado componente opcional à direita do nome (ex.: botões da imagem)
     */
    public static JComponent identidade(JTextField nome, Iniciais iniciais, JComponent aoLado) {
        nome.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                iniciais.setSigla(Iniciais.de(nome.getText()));
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                iniciais.setSigla(Iniciais.de(nome.getText()));
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
            }
        });
        JPanel envoltorioIcone = new JPanel(new java.awt.BorderLayout());
        envoltorioIcone.setOpaque(false);
        envoltorioIcone.add(iniciais, java.awt.BorderLayout.SOUTH);
        JPanel identidade = new JPanel(new java.awt.BorderLayout(12, 0));
        identidade.setOpaque(false);
        identidade.add(envoltorioIcone, java.awt.BorderLayout.WEST);
        identidade.add(campo("Nome", nome), java.awt.BorderLayout.CENTER);
        if (aoLado != null) {
            JPanel envoltorioLado = new JPanel(new java.awt.BorderLayout());
            envoltorioLado.setOpaque(false);
            envoltorioLado.add(aoLado, java.awt.BorderLayout.SOUTH);
            identidade.add(envoltorioLado, java.awt.BorderLayout.EAST);
        }
        return Ui.largo(identidade);
    }

    public static JTextField texto(String valor) {
        JTextField campo = new JTextField(valor);
        campo.setFont(Tema.corpo());
        return Ui.largo(campo);
    }

    public static JScrollPane area(String valor, int linhas) {
        JTextArea area = new JTextArea(valor, linhas, 20);
        area.setFont(Tema.corpo());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane rolagem = new JScrollPane(area);
        rolagem.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
        return Ui.largo(rolagem);
    }

    /** Texto digitado numa área criada por {@link #area}. */
    public static String textoDa(JScrollPane area) {
        return ((JTextArea) area.getViewport().getView()).getText().strip();
    }

    public static JComboBox<String> lista(int largura, String... opcoes) {
        return opcoes(largura, opcoes);
    }

    public static <T> JComboBox<T> opcoes(int largura, T[] opcoes) {
        JComboBox<T> lista = new JComboBox<>(opcoes);
        lista.setFont(Tema.corpo());
        lista.setPreferredSize(new Dimension(largura, 30));
        lista.setMaximumSize(lista.getPreferredSize());
        return lista;
    }

    public static JCheckBox caixa(String texto, boolean marcada) {
        JCheckBox caixa = new JCheckBox(texto, marcada);
        caixa.setFont(Tema.corpo());
        caixa.setOpaque(false);
        return caixa;
    }

    /** Rodapé com Cancelar e o botão de confirmação, alinhados à direita. */
    public static JPanel rodape(Dialogo dialogo, String confirmar) {
        return botoes(dialogo, confirmar, dialogo::fechar);
    }

    /**
     * Rodapé que executa {@code aoConfirmar} e só fecha se der certo: uma {@link IllegalArgumentException}
     * vira um aviso e o diálogo continua aberto. Com {@code aoExcluir}, mostra "Excluir" à esquerda,
     * que pede confirmação antes de agir.
     */
    public static JPanel rodape(Dialogo dialogo, String confirmar, Runnable aoConfirmar,
                                Runnable aoExcluir, String confirmarExclusao) {
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.add(botoes(dialogo, confirmar, () -> {
            try {
                aoConfirmar.run();
                dialogo.fechar();
            } catch (IllegalArgumentException erro) {
                JOptionPane.showMessageDialog(dialogo, erro.getMessage(), confirmar, JOptionPane.WARNING_MESSAGE);
            }
        }), BorderLayout.EAST);
        if (aoExcluir != null) {
            rodape.add(excluir(dialogo, aoExcluir, confirmarExclusao), BorderLayout.WEST);
        }
        return rodape;
    }

    /**
     * Rodapé de um item travado, que só pode ser visto: "Fechar" à direita e, com {@code aoExcluir}, "Excluir"
     * à esquerda, como em {@link #rodape(Dialogo, String, Runnable, Runnable, String)}.
     */
    /**
     * Guarda um rascunho depois que o pop-up já fechou: uma {@link IllegalArgumentException} vira um aviso sobre
     * {@code dono}.
     */
    public static void rascunhar(Component dono, Runnable aoGuardar) {
        try {
            aoGuardar.run();
        } catch (IllegalArgumentException erro) {
            JOptionPane.showMessageDialog(dono, erro.getMessage(), "Rascunho não guardado", JOptionPane.WARNING_MESSAGE);
        }
    }

    public static JPanel rodapeSomenteLeitura(Dialogo dialogo, Runnable aoExcluir, String confirmarExclusao) {
        JButton fechar = Ui.botaoSecundario("Fechar");
        fechar.addActionListener(e -> dialogo.fechar());
        dialogo.getRootPane().setDefaultButton(fechar);
        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        direita.setOpaque(false);
        direita.setBorder(BorderFactory.createEmptyBorder(0, 24, 20, 16));
        direita.add(fechar);
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.add(direita, BorderLayout.EAST);
        if (aoExcluir != null) {
            rodape.add(excluir(dialogo, aoExcluir, confirmarExclusao), BorderLayout.WEST);
        }
        return rodape;
    }

    /** Desabilita os campos, que aparecem acinzentados e continuam legíveis; as áreas de texto ainda rolam. */
    public static void travar(JComponent... campos) {
        for (JComponent campo : campos) {
            if (campo instanceof JScrollPane rolagem && rolagem.getViewport().getView() instanceof JTextArea area) {
                area.setEditable(false);
                area.setEnabled(false);
            } else {
                campo.setEnabled(false);
            }
        }
    }

    private static JPanel excluir(Dialogo dialogo, Runnable aoExcluir, String confirmarExclusao) {
        JButton excluir = Ui.botaoPerigo("Excluir");
        excluir.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(dialogo, confirmarExclusao, "Excluir",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
                aoExcluir.run();
                dialogo.fechar();
            }
        });
        JPanel esquerda = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        esquerda.setOpaque(false);
        esquerda.setBorder(BorderFactory.createEmptyBorder(0, 24, 20, 0));
        esquerda.add(excluir);
        return esquerda;
    }

    private static JPanel botoes(Dialogo dialogo, String confirmar, Runnable aoConfirmar) {
        JButton cancelar = Ui.botaoSecundario("Cancelar");
        JButton ok = Ui.botaoPrimario(confirmar);
        cancelar.addActionListener(e -> dialogo.fechar());
        ok.addActionListener(e -> aoConfirmar.run());
        dialogo.getRootPane().setDefaultButton(ok);
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rodape.setOpaque(false);
        rodape.setBorder(BorderFactory.createEmptyBorder(0, 24, 20, 16));
        rodape.add(cancelar);
        rodape.add(ok);
        return rodape;
    }
}
