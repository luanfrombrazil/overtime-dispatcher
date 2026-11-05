package io.github.luanfrombrazil.overtimer.view.protocolo;

import io.github.luanfrombrazil.overtimer.model.Acessos;
import io.github.luanfrombrazil.overtimer.model.Agenda;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.view.comum.Chip;
import io.github.luanfrombrazil.overtimer.view.comum.Dialogo;
import io.github.luanfrombrazil.overtimer.view.comum.Formulario;
import io.github.luanfrombrazil.overtimer.view.comum.Iniciais;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Pop-up de criação de protocolo: passo a passo, quando roda, acessos e execução.
 */
public class NovoProtocoloDialog extends Dialogo {

    private static final int INTERVALO_PADRAO = 60;

    private final JTextField nome;
    private final JScrollPane passos;
    private final JToggleButton fixos;
    private final TreeSet<LocalTime> horas = new TreeSet<>();
    private final JPanel horarios;
    private final JSpinner intervalo;
    private final JPanel linhaIntervalo;
    private final JCheckBox sites;
    private final JTextField enderecos;
    private final JCheckBox email;
    private final JCheckBox calendario;
    private final JTextField conectorEmail;
    private final JTextField conectorCalendario;
    private final JComboBox<Provedor> provedor;
    private final JComboBox<Esforco> esforco;

    /** @param aoSalvar recebe o protocolo preenchido; uma IllegalArgumentException mantém o pop-up aberto */
    public NovoProtocoloDialog(JFrame dono, Consumer<Protocolo> aoSalvar) {
        super(dono, Tema.BORDA);
        JPanel corpo = Formulario.corpo();
        corpo.add(Ui.cabecalho(Ui.titulo("Novo protocolo"), null));
        corpo.add(Box.createVerticalStrut(14));

        nome = Formulario.texto("");
        corpo.add(Formulario.identidade(nome));
        corpo.add(Box.createVerticalStrut(12));
        passos = Formulario.area("", 4);
        corpo.add(Formulario.campo("Passo a passo", passos));

        Formulario.secao(corpo, "Quando");
        fixos = new JToggleButton("Horários fixos", true);
        JToggleButton aCada = new JToggleButton("Intervalo");
        ButtonGroup modo = new ButtonGroup();
        modo.add(fixos);
        modo.add(aCada);
        fixos.setFont(Tema.legenda());
        aCada.setFont(Tema.legenda());
        corpo.add(Ui.cabecalho(Ui.linha(0, fixos, aCada), null));
        corpo.add(Box.createVerticalStrut(8));
        horarios = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        horarios.setOpaque(false);
        corpo.add(Ui.largo(horarios));
        intervalo = new JSpinner(new SpinnerNumberModel(INTERVALO_PADRAO, 1, 24 * 60, 5));
        intervalo.setFont(Tema.corpo());
        linhaIntervalo = Ui.cabecalho(Ui.linha(8, intervalo, Ui.legenda("minutos")), null);
        corpo.add(linhaIntervalo);
        fixos.addActionListener(e -> mostrarModo());
        aCada.addActionListener(e -> mostrarModo());
        atualizarHorarios();
        mostrarModo();

        Formulario.secao(corpo, "Acessos");
        sites = Formulario.caixa("Sites", false);
        sites.setPreferredSize(new Dimension(100, sites.getPreferredSize().height));
        enderecos = Formulario.texto("https://");
        JPanel linhaSites = new JPanel(new BorderLayout(8, 0));
        linhaSites.setOpaque(false);
        linhaSites.add(sites, BorderLayout.WEST);
        linhaSites.add(enderecos, BorderLayout.CENTER);
        corpo.add(Ui.largo(linhaSites));
        email = Formulario.caixa("E-mail", false);
        calendario = Formulario.caixa("Calendário", false);
        conectorEmail = Formulario.texto("");
        conectorCalendario = Formulario.texto("");
        corpo.add(Box.createVerticalStrut(6));
        corpo.add(acesso(email, conectorEmail, "leitura e rascunhos"));
        corpo.add(Box.createVerticalStrut(6));
        corpo.add(acesso(calendario, conectorCalendario, "só a agenda Overtimer"));

        Formulario.secao(corpo, "Execução");
        provedor = Formulario.opcoes(210, Provedor.values());
        esforco = Formulario.opcoes(140, Esforco.values());
        provedor.setSelectedItem(Provedor.AUTOMATICO);
        esforco.setSelectedItem(Esforco.MEDIUM);
        corpo.add(Ui.cabecalho(Ui.linha(12,
                Formulario.campo("Provedor", provedor),
                Formulario.campo("Esforço", esforco)), null));

        JPanel raiz = (JPanel) getContentPane();
        raiz.add(corpo, BorderLayout.CENTER);
        raiz.add(Formulario.rodape(this, "Criar protocolo", () -> aoSalvar.accept(protocolo()), null, null),
                BorderLayout.SOUTH);
        raiz.setPreferredSize(new Dimension(600, raiz.getPreferredSize().height));
    }

