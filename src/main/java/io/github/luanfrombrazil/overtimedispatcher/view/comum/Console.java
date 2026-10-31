package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Console com abas simples e linhas em fonte monoespaçada. Linhas novas rolam o texto até o fim.
 */
public class Console extends Cartao {

    private static final int MAXIMO_DE_LINHAS = 500;

    private final JTextArea texto = new JTextArea();

    public Console(List<String> abas, List<String> linhas, int altura) {
        super(Tema.LOG, Tema.BORDA, new BorderLayout(0, 6));
        setPreferredSize(new Dimension(100, altura));
        setMinimumSize(new Dimension(100, altura));

        List<JLabel> rotulos = new ArrayList<>();
        for (int i = 0; i < abas.size(); i++) {
            rotulos.add(i == 0
                    ? Ui.texto(abas.get(i), Tema.secao(), Tema.TEXTO_LOG)
                    : Ui.texto(abas.get(i), Tema.corpo(), Tema.TEXTO_SECUNDARIO));
        }
        add(Ui.linha(20, rotulos.toArray(new JLabel[0])), BorderLayout.NORTH);

        texto.setEditable(false);
        texto.setOpaque(false);
        texto.setFont(Tema.mono());
        texto.setForeground(Tema.TEXTO_LOG);
        texto.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        ((DefaultCaret) texto.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        linhas.forEach(this::adicionar);

        JScrollPane rolagem = new JScrollPane(texto);
        rolagem.setOpaque(false);
        rolagem.getViewport().setOpaque(false);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(rolagem, BorderLayout.CENTER);
    }

    /** Acrescenta uma linha no fim, descartando as mais antigas acima do limite. */
    public void adicionar(String linha) {
        if (texto.getDocument().getLength() > 0) {
            texto.append("\n");
        }
        texto.append(linha);
        int excesso = texto.getLineCount() - MAXIMO_DE_LINHAS;
        if (excesso > 0) {
            try {
                texto.replaceRange("", 0, texto.getLineEndOffset(excesso - 1));
            } catch (BadLocationException e) {
                // Não acontece: a linha existe.
            }
        }
        texto.setCaretPosition(texto.getDocument().getLength());
    }
}
