package io.github.luanfrombrazil.overtimer.view.config;

import io.github.luanfrombrazil.overtimer.model.Configuracoes;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Aviso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Execucao;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.LimiteDeUso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Notificacoes;
import io.github.luanfrombrazil.overtimer.model.HorarioFuncionamento;
import io.github.luanfrombrazil.overtimer.model.Provedor;
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
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Configurações, com navegação lateral. Geral, Modelos e Notificações já prototipadas.
 */
public class ConfiguracoesDialog extends Dialogo {

    private static final List<String> SECOES =
            List.of("Geral", "Modelos", "Notificações", "Projetos padrão", "Avançado");

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CardLayout cartoes = new CardLayout();
    private final JPanel paginas = new JPanel(cartoes);
    private final Map<String, JLabel> itensNavegacao = new LinkedHashMap<>();
    private final Configuracoes configuracoes;
    private final Consumer<Configuracoes> aoSalvar;

    // Controles editáveis, lidos por editadas() ao salvar.
    private BarrasHorario barras;
    private final Map<Provedor, ReservaSlider> sliders = new EnumMap<>(Provedor.class);
    private JComboBox<String> intervalo;
    private JSpinner maximoPorIntervalo;
    private JComboBox<String> tentativas;
    private JComboBox<String> timeout;
    private Interruptor silenciarAgora;
    private Interruptor silenciarAutomaticamente;
    private JComboBox<String> silencioInicio;
    private JComboBox<String> silencioFim;
    private Interruptor falhasFuramSilencio;
    private Interruptor desktop;
    private Interruptor whatsapp;
    private Interruptor som;
    private final Map<Aviso, JCheckBox> avisos = new EnumMap<>(Aviso.class);
    private final ModelosPagina modelos;
    private final JButton restaurarPadrao = Ui.botaoSecundario("Restaurar padrão");

