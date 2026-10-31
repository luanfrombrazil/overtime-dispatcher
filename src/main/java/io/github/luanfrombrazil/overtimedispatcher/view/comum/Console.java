package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Console com abas simples e linhas em fonte monoespaçada.
 */
public class Console extends Cartao {

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

        JPanel corpo = Ui.coluna(2);
        for (String linha : linhas) {
            corpo.add(Ui.texto(linha, Tema.mono(), Tema.TEXTO_LOG));
        }
        corpo.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        add(corpo, BorderLayout.CENTER);
    }
}
