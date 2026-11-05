package io.github.luanfrombrazil.overtimer.view;

import io.github.luanfrombrazil.overtimer.model.Consumo;
import io.github.luanfrombrazil.overtimer.view.comum.BarraProgresso;
import io.github.luanfrombrazil.overtimer.view.comum.BotaoIcone;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Barra do topo, à direita da barra lateral: consumo por provedor, notificações e configurações.
 */
public class BarraSuperior extends JPanel {

    /** Barra de consumo 50% maior que a padrão (80 × 6). */
    private static final int LARGURA_CONSUMO = 120;
    private static final int ALTURA_CONSUMO = 9;
    /** Espaço em branco entre as barras de cada provedor. */
    private static final int ESPACO_CONSUMO = 28;

    private final BotaoIcone notificacoes = new BotaoIcone(Ui.icone("sino", 18, Tema.TEXTO), "Notificações");
    private final BotaoIcone configuracoes = new BotaoIcone(Ui.icone("engrenagem", 18, Tema.TEXTO), "Configurações");
    private final JPanel consumo = Ui.linha(0);
    private final Map<String, Medida> medidas = new LinkedHashMap<>();

    /**
     * Nome, barra e porcentagens de um provedor. O uso da sessão (acento) fica sobreposto ao uso semanal (tom
     * claro) na mesma barra; a dica mostra o detalhe (renovação ou motivo de não medir).
     */
    private static final class Medida {
        private final JLabel porcentagem = Ui.legenda("—");
        private final BarraProgresso barra = new BarraProgresso(0, Tema.ACENTO, 0, Tema.ACENTO_CLARO,
                LARGURA_CONSUMO, ALTURA_CONSUMO);
        private final JLabel nome;
        private final JPanel linha;

        Medida(String provedor) {
            nome = Ui.legenda(provedor);
            linha = Ui.linha(6, nome, barra, porcentagem);
        }

        void mostrar(Consumo c) {
            barra.setPorcentagens(Math.max(0, c.sessao()), Math.max(0, c.semanal()));
            porcentagem.setText(c.rotulo());
            // Sem medida, o "—" pareceria um separador entre as barras; o motivo continua na dica.
            porcentagem.setVisible(c.conhecido());
            String dica = c.detalhe() == null
                    ? c.provedor() + " · sessão " + texto(c.sessao()) + " · semana " + texto(c.semanal())
                    : c.provedor() + " · " + c.detalhe();
            linha.setToolTipText(dica);
            nome.setToolTipText(dica);
            barra.setToolTipText(dica);
            porcentagem.setToolTipText(dica);
        }

        private static String texto(int porcentagem) {
            return porcentagem == Consumo.DESCONHECIDO ? "—" : porcentagem + "%";
        }
    }

    public BarraSuperior(List<Consumo> consumo, int naoLidas) {
        super(new BorderLayout());
        setBackground(Tema.PAINEL);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        setNaoLidas(naoLidas);
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
            this.consumo.add(Ui.linha(ESPACO_CONSUMO, linhas.toArray(new JComponent[0])));
        }
        revalidate();
        repaint();
    }

    /** Número no sino: as notificações não lidas. */
    public void setNaoLidas(int naoLidas) {
        notificacoes.setContador(naoLidas);
    }

    public void aoClicarNotificacoes(Runnable acao) {
        notificacoes.addActionListener(e -> acao.run());
    }

    public void aoClicarConfiguracoes(Runnable acao) {
        configuracoes.addActionListener(e -> acao.run());
    }
}
