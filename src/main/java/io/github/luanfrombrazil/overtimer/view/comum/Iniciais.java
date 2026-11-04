package io.github.luanfrombrazil.overtimer.view.comum;

import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ícone quadrado de um projeto, protocolo ou trilha: a imagem escolhida, se houver, ou as iniciais.
 */
public class Iniciais extends JComponent {

    /** Imagens já reduzidas, por caminho, data de alteração e tamanho, para não reler o arquivo a cada tela. */
    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();
    private static final int ARCO = 10;

    private String sigla;
    private final int tamanho;
    private BufferedImage imagem;

    public Iniciais(String sigla, int tamanho) {
        this(sigla, null, tamanho);
    }

    /** @param imagem caminho de uma imagem .png ou .jpg; null ou ilegível mostra as iniciais */
    public Iniciais(String sigla, String imagem, int tamanho) {
        this.sigla = sigla;
        this.tamanho = tamanho;
        this.imagem = carregar(imagem, tamanho);
        Dimension d = new Dimension(tamanho, tamanho);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
        repaint();
    }

    public void setImagem(String imagem) {
        this.imagem = carregar(imagem, tamanho);
        repaint();
    }

    /** Lê a imagem e a recorta no centro, em quadrado, já no tamanho do ícone. */
    private static BufferedImage carregar(String caminho, int tamanho) {
        if (caminho == null) {
            return null;
        }
        File arquivo = new File(caminho);
        String chave = arquivo.getAbsolutePath() + "|" + arquivo.lastModified() + "|" + tamanho;
        BufferedImage pronta = CACHE.get(chave);
        if (pronta != null) {
            return pronta;
        }
        try {
            BufferedImage original = ImageIO.read(arquivo);
            if (original == null) {
                return null;
            }
            int lado = Math.min(original.getWidth(), original.getHeight());
            BufferedImage quadrada = original.getSubimage((original.getWidth() - lado) / 2,
                    (original.getHeight() - lado) / 2, lado, lado);
            pronta = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = pronta.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(quadrada.getScaledInstance(tamanho, tamanho, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();
            CACHE.put(chave, pronta);
            return pronta;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (imagem != null) {
            g2.setClip(new RoundRectangle2D.Float(0, 0, tamanho, tamanho, ARCO, ARCO));
            g2.drawImage(imagem, 0, 0, null);
            g2.setClip(null);
            g2.setColor(Tema.BORDA);
            g2.drawRoundRect(0, 0, tamanho - 1, tamanho - 1, ARCO, ARCO);
            g2.dispose();
            return;
        }
        g2.setColor(Tema.SUTIL);
        g2.fillRoundRect(0, 0, tamanho - 1, tamanho - 1, 10, 10);
        g2.setColor(Tema.BORDA);
        g2.drawRoundRect(0, 0, tamanho - 1, tamanho - 1, 10, 10);
        g2.setFont(Tema.secao());
        g2.setColor(Tema.TEXTO);
        FontMetrics fm = g2.getFontMetrics();
        int x = (tamanho - fm.stringWidth(sigla)) / 2;
        int y = (tamanho - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(sigla, x, y);
        g2.dispose();
    }

    /** Duas primeiras iniciais das palavras de um nome. */
    public static String de(String nome) {
        StringBuilder sb = new StringBuilder();
        for (String parte : nome.trim().split("\\s+")) {
            if (!parte.isEmpty() && Character.isLetterOrDigit(parte.charAt(0)) && parte.length() > 2) {
                sb.append(Character.toUpperCase(parte.charAt(0)));
            }
            if (sb.length() == 2) {
                break;
            }
        }
        if (sb.isEmpty() && !nome.isBlank()) {
            sb.append(Character.toUpperCase(nome.trim().charAt(0)));
        }
        return sb.toString();
    }
}
