package io.github.luanfrombrazil.overtimer.view;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Trilha;
import io.github.luanfrombrazil.overtimer.view.comum.AnelGiratorio;
import io.github.luanfrombrazil.overtimer.view.comum.BotaoIcone;
import io.github.luanfrombrazil.overtimer.view.comum.LinhaLateral;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Barra da esquerda, da altura da janela: no topo Home, agendador e recolher; abaixo o Oráculo, depois Projetos,
 * Protocolos e Trilhas. Recolhida, vira uma faixa fina sem rótulos (ver {@link #setRecolhida}).
 */
public class BarraLateral extends JPanel {

    public static final int LARGURA = 280;
    public static final int LARGURA_RECOLHIDA = 56;

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
    private final JPanel conteudo = new JPanel();
    private final Ouvinte ouvinte;
    private Object selecionado;

    private final BotaoIcone home = new BotaoIcone(Ui.icone("home", 18, Tema.TEXTO), "Home");
    private final JButton agendador = Ui.botaoPrimario(null);
    /** Borda do botão do agendador, que gira enquanto ele roda. */
    private final AnelGiratorio anelAgendador = new AnelGiratorio(agendador, 8);
    private final BotaoIcone recolher = new BotaoIcone(Ui.icone("recolher", 18, Tema.TEXTO), "Recolher barra lateral");
    private final JPanel topo = new JPanel(new BorderLayout()) {
        /** Fica da altura da barra superior, para as bordas de baixo se alinharem. */
        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            if (alturaTopo != null) {
                d.height = alturaTopo.getPreferredSize().height;
            }
            return d;
        }
    };
    private final JScrollPane rolagem = new JScrollPane();
    private JComponent alturaTopo;
    private boolean recolhida;
    private List<Projeto> projetos;
    private List<Protocolo> protocolos;
    private List<Trilha> trilhas;
    private int sugestoes;

    public BarraLateral(List<Projeto> projetos, List<Protocolo> protocolos, List<Trilha> trilhas,
                        int sugestoes, Ouvinte ouvinte) {
        super(new BorderLayout());
        this.ouvinte = ouvinte;
        setBackground(Tema.PAINEL);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDA));

        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setBackground(Tema.PAINEL);
        atualizar(projetos, protocolos, trilhas, sugestoes);

        JPanel envoltorio = new LarguraDaBarra();
        envoltorio.setBackground(Tema.PAINEL);
        envoltorio.add(conteudo, BorderLayout.NORTH);

        rolagem.setViewportView(envoltorio);
        rolagem.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.getVerticalScrollBar().setUnitIncrement(16);

        topo.setOpaque(false);
        mostrarBotaoAgendador(false);
        agendador.setToolTipText("Iniciar agendador");
        // Como o antigo indicador de pausa, a dica do agendador aparece na hora, sem a espera habitual.
        agendador.addMouseListener(new MouseAdapter() {
            private int esperaPadrao;

            @Override
            public void mouseEntered(MouseEvent e) {
                esperaPadrao = ToolTipManager.sharedInstance().getInitialDelay();
                ToolTipManager.sharedInstance().setInitialDelay(0);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ToolTipManager.sharedInstance().setInitialDelay(esperaPadrao);
            }
        });
        recolher.addActionListener(e -> setRecolhida(!recolhida));
        add(topo, BorderLayout.NORTH);
        add(rolagem, BorderLayout.CENTER);
        montar();
    }

    /** Faz o topo da barra ter a mesma altura de {@code referencia} (a barra superior). */
    public void alinharTopoCom(JComponent referencia) {
        alturaTopo = referencia;
        revalidate();
    }

    /**
     * Recolhida, a barra vira uma faixa fina sem rótulos: o botão de expandir, o agendador, o Oráculo, um "+" por
     * seção e os ícones de cada projeto, protocolo e trilha.
     */
    public void setRecolhida(boolean recolhida) {
        this.recolhida = recolhida;
        montar();
        atualizar(projetos, protocolos, trilhas, sugestoes);
    }

    private void montar() {
        topo.removeAll();
        recolher.setToolTipText(recolhida ? "Expandir barra lateral" : "Recolher barra lateral");
        recolher.setAtivo(recolhida);
        if (recolhida) {
            topo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA));
            topo.add(centralizado(recolher), BorderLayout.CENTER);
            setPreferredSize(new Dimension(LARGURA_RECOLHIDA, 100));
        } else {
            topo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
                    // Sem folga vertical: a altura vem da barra superior e os botões ficam centralizados nela; com
                    // folga, o anel do agendador (mais alto que os botões) seria cortado.
                    BorderFactory.createEmptyBorder(0, 16, 0, 12)));
            // O anel já soma alguns pixels de folga em volta do botão, por isso o espaço menor.
            topo.add(Ui.linha(8, home, anelAgendador), BorderLayout.WEST);
            topo.add(Ui.linha(0, recolher), BorderLayout.EAST);
            setPreferredSize(new Dimension(LARGURA, 100));
        }
        // Recolhida, a barra de rolagem comeria boa parte da faixa: fica com largura zero (com a política NEVER a
        // roda do mouse pararia de rolar).
        rolagem.getVerticalScrollBar().setPreferredSize(recolhida ? new Dimension(0, 0) : null);
        revalidate();
        repaint();
    }

    private static JPanel centralizado(JComponent componente) {
        JPanel painel = new JPanel(new java.awt.GridBagLayout());
        painel.setOpaque(false);
        painel.add(componente);
        return painel;
    }

    public void aoClicarHome(Runnable acao) {
        home.addActionListener(e -> acao.run());
    }

    public void setHomeAtivo(boolean ativo) {
        home.setAtivo(ativo);
    }

    public void aoClicarAgendador(Runnable acao) {
        agendador.addActionListener(e -> acao.run());
    }

    /**
     * Mostra se o agendador está rodando: o botão alterna entre play e parar e, rodando, a sua borda gira. O que ele
     * está fazendo aparece ao passar o mouse no botão.
     */
    public void setAgendador(boolean rodando, String situacao) {
        mostrarBotaoAgendador(rodando);
        anelAgendador.setGirando(rodando);
        // A dica junta a ação do botão com a situação ("Pausado", "Rodando · Task"...).
        agendador.setToolTipText((rodando ? "Parar agendador" : "Iniciar agendador")
                + (situacao == null || situacao.isBlank() ? "" : " · " + situacao));
        revalidate();
        repaint();
    }

    /**
     * Botão só com o símbolo: play azul para iniciar, quadrado vermelho para parar. O texto da ação fica na dica.
     */
    private void mostrarBotaoAgendador(boolean rodando) {
        agendador.setIcon(Ui.icone(rodando ? "parar" : "play", 14, Color.WHITE));
        agendador.putClientProperty(FlatClientProperties.STYLE,
                "arc:8; margin:7,7,7,7; focusWidth:0; innerFocusWidth:0; borderWidth:0; foreground:#FFFFFF; "
                        + "background:" + (rodando ? "#C94F4F" : "#3574F0"));
        agendador.setMaximumSize(agendador.getPreferredSize());
    }

    /** Refaz as listas, mantendo o item selecionado. */
    public void atualizar(List<Projeto> projetos, List<Protocolo> protocolos, List<Trilha> trilhas, int sugestoes) {
        this.projetos = projetos;
        this.protocolos = protocolos;
        this.trilhas = trilhas;
        this.sugestoes = sugestoes;
        conteudo.removeAll();
        linhas.clear();
        if (recolhida) {
            // Na faixa fina o agendador sai do topo, que só tem espaço para o botão de expandir.
            JPanel botao = centralizado(anelAgendador);
            botao.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            conteudo.add(Ui.largo(botao));
        }
        conteudo.add(new BotaoOraculo(sugestoes, recolhida, ouvinte::oraculo));

        conteudo.add(cabecalho("Projetos", "+ Projeto", "Novo projeto", ouvinte::novoProjeto));
        for (Projeto p : projetos) {
            adicionar(p, new LinhaLateral(p.estado(), p.nome(), p.sigla(), p.imagem(), p.detalhe(), recolhida,
                    () -> ouvinte.projeto(p)));
        }
        conteudo.add(cabecalho("Protocolos", "+ Protocolo", "Novo protocolo", ouvinte::novoProtocolo));
        for (Protocolo p : protocolos) {
            adicionar(p, new LinhaLateral(p.estado(), p.nome(), p.sigla(), null, p.detalhe(), recolhida,
                    () -> ouvinte.protocolo(p)));
        }
        conteudo.add(cabecalho("Trilhas", "+ Trilha", "Nova trilha", ouvinte::novaTrilha));
        for (Trilha t : trilhas) {
            adicionar(t, new LinhaLateral(t.estado(), t.nome(), t.sigla(), null, t.detalhe(), recolhida,
                    () -> ouvinte.trilha(t)));
        }
        selecionar(selecionado);
        conteudo.revalidate();
        conteudo.repaint();
    }

    private void adicionar(Object item, LinhaLateral linha) {
        linhas.put(chave(item), linha);
        conteudo.add(linha);
    }

    /** Destaca o item aberto; null limpa a seleção. */
    public void selecionar(Object item) {
        selecionado = item;
        Object procurada = chave(item);
        linhas.forEach((chave, linha) -> linha.setSelecionada(chave.equals(procurada)));
    }

    /** Projetos são identificados pelo id, porque cada alteração gera um registro novo. */
    private static Object chave(Object item) {
        if (item instanceof Projeto p) {
            return p.id();
        }
        return item instanceof Protocolo protocolo ? protocolo.id() : item;
    }

    /** Título da seção e botão de criar; recolhida, só o botão "+", com a ação na dica. */
    private JPanel cabecalho(String titulo, String rotuloBotao, String dica, Runnable acao) {
        if (recolhida) {
            JButton botao = Ui.botaoPrimario("+");
            botao.setToolTipText(dica);
            botao.addActionListener(e -> acao.run());
            JPanel linha = centralizado(botao);
            linha.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));
            return Ui.largo(linha);
        }
        JButton botao = Ui.botaoPrimario(rotuloBotao);
        // Os três botões ficam com a largura do maior rótulo, "+ Protocolo".
        Dimension tamanho = Ui.botaoPrimario("+ Protocolo").getPreferredSize();
        botao.setPreferredSize(tamanho);
        botao.setMaximumSize(tamanho);
        botao.addActionListener(e -> acao.run());
        JPanel linha = new JPanel(new BorderLayout());
        linha.setOpaque(false);
        linha.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
        linha.add(Ui.secao(titulo), BorderLayout.WEST);
        linha.add(botao, BorderLayout.EAST);
        return Ui.largo(linha);
    }

    /**
     * Conteúdo rolável que acompanha a largura visível da barra: sem isso, um nome comprido alargaria o conteúdo
     * além da borda e o texto seria cortado fora de vista, em vez de terminar em reticências.
     */
    private static class LarguraDaBarra extends JPanel implements Scrollable {

        LarguraDaBarra() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visivel, int orientacao, int direcao) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visivel, int orientacao, int direcao) {
            return orientacao == SwingConstants.VERTICAL ? visivel.height : visivel.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    /** Botão do Oráculo, com fundo levemente roxo e o símbolo detalhado. Compacto, só o símbolo, centralizado. */
    private static class BotaoOraculo extends JPanel {

        /** Um pouco maior que os ícones das linhas (28), para o símbolo detalhado aparecer bem. */
        private static final int TAMANHO_ICONE = 36;

        private final boolean compacto;

        BotaoOraculo(int sugestoes, boolean compacto, Runnable acao) {
            super(new BorderLayout(12, 0));
            this.compacto = compacto;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            String rotuloSugestoes = sugestoes + (sugestoes == 1 ? " sugestão" : " sugestões");
            if (compacto) {
                setToolTipText(sugestoes == 0 ? "Oráculo" : "Oráculo · " + rotuloSugestoes);
                JLabel icone = new JLabel(Ui.iconeOraculo(true, TAMANHO_ICONE));
                icone.setHorizontalAlignment(JLabel.CENTER);
                add(icone, BorderLayout.CENTER);
            } else {
                setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 12));
                add(new JLabel(Ui.iconeOraculo(true, TAMANHO_ICONE)), BorderLayout.WEST);
                JPanel textos = sugestoes == 0
                        ? Ui.coluna(0, Ui.secao("Oráculo"))
                        : Ui.coluna(1, Ui.secao("Oráculo"), Ui.texto(rotuloSugestoes, Tema.legenda(), Tema.ROXO_CLARO));
                JPanel centro = new JPanel(new java.awt.GridBagLayout());
                centro.setOpaque(false);
                java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
                c.weightx = 1;
                c.fill = java.awt.GridBagConstraints.HORIZONTAL;
                centro.add(textos, c);
                add(centro, BorderLayout.CENTER);
            }
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    acao.run();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(compacto ? LARGURA_RECOLHIDA : LARGURA, 56);
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
