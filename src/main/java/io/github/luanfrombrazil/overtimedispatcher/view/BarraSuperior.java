package io.github.luanfrombrazil.overtimedispatcher.view;

import io.github.luanfrombrazil.overtimedispatcher.model.Consumo;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.BarraProgresso;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.BotaoIcone;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Barra do topo: Home, agendador, consumo por provedor, notificações e configurações.
 */
public class BarraSuperior extends JPanel {

    private final BotaoIcone home = new BotaoIcone(Ui.icone("home", 18, Tema.TEXTO), "Home");
    private final BotaoIcone notificacoes = new BotaoIcone(Ui.icone("sino", 18, Tema.TEXTO), "Notificações");
    private final BotaoIcone configuracoes = new BotaoIcone(Ui.icone("engrenagem", 18, Tema.TEXTO), "Configurações");
    private final JButton agendador = Ui.comIcone(Ui.botaoPrimario("Iniciar agendador"), "play");
    private final JLabel estado = Ui.texto("Pausado · intervalo 5 min", Tema.corpo(), Tema.TEXTO_SECUNDARIO);
    private boolean rodando;

    public BarraSuperior(List<Consumo> consumo, int naoLidas) {
        super(new BorderLayout());
        setBackground(Tema.PAINEL);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        agendador.addActionListener(e -> alternarAgendador());
        add(Ui.linha(12, home, agendador, estado), BorderLayout.WEST);

        List<javax.swing.JComponent> direita = new ArrayList<>();
        for (Consumo c : consumo) {
            direita.add(Ui.linha(6, Ui.legenda(c.provedor()), new BarraProgresso(c.porcentagem(), Tema.ACENTO, 80),
                    Ui.legenda(c.porcentagem() + "%")));
        }
        notificacoes.setContador(naoLidas);
        direita.add(Ui.linha(8, notificacoes, configuracoes));
        add(Ui.linha(16, direita.toArray(new javax.swing.JComponent[0])), BorderLayout.EAST);
    }

    public void aoClicarHome(Runnable acao) {
        home.addActionListener(e -> acao.run());
    }

    public void aoClicarNotificacoes(Runnable acao) {
        notificacoes.addActionListener(e -> acao.run());
    }

    public void aoClicarConfiguracoes(Runnable acao) {
        configuracoes.addActionListener(e -> acao.run());
    }

    public void setHomeAtivo(boolean ativo) {
        home.setAtivo(ativo);
    }

    private void alternarAgendador() {
        rodando = !rodando;
        agendador.setText(rodando ? "Pausar agendador" : "Iniciar agendador");
        agendador.setIcon(Ui.icone(rodando ? "pausa" : "play", 12, Tema.TEXTO));
        estado.setText(rodando ? "Rodando · próximo ciclo em 5 min" : "Pausado · intervalo 5 min");
        agendador.setMaximumSize(agendador.getPreferredSize());
        revalidate();
    }
}
