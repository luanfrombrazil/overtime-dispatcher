package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Linha da barra lateral: borda de estado, iniciais, nome e motivo opcional. Altura fixa.
 */
public class LinhaLateral extends JPanel {

    public static final int ALTURA = 44;

    private final Estado estado;
    private boolean selecionada;
    private boolean sobre;

    public LinhaLateral(Estado estado, String nome, String sigla, String detalhe, Runnable aoClicar) {
        this(estado, nome, sigla, null, detalhe, aoClicar);
    }

    /** @param imagem caminho da imagem do ícone, ou null para mostrar as iniciais */
    public LinhaLateral(Estado estado, String nome, String sigla, String imagem, String detalhe, Runnable aoClicar) {
        this(estado, nome, sigla, imagem, detalhe, false, aoClicar);
    }

    /**
     * @param compacta só o ícone, centralizado, sem nome nem motivo (barra lateral recolhida); o nome fica na dica
     */
    public LinhaLateral(Estado estado, String nome, String sigla, String imagem, String detalhe, boolean compacta,
                        Runnable aoClicar) {
        super(new BorderLayout(10, 0));
        this.estado = estado;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel icone = new JPanel(new java.awt.GridBagLayout());
        icone.setOpaque(false);
        icone.add(new Iniciais(sigla, imagem, 28));
        // A folga de 1 px embaixo é a linha divisória pintada em paintComponent: sem ela o conteúdo seria
        // centralizado contando a linha e ficaria meio pixel acima do meio da área visível.
        if (compacta) {
            setBorder(BorderFactory.createEmptyBorder(0, ItemStatus.LARGURA_BORDA, 1, 0));
            setToolTipText(detalhe == null ? nome : nome + " · " + detalhe);
            add(icone, BorderLayout.CENTER);
        } else {
            setBorder(BorderFactory.createEmptyBorder(0, ItemStatus.LARGURA_BORDA + 10, 1, 12));
            add(icone, BorderLayout.WEST);
            add(textos(nome, detalhe), BorderLayout.CENTER);
        }
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                aoClicar.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                sobre = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sobre = false;
                repaint();
            }
        });
    }

    /**
     * Nome e motivo empilhados, centralizados na vertical e alinhados à esquerda. Coluna com folgas elásticas em
     * cima e embaixo: diferente do GridBagLayout, que com um nome mais largo que a linha passa a posicionar pelo
     * tamanho mínimo e desloca o texto, aqui o rótulo só é cortado com reticências.
     */
    private JPanel textos(String nome, String detalhe) {
        JPanel coluna = new JPanel();
        coluna.setLayout(new BoxLayout(coluna, BoxLayout.Y_AXIS));
        coluna.setOpaque(false);
        coluna.add(Box.createVerticalGlue());
        coluna.add(aEsquerda(Ui.secao(nome)));
        if (detalhe != null) {
            coluna.add(Box.createVerticalStrut(1));
            coluna.add(aEsquerda(Ui.texto(detalhe, Tema.legenda(), Tema.corDetalhe(estado))));
        }
        coluna.add(Box.createVerticalGlue());
        return coluna;
    }

    private static JLabel aEsquerda(JLabel rotulo) {
        rotulo.setAlignmentX(LEFT_ALIGNMENT);
        return rotulo;
    }

    public void setSelecionada(boolean selecionada) {
        this.selecionada = selecionada;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, ALTURA);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, ALTURA);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(selecionada ? Tema.SELECIONADO : sobre ? Tema.SUTIL : Tema.PAINEL);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Tema.cor(estado));
        g.fillRect(0, 0, ItemStatus.LARGURA_BORDA, getHeight());
        g.setColor(Tema.BORDA);
        g.fillRect(0, getHeight() - 1, getWidth(), 1);
    }
}
