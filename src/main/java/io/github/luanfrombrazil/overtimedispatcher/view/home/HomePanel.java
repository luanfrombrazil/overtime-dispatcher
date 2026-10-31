package io.github.luanfrombrazil.overtimedispatcher.view.home;

import io.github.luanfrombrazil.overtimedispatcher.model.BlocoAgendado;
import io.github.luanfrombrazil.overtimedispatcher.model.Consumo;
import io.github.luanfrombrazil.overtimedispatcher.model.Estatistica;
import io.github.luanfrombrazil.overtimedispatcher.model.ItemResumo;
import io.github.luanfrombrazil.overtimedispatcher.service.prototipo.DadosPrototipo;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.BarraProgresso;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Cartao;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.GraficoLinha;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.ItemStatus;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Home: estatísticas, mapa de atividade, última sessão, o que precisa de você e o que está marcado para hoje.
 */
public class HomePanel extends JPanel {

    private static final int LARGURA_COLUNA_DIREITA = 320;

    public HomePanel(DadosPrototipo dados) {
        super(new GridBagLayout());
        setBackground(Tema.APP);
        setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new java.awt.Insets(0, 0, 14, 0);

        c.gridy = 0;
        add(estatisticas(dados.estatisticas()), c);
        c.gridy = 1;
        add(mapa(dados), c);
        c.gridy = 2;
        c.weighty = 1;
        add(rodape(dados), c);
    }

    private static JPanel estatisticas(List<Estatistica> estatisticas) {
        JPanel linha = new JPanel(new GridLayout(1, estatisticas.size(), 12, 0));
        linha.setOpaque(false);
        for (Estatistica e : estatisticas) {
            Cartao cartao = new Cartao(Tema.PAINEL, Tema.BORDA, new BorderLayout(0, 8));
            cartao.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            JLabel numero = Ui.texto(e.valor(), Tema.numero(),
                    e.cor() == io.github.luanfrombrazil.overtimedispatcher.model.Estado.EM_ANDAMENTO
                            || e.cor() == io.github.luanfrombrazil.overtimedispatcher.model.Estado.CONCLUIDO
                            ? Tema.TEXTO : Tema.cor(e.cor()));
            cartao.add(Ui.coluna(0, numero, Ui.legenda(e.rotulo())), BorderLayout.NORTH);
            cartao.add(new GraficoLinha(e.serie(), Tema.cor(e.cor()), 34), BorderLayout.CENTER);
            linha.add(cartao);
        }
        return linha;
    }

    private static JPanel mapa(DadosPrototipo dados) {
        Cartao cartao = new Cartao(Tema.PAINEL, Tema.BORDA, new BorderLayout(0, 10));
        cartao.add(Ui.cabecalho(Ui.secao(dados.resumoAtividade()),
                Ui.linha(16, Ui.legenda(dados.horasDeOvertime()), MapaAtividade.legenda())), BorderLayout.NORTH);
        JPanel grade = new JPanel(new BorderLayout());
        grade.setOpaque(false);
        grade.add(new MapaAtividade(dados.atividade()), BorderLayout.WEST);
        cartao.add(grade, BorderLayout.CENTER);
        return cartao;
    }

