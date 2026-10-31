package io.github.luanfrombrazil.overtimedispatcher.view.oraculo;

import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.Sugestao;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.BotaoIcone;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Cartao;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Chip;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Dialogo;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Etiqueta;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.ItemStatus;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Painel do Oráculo: feed de sugestões, diagnósticos, estudos, roadmaps e protocolos sugeridos.
 */
public class OraculoDialog extends Dialogo {

    private static final int LARGURA = 760;
    private static final Map<String, String> FILTROS = Map.of(
            "Sugestões", "Sugestão", "Diagnósticos", "Diagnóstico", "Estudos", "Estudo");

    private final List<Sugestao> sugestoes;
    private final JPanel feed = new JPanel();
    private final List<Chip> chips = new ArrayList<>();

    public OraculoDialog(JFrame dono, List<Sugestao> sugestoes, String leitura) {
        super(dono, Tema.ROXO);
        this.sugestoes = sugestoes;
        JPanel raiz = (JPanel) getContentPane();
        raiz.setBackground(Tema.PAINEL);

        raiz.add(cabecalho(leitura), BorderLayout.NORTH);
        feed.setLayout(new BoxLayout(feed, BoxLayout.Y_AXIS));
        feed.setOpaque(false);
        feed.setBorder(BorderFactory.createEmptyBorder(16, 22, 20, 22));
        raiz.add(feed, BorderLayout.CENTER);
        filtrar("Tudo");
    }

    private JComponent cabecalho(String leitura) {
        JPanel cabecalho = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Tema.ROXO_FUNDO);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Tema.ROXO);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        cabecalho.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 16));
        cabecalho.add(new JLabel(Ui.iconeOraculo(true, 48)), BorderLayout.WEST);
        cabecalho.add(Ui.coluna(2, Ui.titulo("Oráculo"), Ui.texto(leitura, Tema.legenda(), Tema.ROXO_CLARO)),
                BorderLayout.CENTER);

        List<java.awt.Component> direita = new ArrayList<>();
        for (String filtro : List.of("Tudo", "Sugestões", "Diagnósticos", "Estudos")) {
            Chip chip = new Chip(filtro, false, Tema.ROXO, Tema.ROXO);
            chip.aoClicar(() -> filtrar(filtro));
            chips.add(chip);
            direita.add(chip);
        }
        BotaoIcone fechar = new BotaoIcone(Ui.icone("fechar", 14, Tema.ROXO_CLARO), "Fechar");
        fechar.addActionListener(e -> fechar());
        direita.add(Box.createHorizontalStrut(6));
        direita.add(fechar);
        cabecalho.add(Ui.linha(6, direita.toArray(new java.awt.Component[0])), BorderLayout.EAST);
        return cabecalho;
    }

    private void filtrar(String filtro) {
        chips.forEach(c -> c.setSelecionado(c.getText().equals(filtro)));
        String tipo = FILTROS.get(filtro);
        feed.removeAll();
        boolean primeiro = true;
        for (Sugestao s : sugestoes) {
            if (tipo != null && !tipo.equals(s.tipo())) {
                continue;
            }
            if (!primeiro) {
                feed.add(Box.createVerticalStrut(10));
            }
            feed.add(item(s));
            primeiro = false;
        }
        feed.revalidate();
        feed.repaint();
        if (isDisplayable()) {
            pack();
        }
    }

    private JComponent item(Sugestao s) {
        Cartao cartao = new Cartao(Tema.ROXO_FUNDO, Tema.ROXO, new BorderLayout());
        cartao.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        ItemStatus linha = new ItemStatus(Estado.ORACULO, s.titulo(), s.porque(), null);
        List<JComponent> direita = new ArrayList<>();
        direita.add(Etiqueta.contorno(s.tipo(), Tema.ROXO_CLARO));
        if (s.acaoSecundaria() != null) {
            direita.add(Ui.botaoOraculoSecundario(s.acaoSecundaria()));
        }
        if (s.acaoPrincipal() != null) {
            JButton principal = Ui.botaoOraculo(s.acaoPrincipal());
            direita.add(principal);
        }
        linha.comDireita(direita.toArray(new JComponent[0]));
        cartao.add(linha, BorderLayout.CENTER);
        if (s.etapas() > 0) {
            cartao.add(roadmap(s.etapa(), s.etapas()), BorderLayout.SOUTH);
        }
        return Ui.largo(cartao);
    }

    private static JComponent roadmap(int etapa, int etapas) {
        JComponent barra = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                int largura = 52;
                for (int i = 0; i < etapas; i++) {
                    g.setColor(i < etapa ? Tema.ROXO : Tema.ATIVIDADE[0]);
                    g.fillRoundRect(i * (largura + 3), 0, largura, 6, 6, 6);
                }
            }
        };
        barra.setPreferredSize(new Dimension(etapas * 55, 6));
        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.setBorder(BorderFactory.createEmptyBorder(0, ItemStatus.LARGURA_BORDA + 12, 10, 12));
        envoltorio.add(barra, BorderLayout.WEST);
        return envoltorio;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(LARGURA, d.height);
    }
}
