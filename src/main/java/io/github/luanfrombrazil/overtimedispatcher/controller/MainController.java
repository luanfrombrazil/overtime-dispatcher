package io.github.luanfrombrazil.overtimedispatcher.controller;

import io.github.luanfrombrazil.overtimedispatcher.model.Configuracoes;
import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Protocolo;
import io.github.luanfrombrazil.overtimedispatcher.model.Trilha;
import io.github.luanfrombrazil.overtimedispatcher.service.DadosService;
import io.github.luanfrombrazil.overtimedispatcher.service.ProjetoService;
import io.github.luanfrombrazil.overtimedispatcher.service.consumo.ConsumoService;
import io.github.luanfrombrazil.overtimedispatcher.service.persistence.ConfiguracoesRepositorio;
import io.github.luanfrombrazil.overtimedispatcher.service.scheduler.Agendador;
import io.github.luanfrombrazil.overtimedispatcher.view.BarraLateral;
import io.github.luanfrombrazil.overtimedispatcher.view.BarraSuperior;
import io.github.luanfrombrazil.overtimedispatcher.view.MainFrame;
import io.github.luanfrombrazil.overtimedispatcher.view.config.ConfiguracoesDialog;
import io.github.luanfrombrazil.overtimedispatcher.view.home.HomePanel;
import io.github.luanfrombrazil.overtimedispatcher.view.oraculo.OraculoDialog;
import io.github.luanfrombrazil.overtimedispatcher.view.protocolo.NovoProtocoloDialog;
import io.github.luanfrombrazil.overtimedispatcher.view.protocolo.ProtocoloPanel;
import io.github.luanfrombrazil.overtimedispatcher.view.trilha.TrilhaPanel;

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
    private final ConfiguracoesRepositorio repositorioConfiguracoes = new ConfiguracoesRepositorio();
    private final BarraSuperior barraSuperior;
    private final BarraLateral barraLateral;
    private final MainFrame frame;
    private final ProjetoController projetoController;
    private String projetoAberto;

    public MainController(DadosService dados, ProjetoService projetos, Agendador agendador, ConsumoService consumo) {
        this.dados = dados;
        this.projetos = projetos;
        this.agendador = agendador;
        this.barraSuperior = new BarraSuperior(consumo.consumo(), dados.notificacoesNaoLidas());
        this.barraLateral = new BarraLateral(projetos.projetos(), dados.protocolos(), dados.trilhas(),
                dados.sugestoesDoOraculo(), this);
        this.frame = new MainFrame(barraSuperior, barraLateral);
        this.projetoController = new ProjetoController(frame, projetos, agendador::log,
                this::projetoAlterado, this::projetoExcluido);

        barraSuperior.aoClicarHome(this::home);
        barraSuperior.aoClicarAgendador(this::alternarAgendador);
        barraSuperior.aoClicarConfiguracoes(this::configuracoes);
        barraSuperior.aoClicarNotificacoes(() -> configuracoes("Notificações"));

        // O agendador e o serviço avisam na thread do agendador; a tela só muda na thread do Swing.
        agendador.aoMudar(new Agendador.Ouvinte() {
            @Override
            public void situacao(Agendador.Situacao situacao) {
                SwingUtilities.invokeLater(() -> barraSuperior.setAgendador(situacao.rodando(), situacao.texto()));
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
        consumo.aoMudar(medido -> SwingUtilities.invokeLater(() -> barraSuperior.setConsumo(medido)));
        projetos.aoMudar(p -> SwingUtilities.invokeLater(() -> {
            atualizarBarraLateral();
            if (p.id().equals(projetoAberto)) {
                projetos.buscar(p.id()).ifPresent(atual -> frame.mostrar(projetoController.abrir(atual)));
            }
        }));
    }

    public void iniciar() {
        Agendador.Situacao situacao = agendador.situacao();
        barraSuperior.setAgendador(situacao.rodando(), situacao.texto());
        home();
        frame.setVisible(true);
    }

    public MainFrame frame() {
        return frame;
    }

    public void home() {
        projetoAberto = null;
        barraLateral.selecionar(null);
        barraSuperior.setHomeAtivo(true);
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

    public void configuracoes() {
        configuracoes("Agendamento");
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
        barraSuperior.setHomeAtivo(false);
    }
}
