package io.github.luanfrombrazil.overtimer.controller;

import io.github.luanfrombrazil.overtimer.model.Configuracoes;
import io.github.luanfrombrazil.overtimer.model.Notificacao;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Trilha;
import io.github.luanfrombrazil.overtimer.service.DadosService;
import io.github.luanfrombrazil.overtimer.service.ProjetoService;
import io.github.luanfrombrazil.overtimer.service.comunicacao.NotificacaoService;
import io.github.luanfrombrazil.overtimer.service.consumo.ConsumoService;
import io.github.luanfrombrazil.overtimer.service.persistence.ConfiguracoesRepositorio;
import io.github.luanfrombrazil.overtimer.service.scheduler.Agendador;
import io.github.luanfrombrazil.overtimer.view.Bandeja;
import io.github.luanfrombrazil.overtimer.view.BarraLateral;
import io.github.luanfrombrazil.overtimer.view.BarraSuperior;
import io.github.luanfrombrazil.overtimer.view.MainFrame;
import io.github.luanfrombrazil.overtimer.view.config.ConfiguracoesDialog;
import io.github.luanfrombrazil.overtimer.view.home.HomePanel;
import io.github.luanfrombrazil.overtimer.view.notificacoes.NotificacoesDialog;
import io.github.luanfrombrazil.overtimer.view.oraculo.OraculoDialog;
import io.github.luanfrombrazil.overtimer.view.protocolo.NovoProtocoloDialog;
import io.github.luanfrombrazil.overtimer.view.protocolo.ProtocoloPanel;
import io.github.luanfrombrazil.overtimer.view.trilha.TrilhaPanel;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.UncheckedIOException;

/**
 * Liga as ações da janela principal: navegação entre telas e abertura dos pop-ups.
 */
public class MainController implements BarraLateral.Ouvinte {

    private final DadosService dados;
    private final ProjetoService projetos;
    private final Agendador agendador;
    private final ConsumoService consumo;
    private final NotificacaoService notificacoes;
    private final ConfiguracoesRepositorio repositorioConfiguracoes = new ConfiguracoesRepositorio();
    private final BarraSuperior barraSuperior;
    private final BarraLateral barraLateral;
    private final MainFrame frame;
    private final ProjetoController projetoController;
    private String projetoAberto;

    public MainController(DadosService dados, ProjetoService projetos, Agendador agendador, ConsumoService consumo,
                          NotificacaoService notificacoes) {
        this.dados = dados;
        this.projetos = projetos;
        this.agendador = agendador;
        this.consumo = consumo;
        this.notificacoes = notificacoes;
        this.barraSuperior = new BarraSuperior(consumo.consumo(), notificacoes.naoLidas());
        this.barraLateral = new BarraLateral(projetos.projetos(), dados.protocolos(), dados.trilhas(),
                dados.sugestoesDoOraculo(), this);
        this.frame = new MainFrame(barraSuperior, barraLateral);
        this.projetoController = new ProjetoController(frame, projetos, agendador::log,
                this::projetoAlterado, this::projetoExcluido);

        barraLateral.aoClicarHome(this::home);
        barraLateral.aoClicarAgendador(this::alternarAgendador);
        barraSuperior.aoClicarConfiguracoes(this::configuracoes);
        barraSuperior.aoClicarNotificacoes(this::notificacoes);
        frame.bandeja().ifPresent(b -> b.aoSair(this::sair));

        // O agendador e o serviço avisam na thread do agendador; a tela só muda na thread do Swing.
        agendador.aoMudar(new Agendador.Ouvinte() {
            @Override
            public void situacao(Agendador.Situacao situacao) {
                SwingUtilities.invokeLater(() -> barraLateral.setAgendador(situacao.rodando(), situacao.texto()));
            }

            @Override
            public void taskIniciada(String projetoId, String taskId) {
                // Uma Task vai gastar do provedor: mede agora para a barra mostrar o ponto de partida.
                consumo.atualizar();
            }

            @Override
            public void log(String projetoId, String linha) {
                SwingUtilities.invokeLater(() -> projetoController.adicionarLog(projetoId, linha));
            }
        });
        consumo.aoMudar(medido -> {
            SwingUtilities.invokeLater(() -> barraSuperior.setConsumo(medido));
            // Uma medição nova pode tirar um provedor do limite de uso (ou pôr nele).
            agendador.agora();
        });
        notificacoes.aoMudar(new NotificacaoService.Ouvinte() {
            @Override
            public void mudou(int naoLidas) {
                SwingUtilities.invokeLater(() -> barraSuperior.setNaoLidas(naoLidas));
            }
        });
        projetos.aoMudar(p -> SwingUtilities.invokeLater(() -> {
            atualizarBarraLateral();
            if (p.id().equals(projetoAberto)) {
                projetos.buscar(p.id()).ifPresent(atual -> frame.mostrar(projetoController.abrir(atual)));
            }
        }));
    }

