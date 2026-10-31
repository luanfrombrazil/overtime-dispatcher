package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

/**
 * Item com borda colorida à esquerda indicando o estado, título e detalhe opcional.
 */
public class ItemStatus extends JPanel {

    public static final int LARGURA_BORDA = 6;

    private final Estado estado;
    private final Color fundo;

    public ItemStatus(Estado estado, String titulo, String detalhe, Color fundo) {
        super(new BorderLayout(10, 0));
        this.estado = estado;
        this.fundo = fundo;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, LARGURA_BORDA + 12, 8, 12));

        JLabel rotulo = Ui.secao(titulo);
        JPanel textos = detalhe == null || detalhe.isBlank()
                ? Ui.coluna(0, rotulo)
                : Ui.coluna(2, rotulo, Ui.texto(detalhe, Tema.legenda(), Tema.corDetalhe(estado)));
        add(textos, BorderLayout.CENTER);
    }

    /** Coloca componentes à direita do item (botões, indicadores). */
    public ItemStatus comDireita(JComponent... componentes) {
        add(Ui.linha(8, componentes), BorderLayout.EAST);
        return this;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (fundo != null) {
            g.setColor(fundo);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        g.setColor(Tema.cor(estado));
        g.fillRect(0, 0, LARGURA_BORDA, getHeight());
    }
}
