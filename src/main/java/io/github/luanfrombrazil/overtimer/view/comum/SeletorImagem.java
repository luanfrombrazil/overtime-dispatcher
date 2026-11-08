package io.github.luanfrombrazil.overtimer.view.comum;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.util.Optional;

/**
 * Janela do sistema para escolher uma imagem .png ou .jpg.
 */
public final class SeletorImagem {

    private SeletorImagem() {
    }

    /** @return caminho da imagem escolhida, ou vazio se cancelar */
    public static Optional<String> escolher(Component pai) {
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Imagem do projeto");
        seletor.setAcceptAllFileFilterUsed(false);
        seletor.setFileFilter(new FileNameExtensionFilter("Imagens (.png, .jpg)", "png", "jpg", "jpeg"));
        if (seletor.showOpenDialog(pai) != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        return Optional.of(seletor.getSelectedFile().getAbsolutePath());
    }
}
