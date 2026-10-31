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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Barra do topo: Home, agendador, consumo por provedor, notificações e configurações.
 */
public class BarraSuperior extends JPanel {

    private static final DateTimeFormatter FORMATO_RESET = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final BotaoIcone home = new BotaoIcone(Ui.icone("home", 18, Tema.TEXTO), "Home");
    private final BotaoIcone notificacoes = new BotaoIcone(Ui.icone("sino", 18, Tema.TEXTO), "Notificações");
    private final BotaoIcone configuracoes = new BotaoIcone(Ui.icone("engrenagem", 18, Tema.TEXTO), "Configurações");
    private final JButton agendador = Ui.comIcone(Ui.botaoPrimario("Iniciar agendador"), "play");
    private final JLabel estado = Ui.texto("Pausado · intervalo 5 min", Tema.corpo(), Tema.TEXTO_SECUNDARIO);

    public BarraSuperior(List<Consumo> consumo, int naoLidas) {
        super(new BorderLayout());
        setBackground(Tema.PAINEL);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        add(Ui.linha(12, home, agendador, estado), BorderLayout.WEST);

        List<javax.swing.JComponent> direita = new ArrayList<>();
        for (Consumo c : consumo) {
            JLabel nome = Ui.legenda(c.provedor());
            BarraProgresso barra = new BarraProgresso(c.porcentagem(), Tema.ACENTO, 80);
            JLabel porcentagem = Ui.legenda(c.porcentagem() + "%");
            JPanel linha = Ui.linha(6, nome, barra, porcentagem);
            String dica = dicaDeReset(c);
            for (javax.swing.JComponent parte : new javax.swing.JComponent[]{linha, nome, barra, porcentagem}) {
                parte.setToolTipText(dica);
            }
            direita.add(linha);
        }
        notificacoes.setContador(naoLidas);
        direita.add(Ui.linha(8, notificacoes, configuracoes));
        add(Ui.linha(16, direita.toArray(new javax.swing.JComponent[0])), BorderLayout.EAST);
    }

    /** Texto mostrado ao passar o mouse sobre a barra de uso: quando os créditos do provedor voltam. */
    private static String dicaDeReset(Consumo c) {
        if (c.reset() == null) {
            return c.provedor() + " · data de reset ainda desconhecida";
        }
        return c.provedor() + " · créditos resetam em " + FORMATO_RESET.format(c.reset());
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

    /** Mostra se o agendador está rodando e o que ele está fazendo. */
    public void setAgendador(boolean rodando, String situacao) {
        agendador.setText(rodando ? "Pausar agendador" : "Iniciar agendador");
        agendador.setIcon(Ui.icone(rodando ? "pausa" : "play", 12, Tema.TEXTO));
        agendador.setMaximumSize(agendador.getPreferredSize());
        estado.setText(situacao.length() > 70 ? situacao.substring(0, 69) + "…" : situacao);
        estado.setToolTipText(situacao);
        revalidate();
        repaint();
    }
}
