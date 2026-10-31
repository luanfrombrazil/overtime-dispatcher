package io.github.luanfrombrazil.overtimedispatcher.controller;

import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Protocolo;
import io.github.luanfrombrazil.overtimedispatcher.model.Trilha;
import io.github.luanfrombrazil.overtimedispatcher.service.prototipo.DadosPrototipo;
import io.github.luanfrombrazil.overtimedispatcher.view.BarraLateral;
import io.github.luanfrombrazil.overtimedispatcher.view.BarraSuperior;
import io.github.luanfrombrazil.overtimedispatcher.view.MainFrame;
import io.github.luanfrombrazil.overtimedispatcher.view.config.ConfiguracoesDialog;
import io.github.luanfrombrazil.overtimedispatcher.view.home.HomePanel;
import io.github.luanfrombrazil.overtimedispatcher.view.oraculo.OraculoDialog;
import io.github.luanfrombrazil.overtimedispatcher.view.projeto.NovoProjetoDialog;
import io.github.luanfrombrazil.overtimedispatcher.view.projeto.ProjetoPanel;
import io.github.luanfrombrazil.overtimedispatcher.view.protocolo.NovoProtocoloDialog;
import io.github.luanfrombrazil.overtimedispatcher.view.protocolo.ProtocoloPanel;
import io.github.luanfrombrazil.overtimedispatcher.view.trilha.TrilhaPanel;

/**
 * Liga as ações da janela principal: navegação entre telas e abertura dos pop-ups.
 */
public class MainController implements BarraLateral.Ouvinte {

    private final DadosPrototipo dados;
    private final BarraSuperior barraSuperior;
    private final BarraLateral barraLateral;
    private final MainFrame frame;

    public MainController(DadosPrototipo dados) {
        this.dados = dados;
        this.barraSuperior = new BarraSuperior(dados.consumo(), dados.notificacoesNaoLidas());
        this.barraLateral = new BarraLateral(dados.projetos(), dados.protocolos(), dados.trilhas(),
                dados.sugestoesDoOraculo(), this);
        this.frame = new MainFrame(barraSuperior, barraLateral);

        barraSuperior.aoClicarHome(this::home);
        barraSuperior.aoClicarConfiguracoes(this::configuracoes);
        barraSuperior.aoClicarNotificacoes(() -> new ConfiguracoesDialog(frame).naSecao("Notificações").abrir());
    }

    public void iniciar() {
        home();
        frame.setVisible(true);
    }

    public MainFrame frame() {
        return frame;
    }

    public void home() {
        barraLateral.selecionar(null);
        barraSuperior.setHomeAtivo(true);
        frame.mostrar(new HomePanel(dados));
    }

    @Override
    public void projeto(Projeto projeto) {
        abrirItem(projeto);
        frame.mostrar(new ProjetoPanel(projeto, this::novoProjeto, () -> {
        }));
    }

    @Override
    public void protocolo(Protocolo protocolo) {
        abrirItem(protocolo);
        frame.mostrar(new ProtocoloPanel(protocolo, this::novoProtocolo));
    }

    @Override
    public void trilha(Trilha trilha) {
        abrirItem(trilha);
        frame.mostrar(new TrilhaPanel(trilha));
    }

    @Override
    public void oraculo() {
        new OraculoDialog(frame, dados.sugestoes(), dados.leituraDoOraculo()).abrir();
    }

    @Override
    public void novoProjeto() {
        new NovoProjetoDialog(frame).abrir();
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
        new ConfiguracoesDialog(frame).abrir();
    }

    private void abrirItem(Object item) {
        barraLateral.selecionar(item);
        barraSuperior.setHomeAtivo(false);
    }
}
