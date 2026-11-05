package io.github.luanfrombrazil.overtimer.view;

import io.github.luanfrombrazil.overtimer.view.comum.Ui;

import javax.swing.JFrame;
import java.awt.AWTException;
import java.awt.Frame;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.util.Optional;

/**
 * Ícone da app na bandeja do Windows (D-16): com a janela fechada, a app segue rodando aqui. Duplo clique no ícone
 * reabre a janela.
 */
public final class Bandeja {

    private final TrayIcon icone;

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
        // No Windows, o ActionEvent do TrayIcon é o duplo clique.
        icone.addActionListener(e -> reabrir(janela));
        try {
            bandeja.add(icone);
        } catch (AWTException e) {
            return Optional.empty();
        }
        return Optional.of(new Bandeja(icone));
    }

    /** O ícone em si, para as notificações do desktop. */
    public TrayIcon icone() {
        return icone;
    }

    private static void reabrir(JFrame janela) {
        janela.setExtendedState(janela.getExtendedState() & ~Frame.ICONIFIED);
        janela.setVisible(true);
        janela.toFront();
        janela.requestFocus();
    }
}
