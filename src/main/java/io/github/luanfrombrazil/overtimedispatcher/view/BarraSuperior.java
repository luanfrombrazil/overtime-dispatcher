package io.github.luanfrombrazil.overtimedispatcher.view;

import io.github.luanfrombrazil.overtimedispatcher.model.Consumo;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.BarraProgresso;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.BotaoIcone;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Barra do topo: Home, agendador, consumo por provedor, notificações e configurações.
 */
public class BarraSuperior extends JPanel {

    private final BotaoIcone home = new BotaoIcone(Ui.icone("home", 18, Tema.TEXTO), "Home");
    private final BotaoIcone notificacoes = new BotaoIcone(Ui.icone("sino", 18, Tema.TEXTO), "Notificações");
    private final BotaoIcone configuracoes = new BotaoIcone(Ui.icone("engrenagem", 18, Tema.TEXTO), "Configurações");
    private final JButton agendador = Ui.comIcone(Ui.botaoPrimario("Iniciar agendador"), "play");
    private final JLabel estado = Ui.texto("Pausado · intervalo 5 min", Tema.corpo(), Tema.TEXTO_SECUNDARIO);
    private final JPanel consumo = Ui.linha(0);
    private final Map<String, Medida> medidas = new LinkedHashMap<>();

    /** Nome, barra e porcentagem de um provedor; a dica mostra o detalhe (semana, renovação, motivo). */
    private static final class Medida {
        private final JLabel porcentagem = Ui.legenda("—");
        private final BarraProgresso barra = new BarraProgresso(0, Tema.ACENTO, 80);
        private final JLabel nome;
        private final JPanel linha;

        Medida(String provedor) {
            nome = Ui.legenda(provedor);
            linha = Ui.linha(6, nome, barra, porcentagem);
        }

        void mostrar(Consumo c) {
            barra.setPorcentagem(c.conhecido() ? c.porcentagem() : 0);
            porcentagem.setText(c.rotulo());
            String dica = c.detalhe() == null ? c.provedor() + ": " + c.rotulo() : c.provedor() + " · " + c.detalhe();
            linha.setToolTipText(dica);
            nome.setToolTipText(dica);
            barra.setToolTipText(dica);
            porcentagem.setToolTipText(dica);
        }
    }

    public BarraSuperior(List<Consumo> consumo, int naoLidas) {
        super(new BorderLayout());
        setBackground(Tema.PAINEL);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        add(Ui.linha(12, home, agendador, estado), BorderLayout.WEST);

        notificacoes.setContador(naoLidas);
        setConsumo(consumo);
        add(Ui.linha(16, this.consumo, Ui.linha(8, notificacoes, configuracoes)), BorderLayout.EAST);
    }

    /** Atualiza as barras de consumo; um provedor novo ganha a sua barra. */
    public void setConsumo(List<Consumo> consumo) {
        boolean mudouEstrutura = false;
        for (Consumo c : consumo) {
            Medida medida = medidas.get(c.provedor());
            if (medida == null) {
                medida = new Medida(c.provedor());
                medidas.put(c.provedor(), medida);
                mudouEstrutura = true;
            }
            medida.mostrar(c);
        }
        if (mudouEstrutura) {
            this.consumo.removeAll();
            List<JComponent> linhas = new ArrayList<>();
            for (Medida medida : medidas.values()) {
                linhas.add(medida.linha);
            }
            this.consumo.add(Ui.linha(16, linhas.toArray(new JComponent[0])));
        }
        revalidate();
        repaint();
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