    public void iniciar() {
        Agendador.Situacao situacao = agendador.situacao();
        barraLateral.setAgendador(situacao.rodando(), situacao.texto());
        home();
        frame.setVisible(true);
    }

    public MainFrame frame() {
        return frame;
    }

    public void home() {
        projetoAberto = null;
        barraLateral.selecionar(null);
        barraLateral.setHomeAtivo(true);
        frame.mostrar(new HomePanel(dados));
    }

    @Override
    public void projeto(Projeto projeto) {
        // A linha da barra lateral guarda o registro de quando foi criada; abre sempre a versão atual.
        Projeto atual = projetos.buscar(projeto.id()).orElse(projeto);
        abrirItem(atual);
        projetoAberto = atual.id();
        frame.mostrar(projetoController.abrir(atual));
    }

    @Override
    public void protocolo(Protocolo protocolo) {
        abrirItem(protocolo);
        projetoAberto = null;
        frame.mostrar(new ProtocoloPanel(protocolo, this::novoProtocolo));
    }

    @Override
    public void trilha(Trilha trilha) {
        abrirItem(trilha);
        projetoAberto = null;
        frame.mostrar(new TrilhaPanel(trilha));
    }

    @Override
    public void oraculo() {
        new OraculoDialog(frame, dados.sugestoes(), dados.leituraDoOraculo()).abrir();
    }

    @Override
    public void novoProjeto() {
        projetoController.novoProjeto();
    }

    @Override
    public void novoProtocolo() {
        new NovoProtocoloDialog(frame).abrir();
    }

    @Override
    public void novaTrilha() {
        // Ainda sem diálogo próprio no protótipo.
    }

    /** Pop-up do sino. */
    private void notificacoes() {
        new NotificacoesDialog(frame, notificacoes.listar(), this::abrirNotificacao,
                () -> configuracoes("Notificações")).abrir();
    }

    /** Marca a notificação como lida e abre o projeto dela, se ainda existir. */
    private void abrirNotificacao(Notificacao notificacao) {
        notificacoes.marcarComoLida(notificacao.id());
        if (notificacao.projetoId() != null) {
            projetos.buscar(notificacao.projetoId()).ifPresent(this::projeto);
        }
    }

    public void configuracoes() {
        configuracoes("Geral");
    }

    private void configuracoes(String secao) {
        new ConfiguracoesDialog(frame, repositorioConfiguracoes.carregar(), this::salvarConfiguracoes)
                .naSecao(secao)
                .abrir();
    }

    private void salvarConfiguracoes(Configuracoes configuracoes) {
        try {
            repositorioConfiguracoes.salvar(configuracoes);
        } catch (UncheckedIOException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Configurações", JOptionPane.ERROR_MESSAGE);
        }
        // Mesmo sem conseguir gravar, o que foi escolhido vale para esta sessão.
        agendador.aplicar(configuracoes);
    }

    /** "Sair" da bandeja: para o agendador (e o agente em execução) e a medição de consumo, e fecha a app. */
    private void sair() {
        agendador.encerrar();
        consumo.encerrar();
        frame.bandeja().ifPresent(Bandeja::remover);
        frame.dispose();
        System.exit(0);
    }

    private void alternarAgendador() {
        if (agendador.rodando()) {
            agendador.pausar();
        } else {
            agendador.iniciar();
        }
    }

    /** Uma mudança do usuário pode liberar subtarefas (nova, aprovada, mandada refazer): o agendador olha já. */
    private void projetoAlterado(Projeto projeto) {
        atualizarBarraLateral();
        projeto(projeto);
        agendador.agora();
    }

    private void projetoExcluido() {
        atualizarBarraLateral();
        home();
    }

    private void atualizarBarraLateral() {
        barraLateral.atualizar(projetos.projetos(), dados.protocolos(), dados.trilhas(), dados.sugestoesDoOraculo());
    }

    private void abrirItem(Object item) {
        barraLateral.selecionar(item);
        barraLateral.setHomeAtivo(false);
    }
}
