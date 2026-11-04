package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
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
        if (compacta) {
            setBorder(BorderFactory.createEmptyBorder(0, ItemStatus.LARGURA_BORDA, 0, 0));
            setToolTipText(detalhe == null ? nome : nome + " · " + detalhe);
            add(icone, BorderLayout.CENTER);
        } else {
            setBorder(BorderFactory.createEmptyBorder(0, ItemStatus.LARGURA_BORDA + 10, 0, 12));
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

    private JPanel textos(String nome, String detalhe) {
        JLabel rotulo = Ui.secao(nome);
        JPanel textos = detalhe == null
                ? Ui.coluna(0, rotulo)
                : Ui.coluna(1, rotulo, Ui.texto(detalhe, Tema.legenda(), Tema.corDetalhe(estado)));
        JPanel centro = new JPanel(new java.awt.GridBagLayout());
        centro.setOpaque(false);
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.weightx = 1;
        c.fill = java.awt.GridBagConstraints.HORIZONTAL;
        centro.add(textos, c);
        return centro;
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
