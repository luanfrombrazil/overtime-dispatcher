package io.github.luanfrombrazil.overtimer.view;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Trilha;
import io.github.luanfrombrazil.overtimer.view.comum.BotaoIcone;
import io.github.luanfrombrazil.overtimer.view.comum.IndicadorAgendador;
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Barra da esquerda, da altura da janela: no topo Home, agendador e recolher; abaixo o Oráculo, depois Projetos,
 * Protocolos e Trilhas. Recolhida, mostra só os botões do topo, em coluna.
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
    private final IndicadorAgendador estado = new IndicadorAgendador();
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
    private final JPanel coluna = new JPanel(new BorderLayout());
    private JComponent alturaTopo;
    private boolean recolhida;

    public BarraLateral(List<Projeto> projetos, List<Protocolo> protocolos, List<Trilha> trilhas,
                        int sugestoes, Ouvinte ouvinte) {
        super(new BorderLayout());
        this.ouvinte = ouvinte;
        setBackground(Tema.PAINEL);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDA));

        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setBackground(Tema.PAINEL);
        atualizar(projetos, protocolos, trilhas, sugestoes);

        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setBackground(Tema.PAINEL);
        envoltorio.add(conteudo, BorderLayout.NORTH);

        rolagem.setViewportView(envoltorio);
        rolagem.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.getVerticalScrollBar().setUnitIncrement(16);

        topo.setOpaque(false);
        coluna.setOpaque(false);
        coluna.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        mostrarBotaoAgendador(false);
        recolher.addActionListener(e -> setRecolhida(!recolhida));
        montar();
    }

    /** Faz o topo da barra ter a mesma altura de {@code referencia} (a barra superior). */
    public void alinharTopoCom(JComponent referencia) {
        alturaTopo = referencia;
        revalidate();
    }

    /** Recolhida, a barra mostra só Home, agendador e recolher, um embaixo do outro. */
    public void setRecolhida(boolean recolhida) {
        this.recolhida = recolhida;
        montar();
    }

    private void montar() {
        removeAll();
        topo.removeAll();
        coluna.removeAll();
        recolher.setToolTipText(recolhida ? "Expandir barra lateral" : "Recolher barra lateral");
        recolher.setAtivo(recolhida);
        if (recolhida) {
            topo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA));
            topo.add(centralizado(recolher), BorderLayout.CENTER);
            JPanel botoes = Ui.coluna(12, centralizado(home), centralizado(agendador), centralizado(estado));
            coluna.add(botoes, BorderLayout.NORTH);
            add(topo, BorderLayout.NORTH);
            add(coluna, BorderLayout.CENTER);
            setPreferredSize(new Dimension(LARGURA_RECOLHIDA, 100));
        } else {
            topo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
                    BorderFactory.createEmptyBorder(10, 16, 10, 12)));
            topo.add(Ui.linha(12, home, agendador, estado), BorderLayout.WEST);
            topo.add(Ui.linha(0, recolher), BorderLayout.EAST);
            add(topo, BorderLayout.NORTH);
            add(rolagem, BorderLayout.CENTER);
            setPreferredSize(new Dimension(LARGURA, 100));
        }
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
     * Mostra se o agendador está rodando: o símbolo de pausa ou o círculo girando. O que ele está fazendo só
     * aparece ao passar o mouse.
     */
    public void setAgendador(boolean rodando, String situacao) {
        mostrarBotaoAgendador(rodando);
        estado.setEstado(rodando, situacao);
        revalidate();
        repaint();
    }

    /**
     * Botão só com o símbolo: play azul para iniciar, quadrado vermelho para parar. O texto da ação fica na dica.
     */
    private void mostrarBotaoAgendador(boolean rodando) {
        agendador.setIcon(Ui.icone(rodando ? "parar" : "play", 14, Color.WHITE));
        agendador.setToolTipText(rodando ? "Parar agendador" : "Iniciar agendador");
        agendador.putClientProperty(FlatClientProperties.STYLE,
                "arc:8; margin:7,7,7,7; focusWidth:0; innerFocusWidth:0; borderWidth:0; foreground:#FFFFFF; "
                        + "background:" + (rodando ? "#C94F4F" : "#3574F0"));
        agendador.setMaximumSize(agendador.getPreferredSize());
    }

    /** Refaz as listas, mantendo o item selecionado. */
    public void atualizar(List<Projeto> projetos, List<Protocolo> protocolos, List<Trilha> trilhas, int sugestoes) {
        conteudo.removeAll();
        linhas.clear();
        conteudo.add(new BotaoOraculo(sugestoes, ouvinte::oraculo));

        conteudo.add(cabecalho("Projetos", "+ Projeto", ouvinte::novoProjeto));
        for (Projeto p : projetos) {
            adicionar(p, new LinhaLateral(p.estado(), p.nome(), p.sigla(), p.imagem(), p.detalhe(),
                    () -> ouvinte.projeto(p)));
        }
        conteudo.add(cabecalho("Protocolos", "+ Protocolo", ouvinte::novoProtocolo));
        for (Protocolo p : protocolos) {
            adicionar(p, new LinhaLateral(p.estado(), p.nome(), p.sigla(), p.detalhe(), () -> ouvinte.protocolo(p)));
        }
        conteudo.add(cabecalho("Trilhas", "+ Trilha", ouvinte::novaTrilha));
        for (Trilha t : trilhas) {
            adicionar(t, new LinhaLateral(t.estado(), t.nome(), t.sigla(), t.detalhe(), () -> ouvinte.trilha(t)));
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
        return item instanceof Projeto p ? p.id() : item;
    }

    private static JPanel cabecalho(String titulo, String rotuloBotao, Runnable acao) {
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

    /** Botão do Oráculo, com fundo levemente roxo. */
    private static class BotaoOraculo extends JPanel {

        BotaoOraculo(int sugestoes, Runnable acao) {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            add(new JLabel(Ui.iconeOraculo(false, 28)), BorderLayout.WEST);
            JPanel textos = sugestoes == 0
                    ? Ui.coluna(0, Ui.secao("Oráculo"))
                    : Ui.coluna(1, Ui.secao("Oráculo"), Ui.texto(sugestoes + (sugestoes == 1 ? " sugestão" : " sugestões"),
                    Tema.legenda(), Tema.ROXO_CLARO));
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
