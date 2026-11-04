package io.github.luanfrombrazil.overtimer.view.config;

import io.github.luanfrombrazil.overtimer.model.Configuracoes;
import io.github.luanfrombrazil.overtimer.model.HorarioFuncionamento;
import io.github.luanfrombrazil.overtimer.view.comum.Chip;
import io.github.luanfrombrazil.overtimer.view.comum.Dialogo;
import io.github.luanfrombrazil.overtimer.view.comum.Interruptor;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Configurações, com navegação lateral. Agendamento e Notificações já prototipadas.
 */
public class ConfiguracoesDialog extends Dialogo {

    private static final List<String> SECOES =
            List.of("Geral", "Agendamento", "Provedores", "Notificações", "Projetos padrão", "Avançado");

    /** Valores iniciais da reserva de cada provedor: 0% para automação e 5% para o Oráculo. */
    private static final int AUTOMACAO_PADRAO = 0;
    private static final int ORACULO_PADRAO = 5;

    private final CardLayout cartoes = new CardLayout();
    private final JPanel paginas = new JPanel(cartoes);
    private final Map<String, JLabel> itensNavegacao = new LinkedHashMap<>();
    private final Configuracoes configuracoes;
    private final Consumer<Configuracoes> aoSalvar;
    private BarrasHorario barras;

    /** Mostra as configurações recebidas; "Salvar" entrega a versão editada para {@code aoSalvar}. */
    public ConfiguracoesDialog(JFrame dono, Configuracoes configuracoes, Consumer<Configuracoes> aoSalvar) {
        super(dono, Tema.BORDA);
        this.configuracoes = configuracoes;
        this.aoSalvar = aoSalvar;
        JPanel raiz = (JPanel) getContentPane();
        raiz.setPreferredSize(new Dimension(820, 740));

        raiz.add(navegacao(), BorderLayout.WEST);

        paginas.setOpaque(false);
        for (String secao : SECOES) {
            JComponent pagina = switch (secao) {
                case "Agendamento" -> agendamento();
                case "Notificações" -> notificacoes();
                default -> Ui.margem(Ui.coluna(0, Ui.legenda("Em construção.")), 20, 24, 20, 24);
            };
            JPanel envoltorio = new JPanel(new BorderLayout());
            envoltorio.setOpaque(false);
            envoltorio.add(pagina, BorderLayout.NORTH);
            paginas.add(envoltorio, secao);
        }

        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.setBackground(Tema.PAINEL);
        conteudo.add(paginas, BorderLayout.CENTER);
        conteudo.add(rodape(), BorderLayout.SOUTH);
        raiz.add(conteudo, BorderLayout.CENTER);

        mostrar("Agendamento");
    }

    /** Abre numa seção específica (ex.: "Notificações"). */
    public ConfiguracoesDialog naSecao(String secao) {
        mostrar(secao);
        return this;
    }

