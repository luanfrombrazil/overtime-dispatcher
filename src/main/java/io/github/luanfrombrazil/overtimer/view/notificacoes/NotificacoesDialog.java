package io.github.luanfrombrazil.overtimer.view.notificacoes;

import io.github.luanfrombrazil.overtimer.model.Configuracoes.Aviso;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Notificacao;
import io.github.luanfrombrazil.overtimer.view.comum.BotaoIcone;
import io.github.luanfrombrazil.overtimer.view.comum.Dialogo;
import io.github.luanfrombrazil.overtimer.view.comum.ItemStatus;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lista de notificações, da mais nova para a mais antiga: a do sino e a de "Enquanto você estava fora". Clicar numa
 * a abre; as não lidas têm fundo destacado.
 */
public class NotificacoesDialog extends Dialogo {

    private static final int LARGURA = 520;
    private static final int ALTURA_MAXIMA_DA_LISTA = 460;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_E_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /**
     * @param aoAbrir      ao clicar numa notificação, depois de o pop-up fechar
     * @param aoConfigurar ao clicar em "Configurações", depois de o pop-up fechar
     */
    public NotificacoesDialog(JFrame dono, String titulo, List<Notificacao> notificacoes,
                              Consumer<Notificacao> aoAbrir, Runnable aoConfigurar) {
        super(dono, Tema.BORDA);
        aoClicarFora(this::fechar);
        JPanel raiz = (JPanel) getContentPane();
        raiz.add(cabecalho(titulo, aoConfigurar), BorderLayout.NORTH);

        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setOpaque(false);
        lista.setBorder(BorderFactory.createEmptyBorder(4, 0, 12, 0));
        for (Notificacao n : notificacoes) {
            lista.add(item(n, aoAbrir));
        }
        if (notificacoes.isEmpty()) {
            lista.add(Ui.margem(Ui.cabecalho(Ui.legenda("Nenhuma notificação."), null), 8, 22, 8, 22));
        }
        JScrollPane rolagem = new JScrollPane(lista);
        rolagem.setBorder(null);
        rolagem.setOpaque(false);
        rolagem.getViewport().setOpaque(false);
        rolagem.getVerticalScrollBar().setUnitIncrement(16);
        Dimension preferida = lista.getPreferredSize();
        rolagem.setPreferredSize(new Dimension(LARGURA, Math.min(preferida.height, ALTURA_MAXIMA_DA_LISTA)));
        raiz.add(rolagem, BorderLayout.CENTER);
    }

    private JComponent cabecalho(String titulo, Runnable aoConfigurar) {
        JButton configurar = Ui.botaoSecundario("Configurações");
        configurar.addActionListener(e -> fecharE(aoConfigurar));
        BotaoIcone fechar = new BotaoIcone(Ui.icone("fechar", 14, Tema.TEXTO_SECUNDARIO), "Fechar");
        fechar.addActionListener(e -> fechar());
        JPanel cabecalho = Ui.cabecalho(Ui.titulo(titulo), Ui.linha(8, configurar, fechar));
        cabecalho.setBorder(BorderFactory.createEmptyBorder(16, 22, 12, 16));
        return cabecalho;
    }

    private JComponent item(Notificacao n, Consumer<Notificacao> aoAbrir) {
        ItemStatus item = new ItemStatus(estado(n.tipo()), n.titulo(), n.texto().replace("\n", " · "),
                n.lida() ? null : Tema.SELECIONADO);
        item.comDireita(Ui.legenda(quando(n)));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                fecharE(() -> aoAbrir.accept(n));
            }
        });
        JPanel margem = new JPanel(new BorderLayout());
        margem.setOpaque(false);
        margem.setBorder(BorderFactory.createEmptyBorder(3, 16, 3, 16));
        margem.add(item, BorderLayout.CENTER);
        return Ui.largo(margem);
    }

    /** Fecha e só depois age, para o que abrir (ex.: as Configurações) não ficar preso à espera deste pop-up. */
    private void fecharE(Runnable acao) {
        fechar();
        SwingUtilities.invokeLater(acao);
    }

    /** A cor da borda segue a do estado que gerou o aviso. */
    private static Estado estado(Aviso tipo) {
        return switch (tipo) {
            case VERIFICACAO, DIRECIONAMENTO -> Estado.ATENCAO;
            case FALHAS -> Estado.ERRO;
            case SESSAO_CONCLUIDA -> Estado.CONCLUIDO;
            case MERGE -> Estado.EM_ANDAMENTO;
            case LIMITE_DE_USO -> Estado.PENDENTE;
        };
    }

    private static String quando(Notificacao n) {
        boolean hoje = n.instante().toLocalDate().equals(LocalDate.now());
        return n.instante().format(hoje ? HORA : DIA_E_HORA);
    }
}
