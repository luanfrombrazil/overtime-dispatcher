package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Barra fina de porcentagem (ex.: consumo de um provedor). Pode ter uma segunda porcentagem, em outro tom,
 * sobreposta à primeira na mesma barra (ex.: uso da sessão sobre o uso semanal).
 */
public class BarraProgresso extends JComponent {

    private int porcentagem;
    private final Color cor;
    private int segunda;
    private final Color corSegunda;

    public BarraProgresso(int porcentagem, Color cor, int largura) {
        this(porcentagem, cor, 0, null, largura, 6);
    }

    /**
     * @param segunda    porcentagem desenhada na mesma barra, sobreposta à primeira
     * @param corSegunda tom da segunda porcentagem, ou null para não desenhá-la
     */
    public BarraProgresso(int porcentagem, Color cor, int segunda, Color corSegunda, int largura, int altura) {
        this.porcentagem = limitar(porcentagem);
        this.cor = cor;
        this.segunda = limitar(segunda);
        this.corSegunda = corSegunda;
        Dimension d = new Dimension(largura, altura);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    public int getPorcentagem() {
        return porcentagem;
    }

    public void setPorcentagem(int porcentagem) {
        setPorcentagens(porcentagem, segunda);
    }

    public int getSegunda() {
        return segunda;
    }

    /** Troca as duas porcentagens de uma vez, redesenhando só se alguma mudou. */
    public void setPorcentagens(int porcentagem, int segunda) {
        int nova = limitar(porcentagem);
        int novaSegunda = limitar(segunda);
        if (nova != this.porcentagem || novaSegunda != this.segunda) {
            this.porcentagem = nova;
            this.segunda = novaSegunda;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arco = getHeight();
        g2.setColor(Tema.SUTIL);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arco, arco);
        // A maior vai por baixo, para as duas continuarem visíveis quando se sobrepõem.
        boolean segundaPorBaixo = corSegunda != null && segunda >= porcentagem;
        if (segundaPorBaixo) {
            preencher(g2, segunda, corSegunda, arco);
        }
        preencher(g2, porcentagem, cor, arco);
        if (corSegunda != null && !segundaPorBaixo) {
            preencher(g2, segunda, corSegunda, arco);
        }
        g2.dispose();
    }

    private void preencher(Graphics2D g2, int valor, Color tom, int arco) {
        if (valor <= 0) {
            return;
        }
        g2.setColor(tom);
        g2.fillRoundRect(0, 0, getWidth() * valor / 100, getHeight(), arco, arco);
    }

    private static int limitar(int porcentagem) {
        return Math.clamp(porcentagem, 0, 100);
    }
}
