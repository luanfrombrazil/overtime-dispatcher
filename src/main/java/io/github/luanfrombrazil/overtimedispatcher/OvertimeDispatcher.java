package io.github.luanfrombrazil.overtimedispatcher;

import io.github.luanfrombrazil.overtimedispatcher.controller.MainController;
import io.github.luanfrombrazil.overtimedispatcher.service.DadosService;
import io.github.luanfrombrazil.overtimedispatcher.service.ProjetoService;
import io.github.luanfrombrazil.overtimedispatcher.service.agente.Agentes;
import io.github.luanfrombrazil.overtimedispatcher.service.agente.AreaDeTrabalho;
import io.github.luanfrombrazil.overtimedispatcher.service.persistence.PastaDados;
import io.github.luanfrombrazil.overtimedispatcher.service.persistence.RepositorioProjetos;
import io.github.luanfrombrazil.overtimedispatcher.service.scheduler.Agendador;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.SwingUtilities;
import java.nio.file.Path;

/**
 * Ponto de entrada da aplicação.
 */
public final class OvertimeDispatcher {

    private OvertimeDispatcher() {
    }

    static void main(String[] args) {
        Tema.instalar();
        Path pastaDados = PastaDados.padrao();
        ProjetoService projetos = new ProjetoService(new RepositorioProjetos(pastaDados));
        Agendador agendador = new Agendador(projetos, Agentes.instalados(), new AreaDeTrabalho(pastaDados));
        // Ao fechar a app, o agente em execução é encerrado; a subtarefa volta à fila na próxima abertura.
        Runtime.getRuntime().addShutdownHook(new Thread(agendador::encerrar));
        SwingUtilities.invokeLater(() -> new MainController(new DadosService(), projetos, agendador).iniciar());
    }
}