    private static JPanel rodape(DadosPrototipo dados) {
        JPanel rodape = new JPanel(new BorderLayout(16, 0));
        rodape.setOpaque(false);

        JPanel esquerda = new JPanel(new BorderLayout());
        esquerda.setOpaque(false);
        esquerda.add(ultimaSessao(dados), BorderLayout.NORTH);
        rodape.add(esquerda, BorderLayout.CENTER);

        JPanel direita = new JPanel();
        direita.setLayout(new BoxLayout(direita, BoxLayout.Y_AXIS));
        direita.setOpaque(false);
        direita.add(precisaDeVoce(dados.precisaDeVoce()));
        direita.add(javax.swing.Box.createVerticalStrut(16));
        direita.add(hoje(dados.blocosDeHoje(), dados.prazos()));
        JPanel envoltorio = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(LARGURA_COLUNA_DIREITA, super.getPreferredSize().height);
            }
        };
        envoltorio.setOpaque(false);
        envoltorio.add(direita, BorderLayout.NORTH);
        rodape.add(envoltorio, BorderLayout.EAST);
        return rodape;
    }

    private static JPanel ultimaSessao(DadosPrototipo dados) {
        Cartao cartao = new Cartao(Tema.PAINEL, Tema.BORDA, null);
        cartao.setLayout(new BoxLayout(cartao, BoxLayout.Y_AXIS));
        cartao.add(Ui.cabecalho(Ui.secao("Última sessão"), Ui.legenda(dados.horarioUltimaSessao())));
        cartao.add(javax.swing.Box.createVerticalStrut(8));
        List<JComponent> consumo = new ArrayList<>();
        for (Consumo c : dados.consumoUltimaSessao()) {
            consumo.add(Ui.linha(6, Ui.legenda(c.provedor()), new BarraProgresso(c.porcentagem(), Tema.ACENTO, 70),
                    Ui.legenda(c.porcentagem() + "%")));
        }
        cartao.add(Ui.largo(Ui.linha(16, consumo.toArray(new JComponent[0]))));
        for (ItemResumo item : dados.ultimaSessao()) {
            cartao.add(javax.swing.Box.createVerticalStrut(6));
            cartao.add(new ItemStatus(item.estado(), item.titulo(), item.detalhe(), Tema.SUTIL));
        }
        cartao.add(javax.swing.Box.createVerticalStrut(8));
        cartao.add(Ui.cabecalho(Ui.legenda("+" + dados.concluidasForaDoResumo() + " concluídas"), null));
        return cartao;
    }

    private static JPanel precisaDeVoce(List<ItemResumo> itens) {
        Cartao cartao = new Cartao(Tema.PAINEL, Tema.BORDA, null);
        cartao.setLayout(new BoxLayout(cartao, BoxLayout.Y_AXIS));
        cartao.add(Ui.cabecalho(Ui.secao("Precisa de você"), null));
        for (ItemResumo item : itens) {
            cartao.add(javax.swing.Box.createVerticalStrut(8));
            cartao.add(new ItemStatus(item.estado(), item.titulo(), item.detalhe(), Tema.SUTIL));
        }
        return Ui.largo(cartao);
    }

    private static JPanel hoje(List<BlocoAgendado> blocos, List<String> prazos) {
        Cartao cartao = new Cartao(Tema.PAINEL, Tema.BORDA, null);
        cartao.setLayout(new BoxLayout(cartao, BoxLayout.Y_AXIS));
        cartao.add(Ui.cabecalho(Ui.secao("Hoje"), Ui.legenda("Trilhas")));
        for (BlocoAgendado bloco : blocos) {
            cartao.add(javax.swing.Box.createVerticalStrut(8));
            JPanel linha = new JPanel(new BorderLayout(10, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    g.setColor(Tema.SUTIL);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Tema.ACENTO);
                    g.fillRect(0, 0, 3, getHeight());
                }
            };
            linha.setOpaque(false);
            linha.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 10));
            linha.add(Ui.secao(bloco.hora()), BorderLayout.WEST);
            linha.add(Ui.corpo(bloco.titulo()), BorderLayout.CENTER);
            cartao.add(Ui.largo(linha));
        }
        List<JComponent> rotulos = new ArrayList<>();
        rotulos.add(Ui.legenda("Prazos:"));
        for (int i = 0; i < prazos.size(); i++) {
            rotulos.add(Ui.texto(prazos.get(i), Tema.legenda(), i == 0 ? Tema.AMARELO : Tema.TEXTO_SECUNDARIO));
        }
        cartao.add(javax.swing.Box.createVerticalStrut(10));
        cartao.add(Ui.cabecalho(Ui.linha(8, rotulos.toArray(new JComponent[0])), null));
        return Ui.largo(cartao);
    }
}
