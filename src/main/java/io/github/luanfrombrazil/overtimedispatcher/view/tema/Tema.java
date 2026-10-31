package io.github.luanfrombrazil.overtimedispatcher.view.tema;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import io.github.luanfrombrazil.overtimedispatcher.model.Estado;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

/**
 * Cores e fontes da interface, espelhando as variáveis do Figma.
 */
public final class Tema {

    public static final Color APP = new Color(0x1E1F22);
    public static final Color PAINEL = new Color(0x2B2D30);
    public static final Color SUTIL = new Color(0x313438);
    public static final Color SELECIONADO = new Color(0x2E436E);
    public static final Color LOG = new Color(0x18191B);
    public static final Color BORDA = new Color(0x43454A);
    public static final Color TEXTO = new Color(0xDFE1E5);
    public static final Color TEXTO_SECUNDARIO = new Color(0x9DA0A8);
    public static final Color TEXTO_LOG = new Color(0xBCBEC4);
    public static final Color ACENTO = new Color(0x3574F0);

    public static final Color VERDE = new Color(0x3A8F5C);
    public static final Color AMARELO = new Color(0xB07D12);
    public static final Color VERMELHO = new Color(0xC94F4F);
    public static final Color CINZA = new Color(0x5E6268);
    public static final Color LARANJA = new Color(0xC7712A);

    public static final Color ROXO = new Color(0x8B5CF6);
    public static final Color ROXO_CLARO = new Color(0xC4B5FD);
    public static final Color ROXO_FUNDO = new Color(0x2A2440);
    public static final Color FIO = new Color(0xE5484D);

    public static final Color[] ATIVIDADE = {
            new Color(0x3A3D42), new Color(0x0E4429), new Color(0x006D32), new Color(0x26A641), new Color(0x39D353)
    };

    private Tema() {
    }

    public static void instalar() {
        FlatInterFont.install();
        FlatInterFont.installSemiBold();
        FlatJetBrainsMonoFont.install();
        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
        FlatDarkLaf.setup();

        UIManager.put("Panel.background", APP);
        UIManager.put("Component.arc", 6);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 6);
        UIManager.put("Component.focusWidth", 0);
        UIManager.put("ScrollBar.width", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollPane.border", javax.swing.BorderFactory.createEmptyBorder());
    }

    public static Color cor(Estado estado) {
        return switch (estado) {
            case CONCLUIDO -> VERDE;
            case EM_ANDAMENTO -> ACENTO;
            case ATENCAO -> AMARELO;
            case ERRO -> VERMELHO;
            case PENDENTE -> CINZA;
            case ORACULO -> ROXO;
        };
    }

    public static Color corDetalhe(Estado estado) {
        return switch (estado) {
            case ATENCAO -> AMARELO;
            case ERRO -> VERMELHO;
            case ORACULO -> ROXO_CLARO;
            default -> TEXTO_SECUNDARIO;
        };
    }

    public static Font titulo() {
        return new Font(FlatInterFont.FAMILY_SEMIBOLD, Font.PLAIN, 16);
    }

    public static Font secao() {
        return new Font(FlatInterFont.FAMILY_SEMIBOLD, Font.PLAIN, 13);
    }

    public static Font corpo() {
        return new Font(FlatInterFont.FAMILY, Font.PLAIN, 13);
    }

    public static Font legenda() {
        return new Font(FlatInterFont.FAMILY, Font.PLAIN, 11);
    }

    public static Font numero() {
        return new Font(FlatInterFont.FAMILY_SEMIBOLD, Font.PLAIN, 24);
    }

    public static Font mono() {
        return new Font(FlatJetBrainsMonoFont.FAMILY, Font.PLAIN, 12);
    }
}
