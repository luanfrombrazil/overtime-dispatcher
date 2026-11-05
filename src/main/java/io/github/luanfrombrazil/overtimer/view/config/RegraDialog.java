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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.Optional;

/**
 * Caixa para editar a regra de uma variável modificável, aberta sobre a janela que a pediu.
 */
final class RegraDialog extends JDialog {

    private final JTextArea regra;
    private String salva;

    private RegraDialog(Component origem, Variavel variavel) {
        super(SwingUtilities.getWindowAncestor(origem), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        regra = new JTextArea(variavel.regra());
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
            salva = regra.getText();
            dispose();
        });
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        botoes.add(cancelar);
        botoes.add(salvar);

        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(Tema.PAINEL);
        raiz.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDA, 1),
                BorderFactory.createEmptyBorder(16, 16, 14, 16)));
        raiz.add(Ui.secao(variavel.marcador()), BorderLayout.NORTH);
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
        RegraDialog dialogo = new RegraDialog(origem, variavel);
        dialogo.setVisible(true);
        return Optional.ofNullable(dialogo.salva);
    }
}
