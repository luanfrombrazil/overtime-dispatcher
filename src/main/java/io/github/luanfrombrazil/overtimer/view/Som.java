package io.github.luanfrombrazil.overtimer.view;

import java.awt.Toolkit;

/**
 * Som curto das notificações: o som de aviso do Windows ou, se ele não estiver disponível, o bipe do sistema.
 */
public final class Som {

    private static final String SOM_DO_WINDOWS = "win.sound.asterisk";

    private Som() {
    }

    public static void tocar() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (toolkit.getDesktopProperty(SOM_DO_WINDOWS) instanceof Runnable som) {
            som.run();
        } else {
            toolkit.beep();
        }
    }
}
