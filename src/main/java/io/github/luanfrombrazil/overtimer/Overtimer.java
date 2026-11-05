package io.github.luanfrombrazil.overtimer;

import io.github.luanfrombrazil.overtimer.controller.MainController;
import io.github.luanfrombrazil.overtimer.model.Configuracoes;
import io.github.luanfrombrazil.overtimer.service.DadosService;
import io.github.luanfrombrazil.overtimer.service.ProjetoService;
import io.github.luanfrombrazil.overtimer.service.ProtocoloService;
import io.github.luanfrombrazil.overtimer.service.agente.Agentes;
import io.github.luanfrombrazil.overtimer.service.agente.AreaDeTrabalho;
import io.github.luanfrombrazil.overtimer.service.comunicacao.NotificacaoService;
import io.github.luanfrombrazil.overtimer.service.consumo.ConsumoService;
import io.github.luanfrombrazil.overtimer.service.persistence.ConfiguracoesRepositorio;
import io.github.luanfrombrazil.overtimer.service.persistence.NotificacoesRepositorio;
import io.github.luanfrombrazil.overtimer.service.persistence.PastaDados;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProjetos;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProtocolos;
import io.github.luanfrombrazil.overtimer.service.scheduler.Agendador;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.SwingUtilities;
import java.nio.file.Path;

/**
 * Ponto de entrada da aplicação.
 */
public final class Overtimer {

    private Overtimer() {
    }

    static void main() {
        Tema.instalar();
        Path pastaDados = PastaDados.padrao();
        PastaDados.migrarDoNomeAntigo(pastaDados);
        ProjetoService projetos = new ProjetoService(new RepositorioProjetos(pastaDados));
        ProtocoloService protocolos = new ProtocoloService(new RepositorioProtocolos(pastaDados));
        Agendador agendador = new Agendador(projetos, protocolos, Agentes.instalados(), new AreaDeTrabalho(pastaDados));
        Configuracoes configuracoes = new ConfiguracoesRepositorio(pastaDados).carregar();
        NotificacaoService notificacoes = new NotificacaoService(new NotificacoesRepositorio(pastaDados),
                configuracoes.notificacoes());
        agendador.usarNotificacoes(notificacoes);
        agendador.aplicar(configuracoes);

        Runtime.getRuntime().addShutdownHook(new Thread(agendador::encerrar));
        ConsumoService consumo = ConsumoService.padrao(agendador::executando);
        agendador.usarConsumo(consumo::uso);
        SwingUtilities.invokeLater(() -> {
            new MainController(new DadosService(), projetos, protocolos, agendador, consumo, notificacoes).iniciar();
            // Só depois da tela existir, para a primeira medição já chegar ao ouvinte da barra superior.
            consumo.iniciar();
        });
    }
}
