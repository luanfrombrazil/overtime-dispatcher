package io.github.luanfrombrazil.overtimedispatcher.view.protocolo;

import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.ItemFila;
import io.github.luanfrombrazil.overtimedispatcher.model.Protocolo;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Cartao;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Chip;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Console;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Etiqueta;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Interruptor;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.ItemStatus;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela de um protocolo: passo a passo, acessos, fila de entrada, propostas e o console.
 */
public class ProtocoloPanel extends JPanel {

    private static final int LARGURA_COLUNA_DIREITA = 360;

    public ProtocoloPanel(Protocolo protocolo, Runnable aoEditar) {
        super(new BorderLayout(0, 14));
        setBackground(Tema.APP);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JButton editar = Ui.botaoSecundario("Editar");
        editar.addActionListener(e -> aoEditar.run());
        add(Ui.coluna(10,
                Ui.cabecalho(Ui.titulo(protocolo.nome()), Ui.linha(8,
                        Etiqueta.solida(protocolo.provedor(), Tema.BORDA),
                        Etiqueta.solida(protocolo.esforco(), Tema.CINZA),
                        new Interruptor(true), editar, Ui.comIcone(Ui.botaoPrimario("Rodar agora"), "play"))),
                Ui.legenda(protocolo.horarios())), BorderLayout.NORTH);

        JPanel colunas = new JPanel(new BorderLayout(14, 0));
        colunas.setOpaque(false);
        colunas.add(alinharNoTopo(Ui.coluna(14, passos(protocolo), acessos(protocolo))), BorderLayout.CENTER);
        JPanel direita = Ui.coluna(14, fila("Fila de entrada", protocolo.filaDeEntrada(), Estado.PENDENTE, false),
                fila("Propostas", protocolo.propostas(), Estado.ATENCAO, true));
        JPanel envoltorioDireita = alinharNoTopo(direita);
        envoltorioDireita.setPreferredSize(new Dimension(LARGURA_COLUNA_DIREITA, 10));
        colunas.add(envoltorioDireita, BorderLayout.EAST);
        add(colunas, BorderLayout.CENTER);

        if (!protocolo.ultimaExecucao().isEmpty()) {
            add(new Console(List.of("Última execução", "Histórico"), protocolo.ultimaExecucao(), 150), BorderLayout.SOUTH);
        }
    }

    private static JPanel passos(Protocolo protocolo) {
        Cartao cartao = new Cartao(Tema.PAINEL, Tema.BORDA, new BorderLayout(0, 8));
        cartao.add(Ui.secao("Passo a passo"), BorderLayout.NORTH);
        String html = protocolo.passos().replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
        cartao.add(Ui.corpo("<html><div style='width:500px'>" + html + "</div></html>"), BorderLayout.CENTER);
        return Ui.largo(cartao);
    }

    private static JPanel acessos(Protocolo protocolo) {
        Cartao cartao = new Cartao(Tema.PAINEL, Tema.BORDA, null);
        cartao.setLayout(new BoxLayout(cartao, BoxLayout.Y_AXIS));
        cartao.add(Ui.cabecalho(Ui.secao("Acessos"), null));
        cartao.add(Box.createVerticalStrut(8));
        cartao.add(chips(protocolo.acessos(), false));
        cartao.add(Box.createVerticalStrut(10));
        cartao.add(Ui.cabecalho(Ui.legenda("Aceita direto de"), null));
        cartao.add(Box.createVerticalStrut(6));
        List<String> origens = new ArrayList<>(protocolo.aceitaDiretoDe());
        origens.add("+");
        cartao.add(chips(origens, true));
        return Ui.largo(cartao);
    }

    private static JPanel chips(List<String> textos, boolean ultimoNeutro) {
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        linha.setOpaque(false);
        for (int i = 0; i < textos.size(); i++) {
            boolean neutro = ultimoNeutro && i == textos.size() - 1;
            linha.add(new Chip(textos.get(i), !neutro));
        }
        return Ui.largo(linha);
    }

    private static JPanel fila(String titulo, List<ItemFila> itens, Estado estado, boolean comAcoes) {
        Cartao cartao = new Cartao(Tema.PAINEL, Tema.BORDA, null);
        cartao.setLayout(new BoxLayout(cartao, BoxLayout.Y_AXIS));
        cartao.add(Ui.cabecalho(Ui.secao(titulo), null));
        if (itens.isEmpty()) {
            cartao.add(Box.createVerticalStrut(8));
            cartao.add(Ui.cabecalho(Ui.legenda("Vazia"), null));
        }
        for (ItemFila item : itens) {
            cartao.add(Box.createVerticalStrut(8));
            cartao.add(new ItemStatus(estado, item.titulo(), item.origem(), Tema.SUTIL));
        }
        if (comAcoes && !itens.isEmpty()) {
            cartao.add(Box.createVerticalStrut(8));
            JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            acoes.setOpaque(false);
            acoes.add(Ui.botaoSecundario("Descartar"));
            acoes.add(Ui.botaoSecundario("Editar"));
            acoes.add(Ui.botaoPrimario("Aceitar"));
            cartao.add(Ui.largo(acoes));
        }
        return Ui.largo(cartao);
    }

    private static JPanel alinharNoTopo(JComponent componente) {
        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.add(componente, BorderLayout.NORTH);
        return envoltorio;
    }
}
