package io.github.luanfrombrazil.overtimer.view.config;

import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Caixa para editar a regra de uma variável modificável, ou criar uma nova, aberta sobre a janela que a pediu.
 */
final class RegraDialog extends JDialog {

    private final JTextArea regra;
    private final JTextField nome;
    private Variavel salva;

    /**
     * @param variavel  a variável a editar, ou null para criar uma
     * @param nomeValido se o nome digitado pode ser usado; só ao criar
     */
    private RegraDialog(Component origem, Variavel variavel, Predicate<String> nomeValido) {
        super(SwingUtilities.getWindowAncestor(origem), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        boolean criando = variavel == null;
        nome = criando ? new JTextField() : null;

        regra = new JTextArea(criando ? "" : variavel.regra());
        regra.setFont(Tema.mono());
        regra.setForeground(Tema.TEXTO);
        regra.setBackground(Tema.LOG);
        regra.setCaretColor(Tema.TEXTO);
        regra.setLineWrap(true);
        regra.setWrapStyleWord(true);
        regra.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane rolagem = new JScrollPane(regra);
        rolagem.setBorder(BorderFactory.createLineBorder(Tema.ACENTO, 1));
        rolagem.setPreferredSize(new Dimension(520, 220));

        JButton cancelar = Ui.botaoSecundario("Cancelar");
        JButton salvar = Ui.botaoPrimario("Salvar");
        cancelar.addActionListener(e -> dispose());
        salvar.addActionListener(e -> {
            salva = criando
                    ? new Variavel(nome.getText().strip(), "Variável editável criada nas configurações", regra.getText())
                    : variavel.comRegra(regra.getText());
            dispose();
        });
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        botoes.add(cancelar);
        botoes.add(salvar);

        JComponent topo;
        if (criando) {
            nome.setFont(Tema.mono());
            nome.setPreferredSize(new Dimension(260, 30));
            Ui.fixo(nome);
            salvar.setEnabled(false);
            nome.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    salvar.setEnabled(nomeValido.test(nome.getText().strip()));
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    salvar.setEnabled(nomeValido.test(nome.getText().strip()));
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    // Sem mudança de texto.
                }
            });
            topo = Ui.coluna(12, Ui.secao("Nova variável editável"),
                    Ui.coluna(4, Ui.legenda("Nome"), nome), Ui.legenda("Regra"));
        } else {
            topo = Ui.secao(variavel.marcador());
        }

        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(Tema.PAINEL);
        raiz.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDA, 1),
                BorderFactory.createEmptyBorder(16, 16, 14, 16)));
        raiz.add(topo, BorderLayout.NORTH);
        raiz.add(rolagem, BorderLayout.CENTER);
        raiz.add(botoes, BorderLayout.SOUTH);
        setContentPane(raiz);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(origem);
    }

    /** Mostra a caixa e devolve a regra editada, ou vazio se cancelada. */
    static Optional<String> editar(Component origem, Variavel variavel) {
        RegraDialog dialogo = new RegraDialog(origem, variavel, null);
        dialogo.setVisible(true);
        return Optional.ofNullable(dialogo.salva).map(Variavel::regra);
    }

    /** Pede nome e regra de uma variável editável nova; vazio se cancelada. */
    static Optional<Variavel> criar(Component origem, Predicate<String> nomeValido) {
        RegraDialog dialogo = new RegraDialog(origem, null, nomeValido);
        dialogo.setVisible(true);
        return Optional.ofNullable(dialogo.salva);
    }
}
