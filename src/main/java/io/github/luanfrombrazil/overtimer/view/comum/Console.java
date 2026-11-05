package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Console com abas e linhas em fonte monoespaçada. Clicar numa aba mostra as linhas dela; linhas novas na aba
 * visível rolam o texto até o fim.
 */
public class Console extends Cartao {

    private static final int MAXIMO_DE_LINHAS = 500;

    private final JTextArea texto = new JTextArea();
    private final Map<String, List<String>> conteudo = new LinkedHashMap<>();
    private final Map<String, JLabel> rotulos = new LinkedHashMap<>();
    private String atual;

    /** As linhas vão na primeira aba; as demais começam vazias. */
    public Console(List<String> abas, List<String> linhas, int altura) {
        this(abasCom(abas, linhas), altura);
    }

    /** @param abas linhas de cada aba, na ordem; a primeira começa visível */
    public Console(Map<String, List<String>> abas, int altura) {
        super(Tema.LOG, Tema.BORDA, new BorderLayout(0, 6));
        setPreferredSize(new Dimension(100, altura));
        setMinimumSize(new Dimension(100, altura));

        abas.forEach((aba, linhas) -> {
            conteudo.put(aba, new ArrayList<>(linhas));
            JLabel rotulo = Ui.texto(aba, Tema.corpo(), Tema.TEXTO_SECUNDARIO);
            if (abas.size() > 1) {
                rotulo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                rotulo.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        selecionar(aba);
                    }
                });
            }
            rotulos.put(aba, rotulo);
        });
        add(Ui.linha(20, rotulos.values().toArray(new JLabel[0])), BorderLayout.NORTH);

        texto.setEditable(false);
        texto.setOpaque(false);
        texto.setFont(Tema.mono());
        texto.setForeground(Tema.TEXTO_LOG);
        texto.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        ((DefaultCaret) texto.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane rolagem = new JScrollPane(texto);
        rolagem.setOpaque(false);
        rolagem.getViewport().setOpaque(false);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(rolagem, BorderLayout.CENTER);

        if (!conteudo.isEmpty()) {
            selecionar(conteudo.keySet().iterator().next());
        }
    }

    private static Map<String, List<String>> abasCom(List<String> abas, List<String> linhas) {
        Map<String, List<String>> mapa = new LinkedHashMap<>();
        for (int i = 0; i < abas.size(); i++) {
            mapa.put(abas.get(i), i == 0 ? linhas : List.of());
        }
        return mapa;
    }

    /** Mostra a aba, destacando o rótulo dela. */
    public void selecionar(String aba) {
        if (!conteudo.containsKey(aba)) {
            return;
        }
        atual = aba;
        rotulos.forEach((nome, rotulo) -> {
            boolean selecionada = nome.equals(aba);
            rotulo.setFont(selecionada ? Tema.secao() : Tema.corpo());
            rotulo.setForeground(selecionada ? Tema.TEXTO_LOG : Tema.TEXTO_SECUNDARIO);
        });
        texto.setText("");
        conteudo.get(aba).forEach(this::escrever);
    }

    /** Troca as linhas da aba (ex.: uma execução nova começou). */
    public void substituir(String aba, List<String> linhas) {
        List<String> lista = conteudo.get(aba);
        if (lista == null) {
            return;
        }
        lista.clear();
        lista.addAll(linhas);
        if (aba.equals(atual)) {
            selecionar(aba);
        }
    }

    /** Acrescenta uma linha no fim da aba visível. */
    public void adicionar(String linha) {
        if (atual != null) {
            adicionar(atual, linha);
        }
    }

    /** Acrescenta uma linha no fim da aba, descartando as mais antigas acima do limite. */
    public void adicionar(String aba, String linha) {
        List<String> lista = conteudo.get(aba);
        if (lista == null) {
            return;
        }
        lista.add(linha);
        if (lista.size() > MAXIMO_DE_LINHAS) {
            lista.removeFirst();
        }
        if (aba.equals(atual)) {
            escrever(linha);
        }
    }

    private void escrever(String linha) {
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
