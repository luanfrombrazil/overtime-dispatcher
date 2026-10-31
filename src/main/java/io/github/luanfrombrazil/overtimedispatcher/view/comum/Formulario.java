package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Peças comuns dos formulários (novo projeto, novo protocolo).
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
        JTextField nome = texto("");
        Iniciais iniciais = new Iniciais("", 36);
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

    public static JComboBox<String> lista(int largura, String... opcoes) {
        JComboBox<String> lista = new JComboBox<>(opcoes);
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
        JButton cancelar = Ui.botaoSecundario("Cancelar");
        JButton ok = Ui.botaoPrimario(confirmar);
        cancelar.addActionListener(e -> dialogo.fechar());
        ok.addActionListener(e -> dialogo.fechar());
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rodape.setOpaque(false);
        rodape.setBorder(BorderFactory.createEmptyBorder(0, 24, 20, 16));
        rodape.add(cancelar);
        rodape.add(ok);
        return rodape;
    }
}
