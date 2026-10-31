package io.github.luanfrombrazil.overtimedispatcher.view.comum;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Atalhos para montar as telas com o mesmo estilo em todo lugar.
 */
public final class Ui {

    private Ui() {
    }

    public static JLabel texto(String texto, Font fonte, Color cor) {
        JLabel label = new JLabel(texto);
        label.setFont(fonte);
        label.setForeground(cor);
        return label;
    }

    public static JLabel titulo(String texto) {
        return texto(texto, Tema.titulo(), Tema.TEXTO);
    }

    public static JLabel secao(String texto) {
        return texto(texto, Tema.secao(), Tema.TEXTO);
    }

    public static JLabel corpo(String texto) {
        return texto(texto, Tema.corpo(), Tema.TEXTO);
    }

    public static JLabel legenda(String texto) {
        return texto(texto, Tema.legenda(), Tema.TEXTO_SECUNDARIO);
    }

    /** Painel transparente com BoxLayout horizontal. */
    public static JPanel linha(int espaco, Component... filhos) {
        JPanel painel = transparente(BoxLayout.X_AXIS);
        adicionar(painel, espaco, true, filhos);
        return painel;
    }

    /** Painel transparente com BoxLayout vertical. */
    public static JPanel coluna(int espaco, Component... filhos) {
        JPanel painel = transparente(BoxLayout.Y_AXIS);
        adicionar(painel, espaco, false, filhos);
        return painel;
    }

    private static JPanel transparente(int eixo) {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, eixo));
        painel.setOpaque(false);
        return painel;
    }

    private static void adicionar(JPanel painel, int espaco, boolean horizontal, Component... filhos) {
        for (int i = 0; i < filhos.length; i++) {
            if (i > 0 && espaco > 0) {
                painel.add(horizontal ? Box.createHorizontalStrut(espaco) : Box.createVerticalStrut(espaco));
            }
            if (filhos[i] instanceof JComponent c) {
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
                c.setAlignmentY(Component.CENTER_ALIGNMENT);
            }
            painel.add(filhos[i]);
        }
    }

    /** Linha com um componente à esquerda e outro à direita, ocupando toda a largura. */
    public static JPanel cabecalho(Component esquerda, Component direita) {
        JPanel painel = new JPanel(new java.awt.BorderLayout(12, 0));
        painel.setOpaque(false);
        painel.add(esquerda, java.awt.BorderLayout.WEST);
        if (direita != null) {
            painel.add(direita, java.awt.BorderLayout.EAST);
        }
        return largo(painel);
    }

    public static Component expansor() {
        return Box.createHorizontalGlue();
    }

    public static <T extends JComponent> T margem(T componente, int cima, int esquerda, int baixo, int direita) {
        componente.setBorder(BorderFactory.createEmptyBorder(cima, esquerda, baixo, direita));
        return componente;
    }

    /** Impede que o BoxLayout estique o componente além do tamanho preferido. */
    public static <T extends JComponent> T fixo(T componente) {
        componente.setMaximumSize(componente.getPreferredSize());
        return componente;
    }

    /** Deixa o componente ocupar toda a largura, mantendo a altura preferida. */
    public static <T extends JComponent> T largo(T componente) {
        componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, componente.getPreferredSize().height));
        return componente;
    }

    public static JButton botaoPrimario(String texto) {
        return botao(texto, "background:#3574F0; foreground:#FFFFFF");
    }

    public static JButton botaoSecundario(String texto) {
        return botao(texto, "background:#2B2D30; foreground:#DFE1E5; borderColor:#43454A; borderWidth:1");
    }

    public static JButton botaoOraculo(String texto) {
        return botao(texto, "background:#8B5CF6; foreground:#FFFFFF");
    }

    public static JButton botaoOraculoSecundario(String texto) {
        return botao(texto, "background:#2A2440; foreground:#C4B5FD; borderColor:#8B5CF6; borderWidth:1");
    }

    /** Coloca um ícone branco à esquerda do texto do botão (ex.: play, pausa). */
    public static JButton comIcone(JButton botao, String icone) {
        botao.setIcon(icone(icone, 12, Color.WHITE));
        botao.setIconTextGap(8);
        return fixo(botao);
    }

    private static JButton botao(String texto, String estilo) {
        JButton botao = new JButton(texto);
        botao.setFont(Tema.corpo());
        botao.putClientProperty(FlatClientProperties.STYLE,
                "arc:8; margin:5,14,5,14; focusWidth:0; innerFocusWidth:0; borderWidth:0; " + estilo);
        botao.setFocusable(false);
        return Ui.fixo(botao);
    }

    /** Símbolo do Oráculo: detalhado (telas grandes) ou simples (barra lateral). */
    public static FlatSVGIcon iconeOraculo(boolean detalhado, int tamanho) {
        return new FlatSVGIcon("icones/oraculo-" + (detalhado ? "detalhado" : "simples") + ".svg", tamanho, tamanho);
    }

    public static FlatSVGIcon icone(String nome, int tamanho, Color cor) {
        FlatSVGIcon icone = new FlatSVGIcon("icones/" + nome + ".svg", tamanho, tamanho);
        icone.setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor));
        return icone;
    }
}
