package io.github.luanfrombrazil.overtimedispatcher.view;

import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Protocolo;
import io.github.luanfrombrazil.overtimedispatcher.model.Trilha;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.LinhaLateral;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Barra da esquerda: Oráculo no topo, depois Projetos, Protocolos e Trilhas.
 */
public class BarraLateral extends JScrollPane {

    public static final int LARGURA = 280;

    /** Ações disparadas pela barra lateral. */
    public interface Ouvinte {
        void oraculo();

        void projeto(Projeto projeto);

        void protocolo(Protocolo protocolo);

        void trilha(Trilha trilha);

        void novoProjeto();

        void novoProtocolo();

        void novaTrilha();
    }

    private final Map<Object, LinhaLateral> linhas = new HashMap<>();

    public BarraLateral(List<Projeto> projetos, List<Protocolo> protocolos, List<Trilha> trilhas,
                        int sugestoes, Ouvinte ouvinte) {
        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setBackground(Tema.PAINEL);

        conteudo.add(new BotaoOraculo(sugestoes, ouvinte::oraculo));

        conteudo.add(cabecalho("Projetos", "+ Projeto", ouvinte::novoProjeto));
        for (Projeto p : projetos) {
            adicionar(conteudo, p, new LinhaLateral(p.estado(), p.nome(), p.sigla(), p.detalhe(), () -> ouvinte.projeto(p)));
        }
        conteudo.add(cabecalho("Protocolos", "+ Protocolo", ouvinte::novoProtocolo));
        for (Protocolo p : protocolos) {
            adicionar(conteudo, p, new LinhaLateral(p.estado(), p.nome(), p.sigla(), p.detalhe(), () -> ouvinte.protocolo(p)));
        }
        conteudo.add(cabecalho("Trilhas", "+ Trilha", ouvinte::novaTrilha));
        for (Trilha t : trilhas) {
            adicionar(conteudo, t, new LinhaLateral(t.estado(), t.nome(), t.sigla(), t.detalhe(), () -> ouvinte.trilha(t)));
        }

        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setBackground(Tema.PAINEL);
        envoltorio.add(conteudo, BorderLayout.NORTH);

        setViewportView(envoltorio);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDA));
        getVerticalScrollBar().setUnitIncrement(16);
        setPreferredSize(new Dimension(LARGURA, 100));
    }

    private void adicionar(JPanel conteudo, Object item, LinhaLateral linha) {
        linhas.put(item, linha);
        conteudo.add(linha);
    }

    /** Destaca o item aberto; null limpa a seleção. */
    public void selecionar(Object item) {
        linhas.forEach((chave, linha) -> linha.setSelecionada(chave.equals(item)));
    }

    private static JPanel cabecalho(String titulo, String rotuloBotao, Runnable acao) {
        JButton botao = Ui.botaoPrimario(rotuloBotao);
        botao.addActionListener(e -> acao.run());
        JPanel linha = new JPanel(new BorderLayout());
        linha.setOpaque(false);
        linha.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
        linha.add(Ui.secao(titulo), BorderLayout.WEST);
        linha.add(botao, BorderLayout.EAST);
        return Ui.largo(linha);
    }

    /** Botão do Oráculo, com fundo levemente roxo. */
    private static class BotaoOraculo extends JPanel {

        BotaoOraculo(int sugestoes, Runnable acao) {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            add(new JLabel(Ui.iconeOraculo(false, 28)), BorderLayout.WEST);
            JPanel textos = Ui.coluna(1, Ui.secao("Oráculo"),
                    Ui.texto(sugestoes + " sugestões", Tema.legenda(), Tema.ROXO_CLARO));
            JPanel centro = new JPanel(new java.awt.GridBagLayout());
            centro.setOpaque(false);
            java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
            c.weightx = 1;
            c.fill = java.awt.GridBagConstraints.HORIZONTAL;
            centro.add(textos, c);
            add(centro, BorderLayout.CENTER);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    acao.run();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(LARGURA, 56);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 56);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Tema.ROXO_FUNDO);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Tema.ROXO);
            g.fillRect(0, getHeight() - 1, getWidth(), 1);
        }
    }
}
