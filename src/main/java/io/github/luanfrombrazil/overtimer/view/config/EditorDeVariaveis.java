package io.github.luanfrombrazil.overtimer.view.config;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Editor de texto com variáveis ({@code {nome}}): pinta as variáveis, aceita as soltas por arrastar na posição do
 * mouse, sugere as que combinam com o que se digita depois de "{" e, no duplo clique numa variável modificável,
 * pede para editar a regra dela.
 */
class EditorDeVariaveis extends JTextPane {

    private static final Pattern VARIAVEL = Pattern.compile("\\{[\\w.]+}");
    /** "{" seguido do que já foi digitado do nome, colado no cursor. */
    private static final Pattern INICIO_DE_VARIAVEL = Pattern.compile("\\{([\\w.]*)$");

    private final List<Variavel> variaveis;
    private final Consumer<Variavel> editarRegra;

    private final JPopupMenu sugestoes = new JPopupMenu();
    private final JList<Variavel> lista = new JList<>();
    private int inicioDaSugestao = -1;

    /**
     * @param variaveis   as que podem entrar neste texto
     * @param editarRegra chamado no duplo clique numa variável modificável
     */
    EditorDeVariaveis(List<Variavel> variaveis, Consumer<Variavel> editarRegra) {
        this.variaveis = variaveis;
        this.editarRegra = editarRegra;
        setFont(Tema.mono());
        setForeground(Tema.TEXTO);
        setBackground(Tema.LOG);
        setCaretColor(Tema.TEXTO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // O TransferHandler padrão do texto já insere o que é solto na posição do mouse.
        setDragEnabled(true);

        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    colorir();
                    sugerir();
                });
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    colorir();
                    sugerir();
                });
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Mudança só de estilo (a própria coloração): nada a fazer.
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    variavelEm(viewToModel2D(e.getPoint())).filter(Variavel::modificavel).ifPresent(editarRegra);
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                teclaNaSugestao(e);
            }
        });
        configurarSugestoes();
    }

    /** Insere a variável no cursor (clique na variável da coluna lateral). */
    void inserir(Variavel variavel) {
        replaceSelection(variavel.marcador());
        requestFocusInWindow();
    }

    /** A variável conhecida cujo marcador contém a posição {@code pos} do texto. */
    private Optional<Variavel> variavelEm(int pos) {
        Matcher m = VARIAVEL.matcher(getText());
        while (m.find()) {
            if (m.start() <= pos && pos < m.end()) {
                String marcador = m.group();
                return variaveis.stream().filter(v -> v.marcador().equals(marcador)).findFirst();
            }
        }
        return Optional.empty();
    }

    /** Pinta as variáveis (as modificáveis em laranja) e o resto do texto com a cor normal. */
    private void colorir() {
        StyledDocument documento = getStyledDocument();
        String texto = getText();
        documento.setCharacterAttributes(0, texto.length(), cor(Tema.TEXTO), true);
        Matcher m = VARIAVEL.matcher(texto);
        while (m.find()) {
            String marcador = m.group();
            boolean modificavel = variaveis.stream()
                    .anyMatch(v -> v.marcador().equals(marcador) && v.modificavel());
            documento.setCharacterAttributes(m.start(), m.end() - m.start(), cor(corDaVariavel(modificavel)), true);
        }
    }

    /** Cor de uma variável no texto e na coluna lateral: laranja se a regra dela pode ser editada. */
    static Color corDaVariavel(boolean modificavel) {
        return modificavel ? Tema.LARANJA : Tema.ACENTO_CLARO;
    }

    private static SimpleAttributeSet cor(Color cor) {
        SimpleAttributeSet atributos = new SimpleAttributeSet();
        StyleConstants.setForeground(atributos, cor);
        return atributos;
    }

    // ---------- Autocompletar ----------

    private void configurarSugestoes() {
        lista.setFont(Tema.mono());
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setFocusable(false);
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object valor, int indice, boolean selecionado,
                                                          boolean foco) {
                JLabel item = (JLabel) super.getListCellRendererComponent(l, valor, indice, selecionado, false);
                Variavel v = (Variavel) valor;
                item.setText(v.marcador());
                item.setToolTipText(v.descricao());
                item.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                return item;
            }
        });
        lista.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                aceitarSugestao();
            }
        });
        sugestoes.setFocusable(false);
        sugestoes.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
        sugestoes.add(lista);
    }

    /** Abre, filtra ou fecha a lista de sugestões conforme o que está antes do cursor. */
    private void sugerir() {
        if (!isFocusOwner()) {
            fecharSugestoes();
            return;
        }
        int cursor = getCaretPosition();
        String antes;
        try {
            antes = getText(0, cursor);
        } catch (BadLocationException e) {
            fecharSugestoes();
            return;
        }
        Matcher m = INICIO_DE_VARIAVEL.matcher(antes);
        if (!m.find()) {
            fecharSugestoes();
            return;
        }
        String digitado = m.group(1);
        List<Variavel> combinam = variaveis.stream().filter(v -> v.nome().startsWith(digitado)).toList();
        if (combinam.isEmpty()) {
            combinam = variaveis.stream().filter(v -> v.nome().contains(digitado)).toList();
        }
        if (combinam.isEmpty()) {
            fecharSugestoes();
            return;
        }
        inicioDaSugestao = m.start();
        lista.setListData(combinam.toArray(Variavel[]::new));
        lista.setSelectedIndex(0);
        lista.setVisibleRowCount(Math.min(combinam.size(), 8));
        try {
            Rectangle2D r = modelToView2D(inicioDaSugestao);
            Rectangle area = r.getBounds();
            sugestoes.pack();
            Point p = new Point(area.x, area.y + area.height);
            sugestoes.show(this, p.x, p.y);
        } catch (BadLocationException e) {
            fecharSugestoes();
        }
    }

    private void teclaNaSugestao(KeyEvent e) {
        if (!sugestoes.isVisible()) {
            return;
        }
        int indice = lista.getSelectedIndex();
        int total = lista.getModel().getSize();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN -> lista.setSelectedIndex((indice + 1) % total);
            case KeyEvent.VK_UP -> lista.setSelectedIndex((indice - 1 + total) % total);
            case KeyEvent.VK_ENTER, KeyEvent.VK_TAB -> aceitarSugestao();
            case KeyEvent.VK_ESCAPE -> fecharSugestoes();
            default -> {
                return;
            }
        }
        lista.ensureIndexIsVisible(lista.getSelectedIndex());
        e.consume();
    }

    /** Troca o "{" e o que foi digitado depois dele pela variável escolhida. */
    private void aceitarSugestao() {
        Variavel escolhida = lista.getSelectedValue();
        int fim = getCaretPosition();
        fecharSugestoes();
        if (escolhida == null || inicioDaSugestao < 0 || inicioDaSugestao > fim) {
            return;
        }
        select(inicioDaSugestao, fim);
        replaceSelection(escolhida.marcador());
        inicioDaSugestao = -1;
        // A inserção reabriria a lista; o "}" no fim já impede, mas fecha de novo por garantia.
        SwingUtilities.invokeLater(this::fecharSugestoes);
        requestFocusInWindow();
    }

    private void fecharSugestoes() {
        sugestoes.setVisible(false);
    }
}
