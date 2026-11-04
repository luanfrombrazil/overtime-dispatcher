package io.github.luanfrombrazil.overtimer;

import io.github.luanfrombrazil.overtimer.controller.MainController;
import io.github.luanfrombrazil.overtimer.service.DadosService;
import io.github.luanfrombrazil.overtimer.service.ProjetoService;
import io.github.luanfrombrazil.overtimer.service.agente.Agentes;
import io.github.luanfrombrazil.overtimer.service.agente.AreaDeTrabalho;
import io.github.luanfrombrazil.overtimer.service.consumo.ConsumoService;
import io.github.luanfrombrazil.overtimer.service.persistence.ConfiguracoesRepositorio;
import io.github.luanfrombrazil.overtimer.service.persistence.PastaDados;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProjetos;
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
        Agendador agendador = new Agendador(projetos, Agentes.instalados(), new AreaDeTrabalho(pastaDados));
        agendador.usarHorario(new ConfiguracoesRepositorio(pastaDados).carregar().horarioFuncionamento());

        Runtime.getRuntime().addShutdownHook(new Thread(agendador::encerrar));
        ConsumoService consumo = ConsumoService.padrao(agendador::executando);
        SwingUtilities.invokeLater(() -> {
            new MainController(new DadosService(), projetos, agendador, consumo).iniciar();
            // Só depois da tela existir, para a primeira medição já chegar ao ouvinte da barra superior.
            consumo.iniciar();
        });
    }
}