    /** Caixa do acesso, nome do servidor MCP (habilitado só com a caixa marcada) e o limite do acesso. */
    private static JComponent acesso(JCheckBox caixa, JTextField conector, String limite) {
        caixa.setPreferredSize(new Dimension(100, caixa.getPreferredSize().height));
        conector.setToolTipText("Conector MCP");
        conector.setEnabled(false);
        caixa.addActionListener(e -> conector.setEnabled(caixa.isSelected()));
        JPanel linha = new JPanel(new BorderLayout(8, 0));
        linha.setOpaque(false);
        linha.add(caixa, BorderLayout.WEST);
        linha.add(conector, BorderLayout.CENTER);
        linha.add(Ui.legenda(limite), BorderLayout.EAST);
        return Ui.largo(linha);
    }

    private static String conectorDe(JCheckBox caixa, JTextField conector) {
        if (!caixa.isSelected()) {
            return null;
        }
        if (conector.getText().isBlank()) {
            throw new IllegalArgumentException("Informe o conector MCP de " + caixa.getText()
                    + " ou desmarque o acesso.");
        }
        return conector.getText().strip();
    }

    private void mostrarModo() {
        horarios.setVisible(fixos.isSelected());
        linhaIntervalo.setVisible(!fixos.isSelected());
        revalidate();
        repaint();
    }

    /** Um chip por horário, que sai ao ser clicado, e o "+ Horário" no fim. */
    private void atualizarHorarios() {
        horarios.removeAll();
        for (LocalTime hora : horas) {
            Chip chip = new Chip(hora + "  ×", true).aoClicar(() -> {
                horas.remove(hora);
                atualizarHorarios();
            });
            horarios.add(chip);
        }
        horarios.add(new Chip("+ Horário", false).aoClicar(this::adicionarHorario));
        horarios.revalidate();
        horarios.repaint();
    }

    private void adicionarHorario() {
        String digitado = JOptionPane.showInputDialog(this, "Horário (HH:mm)", "08:00");
        if (digitado == null || digitado.isBlank()) {
            return;
        }
        try {
            horas.add(LocalTime.parse(digitado.strip().length() == 4 ? "0" + digitado.strip() : digitado.strip()));
            atualizarHorarios();
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "\"" + digitado.strip() + "\" não é um horário.", "Horário",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private Protocolo protocolo() {
        String nomeDigitado = nome.getText().strip();
        Agenda agenda = fixos.isSelected() ? Agenda.nosHorarios(List.copyOf(horas))
                : Agenda.aCada((Integer) intervalo.getValue());
        List<String> listaSites = sites.isSelected()
                ? List.of(enderecos.getText().split("[,\\s]+")).stream().filter(s -> !s.equals("https://")).toList()
                : List.of();
        if (sites.isSelected() && listaSites.stream().allMatch(String::isBlank)) {
            throw new IllegalArgumentException("Informe o endereço do site ou desmarque \"Sites\".");
        }
        Acessos acessos = new Acessos(listaSites, email.isSelected(), calendario.isSelected(),
                conectorDe(email, conectorEmail), conectorDe(calendario, conectorCalendario));
        return Protocolo.novo(nomeDigitado, Iniciais.de(nomeDigitado), Formulario.textoDa(passos), agenda,
                (Provedor) provedor.getSelectedItem(), (Esforco) esforco.getSelectedItem(), acessos, List.of());
    }
}
