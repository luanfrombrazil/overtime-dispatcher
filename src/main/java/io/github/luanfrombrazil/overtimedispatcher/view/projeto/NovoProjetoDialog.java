package io.github.luanfrombrazil.overtimedispatcher.view.projeto;

import io.github.luanfrombrazil.overtimedispatcher.view.comum.Dialogo;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Formulario;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Pop-up de criação de projeto: nome, descrição, pasta, git, padrões e permissões.
 */
public class NovoProjetoDialog extends Dialogo {

    public NovoProjetoDialog(JFrame dono) {
        super(dono, Tema.BORDA);
        JPanel corpo = Formulario.corpo();
        corpo.add(Ui.cabecalho(Ui.titulo("Novo projeto"), null));
        corpo.add(Box.createVerticalStrut(14));

        corpo.add(Formulario.identidade());
        corpo.add(Box.createVerticalStrut(12));
        corpo.add(Formulario.campo("Descrição", Formulario.area("", 3)));

        Formulario.secao(corpo, "Pasta do projeto");
        JTextField pasta = Formulario.texto("");
        JButton procurar = Ui.botaoSecundario("Procurar…");
        procurar.addActionListener(e -> {
            JFileChooser seletor = new JFileChooser();
            seletor.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pasta.setText(seletor.getSelectedFile().getAbsolutePath());
            }
        });
        JPanel linhaPasta = new JPanel(new BorderLayout(8, 0));
        linhaPasta.setOpaque(false);
        linhaPasta.add(pasta, BorderLayout.CENTER);
        linhaPasta.add(procurar, BorderLayout.EAST);
        corpo.add(Ui.largo(linhaPasta));
        corpo.add(Box.createVerticalStrut(8));
        JCheckBox usarGit = Formulario.caixa("Usar git", true);
        corpo.add(Ui.cabecalho(usarGit, null));
        JComboBox<String> branch = Formulario.lista(200, "main", "master", "develop");
        corpo.add(Box.createVerticalStrut(6));
        corpo.add(Ui.cabecalho(Formulario.campo("Branch padrão", branch), null));
        usarGit.addActionListener(e -> branch.setEnabled(usarGit.isSelected()));

        Formulario.secao(corpo, "Padrões das tasks");
        corpo.add(Ui.cabecalho(Ui.linha(12,
                Formulario.campo("Provedor", Formulario.lista(244, "Automático", "Claude Code", "Codex", "Gemini")),
                Formulario.campo("Esforço", Formulario.lista(244, "Médio", "Baixo", "Alto", "Muito alto", "Máximo"))),
                null));

        Formulario.secao(corpo, "Permissões do agente");
        corpo.add(Ui.cabecalho(Formulario.caixa("Editar arquivos", true), null));
        corpo.add(Ui.cabecalho(Formulario.caixa("Rodar testes e build", true), null));
        corpo.add(Ui.cabecalho(Formulario.caixa("Instalar dependências", false), null));
        corpo.add(Ui.cabecalho(Formulario.caixa("Acessar a internet", false), null));

        JPanel raiz = (JPanel) getContentPane();
        raiz.add(corpo, BorderLayout.CENTER);
        raiz.add(Formulario.rodape(this, "Criar projeto"), BorderLayout.SOUTH);
        raiz.setPreferredSize(new Dimension(560, raiz.getPreferredSize().height));
    }
}
