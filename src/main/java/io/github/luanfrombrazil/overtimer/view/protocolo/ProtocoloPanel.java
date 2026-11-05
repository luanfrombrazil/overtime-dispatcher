package io.github.luanfrombrazil.overtimer.view.protocolo;

import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.ExecucaoProtocolo;
import io.github.luanfrombrazil.overtimer.model.ItemFila;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.view.comum.Cartao;
import io.github.luanfrombrazil.overtimer.view.comum.Chip;
import io.github.luanfrombrazil.overtimer.view.comum.Console;
import io.github.luanfrombrazil.overtimer.view.comum.Etiqueta;
import io.github.luanfrombrazil.overtimer.view.comum.Interruptor;
import io.github.luanfrombrazil.overtimer.view.comum.ItemStatus;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Tela de um protocolo: passo a passo, acessos, fila de entrada, propostas e o console com a última execução e o
 * histórico.
 */
public class ProtocoloPanel extends JPanel {

    private static final int LARGURA_COLUNA_DIREITA = 360;
    private static final int ALTURA_CONSOLE = 180;
    private static final String ULTIMA = "Última execução";
    private static final String HISTORICO = "Histórico";

    private final List<ExecucaoProtocolo> historico;
    private Console console;
    /** Se o console já recebeu uma linha da execução em andamento, que substitui a última execução. */
    private boolean aoVivo;

    /**
     * @param historico últimas execuções, da mais recente para a mais antiga
     * @param nomeDe    nome do protocolo ou projeto com o id dado, para as origens das tasks injetadas
     * @param aoLigar   chamado com o novo estado quando o interruptor muda
     * @param aoRodar   pede a execução do protocolo no próximo ciclo do agendador
     */
    public ProtocoloPanel(Protocolo protocolo, List<ExecucaoProtocolo> historico, Function<String, String> nomeDe,
                          Consumer<Boolean> aoLigar, Runnable aoRodar, Runnable aoEditar) {
        super(new BorderLayout(0, 14));
        setBackground(Tema.APP);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JButton editar = Ui.botaoSecundario("Editar");
        editar.addActionListener(e -> aoEditar.run());
        Interruptor ativo = new Interruptor(protocolo.ativo());
        ativo.addPropertyChangeListener("ligado", e -> aoLigar.accept((Boolean) e.getNewValue()));
        JButton rodar = Ui.comIcone(Ui.botaoPrimario("Rodar agora"), "play");
        rodar.addActionListener(e -> aoRodar.run());
        add(Ui.coluna(10,
                Ui.cabecalho(Ui.titulo(protocolo.nome()), Ui.linha(8,
                        Etiqueta.solida(protocolo.provedor().toString(), Tema.BORDA),
                        Etiqueta.solida(protocolo.esforco().toString(), Tema.CINZA),
                        ativo, editar, rodar)),
                Ui.legenda(protocolo.agenda().descricao())), BorderLayout.NORTH);

        JPanel colunas = new JPanel(new BorderLayout(14, 0));
        colunas.setOpaque(false);
        colunas.add(alinharNoTopo(Ui.coluna(14, passos(protocolo), acessos(protocolo, nomeDe))), BorderLayout.CENTER);
        JPanel direita = Ui.coluna(14,
                fila("Fila de entrada", protocolo.filaDeEntrada(), nomeDe, Estado.PENDENTE, false),
                fila("Propostas", protocolo.propostas(), nomeDe, Estado.ATENCAO, true));
        JPanel envoltorioDireita = alinharNoTopo(direita);
        envoltorioDireita.setPreferredSize(new Dimension(LARGURA_COLUNA_DIREITA, 10));
        colunas.add(envoltorioDireita, BorderLayout.EAST);
        add(colunas, BorderLayout.CENTER);

        this.historico = List.copyOf(historico);
        if (!historico.isEmpty()) {
            mostrarConsole();
        }
    }