    /** Mostra as configurações recebidas; "Salvar" entrega a versão editada para {@code aoSalvar}. */
    public ConfiguracoesDialog(JFrame dono, Configuracoes configuracoes, Consumer<Configuracoes> aoSalvar) {
        super(dono, Tema.BORDA);
        this.configuracoes = configuracoes;
        this.aoSalvar = aoSalvar;
        modelos = new ModelosPagina(configuracoes.prompts());
        JPanel raiz = (JPanel) getContentPane();
        raiz.setPreferredSize(new Dimension(1120, 740));

        raiz.add(navegacao(), BorderLayout.WEST);

        paginas.setOpaque(false);
        for (String secao : SECOES) {
            JComponent pagina = switch (secao) {
                case "Geral" -> geral();
                case "Modelos" -> modelos;
                case "Notificações" -> notificacoes();
                default -> Ui.margem(Ui.coluna(0, Ui.legenda("Em construção.")), 20, 24, 20, 24);
            };
            JPanel envoltorio = new JPanel(new BorderLayout());
            envoltorio.setOpaque(false);
            envoltorio.add(pagina, BorderLayout.NORTH);
            // Seções longas (ex.: Modelos) rolam dentro do pop-up, que mantém o tamanho.
            JScrollPane rolagem = new JScrollPane(envoltorio,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            rolagem.setBorder(BorderFactory.createEmptyBorder());
            rolagem.setOpaque(false);
            rolagem.getViewport().setOpaque(false);
            rolagem.getVerticalScrollBar().setUnitIncrement(16);
            paginas.add(rolagem, secao);
        }

        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.setBackground(Tema.PAINEL);
        conteudo.add(paginas, BorderLayout.CENTER);
        conteudo.add(rodape(), BorderLayout.SOUTH);
        raiz.add(conteudo, BorderLayout.CENTER);

        mostrar("Geral");
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
        restaurarPadrao.setVisible(secao.equals("Modelos"));
    }

    private JComponent rodape() {
        JButton cancelar = Ui.botaoSecundario("Cancelar");
        JButton salvar = Ui.botaoPrimario("Salvar");
        cancelar.addActionListener(e -> fechar());
        salvar.addActionListener(e -> {
            aoSalvar.accept(editadas());
            fechar();
        });
        restaurarPadrao.addActionListener(e -> modelos.restaurarPadrao());
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        botoes.add(cancelar);
        botoes.add(salvar);
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(14, 24, 14, 16)));
        rodape.add(restaurarPadrao, BorderLayout.WEST);
        rodape.add(botoes, BorderLayout.EAST);
        return rodape;
    }

    /** As configurações como estão na tela. */
    private Configuracoes editadas() {
        Map<Provedor, LimiteDeUso> limites = new EnumMap<>(Provedor.class);
        sliders.forEach((provedor, slider) ->
                limites.put(provedor, new LimiteDeUso(slider.automacao(), slider.oraculo())));
        try {
            // Um número digitado sem Enter ainda não foi para o modelo.
            maximoPorIntervalo.commitEdit();
        } catch (ParseException e) {
            // Texto inválido: fica o último valor aceito.
        }
        Execucao execucao = new Execucao(numero(intervalo), numero(tentativas), numero(timeout),
                (Integer) maximoPorIntervalo.getValue());
        Set<Aviso> marcados = EnumSet.noneOf(Aviso.class);
        avisos.forEach((aviso, caixa) -> {
            if (caixa.isSelected()) {
                marcados.add(aviso);
            }
        });
        Notificacoes notificacoes = new Notificacoes(silenciarAgora.isLigado(), silenciarAutomaticamente.isLigado(),
                LocalTime.parse((String) silencioInicio.getSelectedItem()),
                LocalTime.parse((String) silencioFim.getSelectedItem()), falhasFuramSilencio.isLigado(),
                desktop.isLigado(), whatsapp.isLigado(), som.isLigado(), marcados);
        return new Configuracoes(barras.horario(), limites, execucao, notificacoes, modelos.prompts());
    }

    // ---------- Geral ----------

    private JComponent geral() {
        JPanel pagina = pagina();

        pagina.add(Ui.cabecalho(Ui.secao("Horário de funcionamento"), null));
        pagina.add(Box.createVerticalStrut(4));
        pagina.add(Ui.cabecalho(Ui.legenda("O agendador só começa execuções nos horários marcados. Clique e arraste "
                + "para marcar ou desmarcar."), null));
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
        pagina.add(Ui.cabecalho(Ui.secao("Limite de uso"), null));
        pagina.add(Box.createVerticalStrut(4));
        pagina.add(Ui.cabecalho(Ui.legenda("O agendador não começa Tasks num provedor cujo uso (sessão ou semanal) "
                + "chegou ao limite azul. Roxo: Oráculo; o resto fica para você."), null));
        pagina.add(Box.createVerticalStrut(8));
        for (int i = 0; i < Configuracoes.PROVEDORES_COM_LIMITE.size(); i++) {
            if (i > 0) {
                pagina.add(Box.createVerticalStrut(6));
            }
            pagina.add(limiteDeUso(Configuracoes.PROVEDORES_COM_LIMITE.get(i)));
        }

        pagina.add(Box.createVerticalStrut(20));
        pagina.add(Ui.cabecalho(Ui.secao("Execução"), null));
        pagina.add(Box.createVerticalStrut(8));
        Execucao execucao = configuracoes.execucao();
        intervalo = lista(execucao.intervaloMinutos() + " min", "5 min", "10 min", "15 min", "30 min", "60 min");
        maximoPorIntervalo = new JSpinner(new SpinnerNumberModel(execucao.maximoPorIntervalo(), 0, 999, 1));
        maximoPorIntervalo.setFont(Tema.corpo());
        maximoPorIntervalo.setPreferredSize(new Dimension(110, 30));
        maximoPorIntervalo.setMaximumSize(maximoPorIntervalo.getPreferredSize());
        tentativas = lista(String.valueOf(execucao.tentativas()), "1", "2", "3", "5");
        timeout = lista(execucao.timeoutMinutos() + " min", "15 min", "30 min", "60 min");
        pagina.add(Ui.cabecalho(Ui.linha(24,
                campo("Intervalo", intervalo),
                campo("Máx. por intervalo", maximoPorIntervalo),
                campo("Tentativas", tentativas),
                campo("Timeout", timeout)), null));
        pagina.add(Box.createVerticalStrut(6));
        pagina.add(Ui.cabecalho(Ui.legenda("Máximo de execuções que o agendador começa em cada intervalo; "
                + "0 para não limitar."), null));
        return pagina;
    }

    private JComponent limiteDeUso(Provedor provedor) {
        LimiteDeUso atual = configuracoes.limiteDeUso(provedor);
        ReservaSlider slider = new ReservaSlider(atual.automacao(), atual.oraculo());
        sliders.put(provedor, slider);
        JLabel valorAutomacao = Ui.texto("", Tema.corpo(), Tema.ACENTO);
        JLabel valorOraculo = Ui.texto("", Tema.corpo(), Tema.ROXO_CLARO);
        JLabel valorVoce = Ui.corpo("");
        slider.aoMudar((o, v) -> {
            valorAutomacao.setText((100 - o - v) + "%");
            valorOraculo.setText(o + "%");
            valorVoce.setText(v + "%");
        });
        return Ui.cabecalho(Ui.linha(12, largura(Ui.corpo(provedor.toString()), 100), largura(slider, 280),
                largura(valorAutomacao, 40), largura(valorOraculo, 40), largura(valorVoce, 40)), null);
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
        Notificacoes atual = configuracoes.notificacoes();
        silenciarAgora = new Interruptor(atual.silenciarAgora());
        silenciarAutomaticamente = new Interruptor(atual.silenciarAutomaticamente());
        silencioInicio = lista(atual.silencioInicio().format(HORA), "23:00", "22:00", "00:00");
        silencioFim = lista(atual.silencioFim().format(HORA), "07:00", "06:00", "08:00");
        falhasFuramSilencio = new Interruptor(atual.falhasFuramSilencio());
        desktop = new Interruptor(atual.desktop());
        whatsapp = new Interruptor(atual.whatsapp());
        som = new Interruptor(atual.som());

        JPanel pagina = pagina();
        pagina.add(Ui.cabecalho(Ui.secao("Modo silencioso"), null));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("Silenciar agora", silenciarAgora));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("Silenciar automaticamente", silenciarAutomaticamente));
        pagina.add(Box.createVerticalStrut(6));
        pagina.add(Ui.cabecalho(Ui.linha(8, Ui.legenda("Das"), silencioInicio,
                Ui.legenda("às"), silencioFim), null));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("Falhas furam o silêncio", falhasFuramSilencio));

        pagina.add(Box.createVerticalStrut(20));
        pagina.add(Ui.cabecalho(Ui.secao("Canais"), null));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("Desktop (Windows)", desktop));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("WhatsApp", whatsapp));
        pagina.add(Box.createVerticalStrut(8));
        pagina.add(opcao("Som", som));

        pagina.add(Box.createVerticalStrut(20));
        pagina.add(Ui.cabecalho(Ui.secao("Avisar sobre"), null));
        pagina.add(Box.createVerticalStrut(8));
        JPanel tipos = new JPanel(new java.awt.GridLayout(0, 4, 16, 4));
        tipos.setOpaque(false);
        for (Aviso aviso : Aviso.values()) {
            JCheckBox caixa = new JCheckBox(aviso.toString(), atual.avisos().contains(aviso));
            caixa.setFont(Tema.corpo());
            caixa.setOpaque(false);
            avisos.put(aviso, caixa);
            tipos.add(caixa);
        }
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

    private static JComponent opcao(String texto, Interruptor interruptor) {
        return Ui.cabecalho(Ui.corpo(texto), interruptor);
    }

    private static JComponent campo(String rotulo, JComponent entrada) {
        return Ui.coluna(4, Ui.legenda(rotulo), entrada);
    }

    /** Lista com as opções e {@code atual} selecionado; um valor salvo fora das opções entra no fim delas. */
    private static JComboBox<String> lista(String atual, String... opcoes) {
        JComboBox<String> lista = new JComboBox<>(opcoes);
        if (!List.of(opcoes).contains(atual)) {
            lista.addItem(atual);
        }
        lista.setSelectedItem(atual);
        lista.setFont(Tema.corpo());
        lista.setPreferredSize(new Dimension(110, 30));
        lista.setMaximumSize(lista.getPreferredSize());
        return lista;
    }

    /** O número no começo da opção escolhida ("30 min" → 30). */
    private static int numero(JComboBox<String> lista) {
        return Integer.parseInt(((String) lista.getSelectedItem()).split(" ")[0]);
    }
}
