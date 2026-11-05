package io.github.luanfrombrazil.overtimer.view;

import io.github.luanfrombrazil.overtimer.model.Notificacao;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Frame;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.util.Optional;

/**
 * Ícone da app na bandeja do Windows (D-16): com a janela fechada, a app segue rodando aqui. Duplo clique no ícone
 * ou "Abrir" no menu reabre a janela; "Sair" encerra a app.
 */
public final class Bandeja {

    private final TrayIcon icone;
    private volatile Runnable aoSair = () -> {
    };
    private volatile Runnable aoVoltar = () -> {
    };

    private Bandeja(TrayIcon icone) {
        this.icone = icone;
    }

    /** Põe o ícone na bandeja; vazio quando o sistema não tem bandeja ou não deixou adicionar o ícone. */
    static Optional<Bandeja> instalar(JFrame janela) {
        if (!SystemTray.isSupported()) {
            return Optional.empty();
        }
        SystemTray bandeja = SystemTray.getSystemTray();
        int tamanho = bandeja.getTrayIconSize().height;
        TrayIcon icone = new TrayIcon(Ui.iconeOraculo(true, tamanho).getImage(), janela.getTitle());
        icone.setImageAutoSize(true);
        Bandeja instalada = new Bandeja(icone);
        // No Windows, o ActionEvent do TrayIcon é o duplo clique.
        icone.addActionListener(e -> instalada.reabrir(janela));
        PopupMenu menu = new PopupMenu();
        MenuItem abrir = new MenuItem("Abrir");
        abrir.addActionListener(e -> instalada.reabrir(janela));
        MenuItem sair = new MenuItem("Sair");
        sair.addActionListener(e -> instalada.aoSair.run());
        menu.add(abrir);
        menu.addSeparator();
        menu.add(sair);
        icone.setPopupMenu(menu);
        try {
            bandeja.add(icone);
        } catch (AWTException e) {
            return Optional.empty();
        }
        return Optional.of(instalada);
    }

    /** O ícone em si, para as notificações do desktop. */
    public TrayIcon icone() {
        return icone;
    }

    /**
     * Mostra a notificação do Windows. Clicar nela dispara o mesmo ActionEvent do duplo clique, que reabre a
     * janela.
     */
    public void mostrar(Notificacao notificacao) {
        TrayIcon.MessageType tipo = switch (notificacao.tipo()) {
            case FALHAS -> TrayIcon.MessageType.ERROR;
            case LIMITE_DE_USO -> TrayIcon.MessageType.WARNING;
            default -> TrayIcon.MessageType.INFO;
        };
        icone.displayMessage(notificacao.titulo(), notificacao.texto(), tipo);
    }

    /** O que o "Sair" do menu faz: encerrar a app. */
    public void aoSair(Runnable acao) {
        this.aoSair = acao;
    }

    /** Tira o ícone da bandeja (ao encerrar a app). */
    public void remover() {
        SystemTray.getSystemTray().remove(icone);
    }

    /** O que fazer quando a janela, que estava escondida na bandeja, volta a aparecer. */
    public void aoVoltar(Runnable acao) {
        this.aoVoltar = acao;
    }

    private void reabrir(JFrame janela) {
        boolean estavaEscondida = !janela.isVisible();
        janela.setExtendedState(janela.getExtendedState() & ~Frame.ICONIFIED);
        janela.setVisible(true);
        janela.toFront();
        janela.requestFocus();
        if (estavaEscondida) {
            // Depois de a janela aparecer, para o que for aberto ficar sobre ela.
            SwingUtilities.invokeLater(aoVoltar);
        }
    }
}