    private void mostrarConsole() {
        Map<String, List<String>> abas = new LinkedHashMap<>();
        abas.put(ULTIMA, historico.isEmpty() ? List.of() : ultima(historico.getFirst()));
        abas.put(HISTORICO, historico.stream().map(ProtocoloPanel::linhaDoHistorico).toList());
        console = new Console(abas, ALTURA_CONSOLE);
        add(console, BorderLayout.SOUTH);
        revalidate();
    }

    /** O resumo, o log e a resposta do agente. */
    private static List<String> ultima(ExecucaoProtocolo execucao) {
        List<String> linhas = new ArrayList<>();
        linhas.add(execucao.resumo());
        linhas.addAll(execucao.log());
        if (execucao.resultado() != null && !execucao.resultado().isBlank()) {
            linhas.add("");
            linhas.addAll(execucao.resultado().strip().lines().toList());
        }
        return linhas;
    }

    private static String linhaDoHistorico(ExecucaoProtocolo execucao) {
        String detalhe = execucao.primeiraLinha();
        return execucao.resumo() + (detalhe.isEmpty() ? "" : " · " + detalhe);
    }

    /**
     * Linha do log da execução em andamento: a primeira troca o conteúdo da "Última execução", que passa a mostrar a
     * execução ao vivo até ela terminar e a tela ser refeita com o histórico.
     */
    public void adicionarLog(String linha) {
        if (console == null) {
            mostrarConsole();
        }
        if (!aoVivo) {
            aoVivo = true;
            console.substituir(ULTIMA, List.of(linha));
            console.selecionar(ULTIMA);
        } else {
            console.adicionar(ULTIMA, linha);
        }
    }

    private static JPanel passos(Protocolo protocolo) {
        Cartao cartao = new Cartao(Tema.PAINEL, Tema.BORDA, new BorderLayout(0, 8));
        cartao.add(Ui.secao("Passo a passo"), BorderLayout.NORTH);
        String passos = protocolo.passos() == null ? "" : protocolo.passos();
        String html = passos.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
        cartao.add(Ui.corpo("<html><div style='width:500px'>" + html + "</div></html>"), BorderLayout.CENTER);
        return Ui.largo(cartao);
    }

    private static JPanel acessos(Protocolo protocolo, Function<String, String> nomeDe) {
        Cartao cartao = new Cartao(Tema.PAINEL, Tema.BORDA, null);
        cartao.setLayout(new BoxLayout(cartao, BoxLayout.Y_AXIS));
        cartao.add(Ui.cabecalho(Ui.secao("Acessos"), null));
        cartao.add(Box.createVerticalStrut(8));
        List<String> acessos = protocolo.acessos().rotulos();
        if (acessos.isEmpty()) {
            cartao.add(Ui.cabecalho(Ui.legenda("Nenhum"), null));
        } else {
            cartao.add(chips(acessos));
        }
        if (!protocolo.aceitaDiretoDe().isEmpty()) {
            cartao.add(Box.createVerticalStrut(10));
            cartao.add(Ui.cabecalho(Ui.legenda("Aceita direto de"), null));
            cartao.add(Box.createVerticalStrut(6));
            cartao.add(chips(protocolo.aceitaDiretoDe().stream().map(nomeDe).toList()));
        }
        return Ui.largo(cartao);
    }

    private static JPanel chips(List<String> textos) {
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        linha.setOpaque(false);
        for (String texto : textos) {
            linha.add(new Chip(texto, true));
        }
        return Ui.largo(linha);
    }

    private static JPanel fila(String titulo, List<ItemFila> itens, Function<String, String> nomeDe,
                               Estado estado, boolean comAcoes) {
        Cartao cartao = new Cartao(Tema.PAINEL, Tema.BORDA, null);
        cartao.setLayout(new BoxLayout(cartao, BoxLayout.Y_AXIS));
        cartao.add(Ui.cabecalho(Ui.secao(titulo), null));
        if (itens.isEmpty()) {
            cartao.add(Box.createVerticalStrut(8));
            cartao.add(Ui.cabecalho(Ui.legenda("Vazia"), null));
        }
        for (ItemFila item : itens) {
            cartao.add(Box.createVerticalStrut(8));
            cartao.add(new ItemStatus(estado, item.titulo(), nomeDe.apply(item.origem()), Tema.SUTIL));
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
