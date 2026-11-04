package io.github.luanfrombrazil.overtimedispatcher.view;

import io.github.luanfrombrazil.overtimedispatcher.model.Consumo;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.BarraProgresso;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.BotaoIcone;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.IndicadorAgendador;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Barra do topo: Home, agendador, consumo por provedor, notificações e configurações.
 */
public class BarraSuperior extends JPanel {

    /** Barra de consumo 50% maior que a padrão (80 × 6). */
    private static final int LARGURA_CONSUMO = 120;
    private static final int ALTURA_CONSUMO = 9;

    private final BotaoIcone home = new BotaoIcone(Ui.icone("home", 18, Tema.TEXTO), "Home");
    private final BotaoIcone notificacoes = new BotaoIcone(Ui.icone("sino", 18, Tema.TEXTO), "Notificações");
    private final BotaoIcone configuracoes = new BotaoIcone(Ui.icone("engrenagem", 18, Tema.TEXTO), "Configurações");
    private final JButton agendador = Ui.comIcone(Ui.botaoPrimario("Iniciar agendador"), "play");
    private final IndicadorAgendador estado = new IndicadorAgendador();

    public BarraSuperior(List<Consumo> consumo, int naoLidas) {
        super(new BorderLayout());
        setBackground(Tema.PAINEL);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        add(Ui.linha(12, home, agendador, estado), BorderLayout.WEST);

        List<javax.swing.JComponent> direita = new ArrayList<>();
        for (Consumo c : consumo) {
            // Uso da sessão (acento) sobreposto ao uso semanal (tom claro) na mesma barra.
            BarraProgresso barra = new BarraProgresso(c.sessao(), Tema.ACENTO, c.semanal(), Tema.ACENTO_CLARO,
                    LARGURA_CONSUMO, ALTURA_CONSUMO);
            javax.swing.JComponent item = Ui.linha(6, Ui.legenda(c.provedor()), barra,
                    Ui.legenda(c.sessao() + "% · " + c.semanal() + "%"));
            String dica = c.provedor() + " · sessão " + c.sessao() + "% · semana " + c.semanal() + "%";
            item.setToolTipText(dica);
            barra.setToolTipText(dica);
            direita.add(item);
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

    public void aoClicarAgendador(Runnable acao) {
        agendador.addActionListener(e -> acao.run());
    }

    /**
     * Mostra se o agendador está rodando: o símbolo de pausa ou o círculo girando. O que ele está fazendo só
     * aparece ao passar o mouse.
     */
    public void setAgendador(boolean rodando, String situacao) {
        agendador.setText(rodando ? "Pausar agendador" : "Iniciar agendador");
        agendador.setIcon(Ui.icone(rodando ? "pausa" : "play", 12, Tema.TEXTO));
        agendador.setMaximumSize(agendador.getPreferredSize());
        estado.setEstado(rodando, situacao);
        revalidate();
        repaint();
    }
}
