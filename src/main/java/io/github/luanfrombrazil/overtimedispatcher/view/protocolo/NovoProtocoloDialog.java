package io.github.luanfrombrazil.overtimedispatcher.view.protocolo;

import io.github.luanfrombrazil.overtimedispatcher.view.comum.Chip;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Dialogo;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Formulario;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Pop-up de criação de protocolo: passo a passo, quando roda, acessos e execução.
 */
public class NovoProtocoloDialog extends Dialogo {

    public NovoProtocoloDialog(JFrame dono) {
        super(dono, Tema.BORDA);
        JPanel corpo = Formulario.corpo();
        corpo.add(Ui.cabecalho(Ui.titulo("Novo protocolo"), null));
        corpo.add(Box.createVerticalStrut(14));

        corpo.add(Formulario.identidade());
        corpo.add(Box.createVerticalStrut(12));
        corpo.add(Formulario.campo("Passo a passo", Formulario.area("", 4)));

        Formulario.secao(corpo, "Quando");
        JToggleButton fixos = new JToggleButton("Horários fixos", true);
        JToggleButton intervalo = new JToggleButton("Intervalo");
        ButtonGroup modo = new ButtonGroup();
        modo.add(fixos);
        modo.add(intervalo);
        fixos.setFont(Tema.legenda());
        intervalo.setFont(Tema.legenda());
        corpo.add(Ui.cabecalho(Ui.linha(0, fixos, intervalo), null));
        corpo.add(Box.createVerticalStrut(8));
        JPanel horarios = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        horarios.setOpaque(false);
        horarios.add(new Chip("07:00  ×", true));
        horarios.add(new Chip("13:00  ×", true));
        horarios.add(new Chip("+", false));
        corpo.add(Ui.largo(horarios));

        Formulario.secao(corpo, "Acessos");
        JCheckBox sites = Formulario.caixa("Sites", true);
        sites.setPreferredSize(new Dimension(100, sites.getPreferredSize().height));
        JPanel linhaSites = new JPanel(new BorderLayout(8, 0));
        linhaSites.setOpaque(false);
        linhaSites.add(sites, BorderLayout.WEST);
        linhaSites.add(Formulario.texto("https://"), BorderLayout.CENTER);
        linhaSites.add(Ui.botaoSecundario("Fazer login"), BorderLayout.EAST);
        corpo.add(Ui.largo(linhaSites));
        corpo.add(acesso("E-mail", "leitura e rascunhos"));
        corpo.add(acesso("Calendário", "só a agenda Overtime"));

        Formulario.secao(corpo, "Execução");
        corpo.add(Ui.cabecalho(Ui.linha(12,
                Formulario.campo("Provedor · modelo", Formulario.lista(210, "Claude Code · Sonnet", "Claude Code · Opus",
                        "Codex", "Gemini")),
                Formulario.campo("Esforço", Formulario.lista(120, "Baixo", "Médio", "Alto")),
                Formulario.campo("Avisar", Formulario.lista(170, "Só novidades", "Sempre", "Nunca"))), null));
        corpo.add(Box.createVerticalStrut(10));
        corpo.add(Ui.cabecalho(Ui.legenda("Aceitar tasks direto de"), null));
        corpo.add(Box.createVerticalStrut(6));
        JPanel origens = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        origens.setOpaque(false);
        origens.add(new Chip("Nenhum", false));
        origens.add(new Chip("+", false));
        corpo.add(Ui.largo(origens));

        JPanel raiz = (JPanel) getContentPane();
        raiz.add(corpo, BorderLayout.CENTER);
        raiz.add(Formulario.rodape(this, "Criar protocolo"), BorderLayout.SOUTH);
        raiz.setPreferredSize(new Dimension(600, raiz.getPreferredSize().height));
    }

    private static JComponent acesso(String nome, String limite) {
        JCheckBox caixa = Formulario.caixa(nome, false);
        Dimension d = new Dimension(100, caixa.getPreferredSize().height);
        caixa.setPreferredSize(d);
        caixa.setMaximumSize(d);
        return Ui.cabecalho(Ui.linha(8, caixa, Ui.legenda(limite)), null);
    }
}
