package io.github.luanfrombrazil.overtimer.view.projeto;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Título que vira campo de texto ao ser clicado. Enter ou sair do campo grava o nome; Esc desfaz. Nome vazio
 * ou igual ao anterior não grava nada.
 */
class TituloEditavel extends JTextField {

    private final String original;
    private final Consumer<String> aoRenomear;
    private boolean gravado;

    TituloEditavel(String nome, Consumer<String> aoRenomear) {
        super(nome);
        this.original = nome;
        this.aoRenomear = aoRenomear;
        setFont(Tema.titulo());
        setForeground(Tema.TEXTO);
        setCaretColor(Tema.TEXTO);
        setBackground(Tema.PAINEL);
        setToolTipText("Clique para renomear");
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        // Só fica focável depois do clique, para a tela não abrir já editando o título.
        setFocusable(false);
        mostrarEditando(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isFocusable()) {
                    setFocusable(true);
                    requestFocusInWindow();
                    setCaretPosition(viewToModel2D(e.getPoint()));
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                mostrarEditando(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                gravar();
            }
        });
        addActionListener(e -> gravar());
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "desfazer");
        getActionMap().put("desfazer", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setText(original);
                encerrar();
            }
        });
        // O campo acompanha a largura do texto enquanto se digita.
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                revalidate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                revalidate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                revalidate();
            }
        });
    }

    private void gravar() {
        if (gravado) {
            return;
        }
        String nome = getText().strip();
        if (nome.isEmpty() || nome.equals(original)) {
            setText(original);
            encerrar();
            return;
        }
        gravado = true;
        encerrar();
        aoRenomear.accept(nome);
    }

    private void encerrar() {
        mostrarEditando(false);
        setFocusable(false);
    }

    /** Parado, parece um título comum; editando, ganha fundo e borda de campo. */
    private void mostrarEditando(boolean editando) {
        setOpaque(editando);
        setBorder(editando
                ? BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.ACENTO),
                BorderFactory.createEmptyBorder(0, 3, 0, 3))
                : BorderFactory.createEmptyBorder(1, 4, 1, 4));
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension tamanho = super.getPreferredSize();
        tamanho.width += 4;
        return tamanho;
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