    private JComponent navegacao() {
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(Tema.APP);
        nav.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));
        nav.setPreferredSize(new Dimension(180, 10));
        nav.add(Ui.margem(Ui.titulo("Configurações"), 0, 8, 12, 0));
        for (String secao : SECOES) {
            JLabel item = Ui.texto(secao, Tema.corpo(), Tema.TEXTO_SECUNDARIO);
            item.setOpaque(true);
            item.setBackground(Tema.APP);
            item.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    mostrar(secao);
                }
            });
            Ui.largo(item);
            itensNavegacao.put(secao, item);
            nav.add(item);
            nav.add(Box.createVerticalStrut(2));
        }
        return nav;
    }

    private void mostrar(String secao) {
        cartoes.show(paginas, secao);
        itensNavegacao.forEach((nome, item) -> {
            boolean ativo = nome.equals(secao);
            item.setBackground(ativo ? Tema.SELECIONADO : Tema.APP);
            item.setForeground(ativo ? Tema.TEXTO : Tema.TEXTO_SECUNDARIO);
        });
    }

    private JComponent rodape() {
        JButton cancelar = Ui.botaoSecundario("Cancelar");
        JButton salvar = Ui.botaoPrimario("Salvar");
        cancelar.addActionListener(e -> fechar());
        salvar.addActionListener(e -> {
            aoSalvar.accept(new Configuracoes(barras.horario()));
            fechar();
        });
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rodape.setOpaque(false);
        rodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(14, 24, 14, 16)));
        rodape.add(cancelar);
        rodape.add(salvar);
        return rodape;
    }

    // ---------- Agendamento ----------

    private JComponent agendamento() {
        JPanel pagina = pagina();

        pagina.add(Ui.cabecalho(Ui.secao("Horário de funcionamento"), null));
        pagina.add(Box.createVerticalStrut(10));
        HorarioFuncionamento atual = configuracoes.horarioFuncionamento();
        barras = new BarrasHorario(atual);
        Chip sempre = new Chip("Sempre", atual.equals(HorarioFuncionamento.sempre()));
        Chip noites = new Chip("Noites e fins de semana", atual.equals(HorarioFuncionamento.noitesEFinsDeSemana()));
        Chip personalizado = new Chip("Personalizado", !sempre.isSelecionado() && !noites.isSelecionado());
        List<Chip> presets = List.of(sempre, noites, personalizado);
        Runnable selecionarPersonalizado = () -> presets.forEach(c -> c.setSelecionado(c == personalizado));
        sempre.aoClicar(() -> {
            barras.mostrar(HorarioFuncionamento.sempre());
            presets.forEach(c -> c.setSelecionado(c == sempre));
        });
        noites.aoClicar(() -> {
            barras.mostrar(HorarioFuncionamento.noitesEFinsDeSemana());
            presets.forEach(c -> c.setSelecionado(c == noites));
        });
        personalizado.aoClicar(selecionarPersonalizado);
        barras.aoEditar(selecionarPersonalizado);
        pagina.add(Ui.cabecalho(Ui.linha(8, sempre, noites, personalizado), null));
        pagina.add(Box.createVerticalStrut(12));
        pagina.add(Ui.cabecalho(barras, null));

        pagina.add(Box.createVerticalStrut(20));
        pagina.add(Ui.cabecalho(Ui.secao("Reserva para você"), null));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(reserva("Claude Code", AUTOMACAO_PADRAO, ORACULO_PADRAO));
        pagina.add(Box.createVerticalStrut(6));
        pagina.add(reserva("Codex", AUTOMACAO_PADRAO, ORACULO_PADRAO));
        pagina.add(Box.createVerticalStrut(6));
        pagina.add(reserva("Gemini", AUTOMACAO_PADRAO, ORACULO_PADRAO));

        pagina.add(Box.createVerticalStrut(20));
        pagina.add(Ui.cabecalho(Ui.secao("Execução"), null));
        pagina.add(Box.createVerticalStrut(8));
        JComboBox<String> tentativas = lista("1", "2", "3", "5");
        tentativas.setSelectedItem("3");
        JComboBox<String> timeout = lista("15 min", "30 min", "60 min");
        timeout.setSelectedItem("30 min");
        pagina.add(Ui.cabecalho(Ui.linha(24,
                campo("Intervalo", lista("5 min", "10 min", "15 min", "30 min")),
                campo("Tentativas", tentativas),
                campo("Timeout", timeout)), null));
        return pagina;
    }

    private static JComponent reserva(String provedor, int automacao, int oraculo) {
        ReservaSlider slider = new ReservaSlider(automacao, oraculo);
        JLabel valorOraculo = Ui.texto("", Tema.corpo(), Tema.ROXO_CLARO);
        JLabel valorVoce = Ui.corpo("");
        slider.aoMudar((o, v) -> {
            valorOraculo.setText(o + "%");
            valorVoce.setText(v + "%");
        });
        return Ui.cabecalho(Ui.linha(12, largura(Ui.corpo(provedor), 100), largura(slider, 300),
                largura(valorOraculo, 40), largura(valorVoce, 40)), null);
    }

    private static <T extends JComponent> T largura(T componente, int largura) {
        Dimension d = new Dimension(largura, componente.getPreferredSize().height);
        componente.setPreferredSize(d);
        componente.setMinimumSize(d);
        componente.setMaximumSize(d);
        return componente;
    }

    // ---------- Notificações ----------

    private JComponent notificacoes() {
        JPanel pagina = pagina();
        pagina.add(Ui.cabecalho(Ui.secao("Modo silencioso"), null));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("Silenciar agora", false));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("Silenciar automaticamente", true));
        pagina.add(Box.createVerticalStrut(6));
        pagina.add(Ui.cabecalho(Ui.linha(8, Ui.legenda("Das"), lista("23:00", "22:00", "00:00"),
                Ui.legenda("às"), lista("07:00", "06:00", "08:00")), null));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("Falhas furam o silêncio", true));

        pagina.add(Box.createVerticalStrut(20));
        pagina.add(Ui.cabecalho(Ui.secao("Canais"), null));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("Desktop (Windows)", true));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("WhatsApp", false));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("Som", false));

        pagina.add(Box.createVerticalStrut(20));
        pagina.add(Ui.cabecalho(Ui.secao("Avisar sobre"), null));
        pagina.add(Box.createVerticalStrut(8));
        JPanel tipos = new JPanel(new java.awt.GridLayout(0, 4, 16, 4));
        tipos.setOpaque(false);
        Map<String, Boolean> avisos = new LinkedHashMap<>();
        avisos.put("Verificação", true);
        avisos.put("Direcionamento", true);
        avisos.put("Merge", true);
        avisos.put("Falhas", true);
        avisos.put("Limite de uso", false);
        avisos.put("Sessão concluída", false);
        avisos.forEach((nome, marcado) -> {
            JCheckBox caixa = new JCheckBox(nome, marcado);
            caixa.setFont(Tema.corpo());
            caixa.setOpaque(false);
            tipos.add(caixa);
        });
        pagina.add(Ui.largo(tipos));
        return pagina;
    }

    // ---------- Auxiliares ----------

    private static JPanel pagina() {
        JPanel pagina = new JPanel();
        pagina.setLayout(new BoxLayout(pagina, BoxLayout.Y_AXIS));
        pagina.setOpaque(false);
        pagina.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        return pagina;
    }

    private static JComponent opcao(String texto, boolean ligado) {
        return Ui.cabecalho(Ui.corpo(texto), new Interruptor(ligado));
    }

    private static JComponent campo(String rotulo, JComponent entrada) {
        return Ui.coluna(4, Ui.legenda(rotulo), entrada);
    }

    private static JComboBox<String> lista(String... opcoes) {
        JComboBox<String> lista = new JComboBox<>(opcoes);
        lista.setFont(Tema.corpo());
        lista.setPreferredSize(new Dimension(110, 30));
        lista.setMaximumSize(lista.getPreferredSize());
        return lista;
    }
}
