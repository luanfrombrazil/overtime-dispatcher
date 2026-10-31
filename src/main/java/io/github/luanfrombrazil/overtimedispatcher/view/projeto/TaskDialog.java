package io.github.luanfrombrazil.overtimedispatcher.view.projeto;

import io.github.luanfrombrazil.overtimedispatcher.model.Esforco;
import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Provedor;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Dialogo;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Formulario;
import io.github.luanfrombrazil.overtimedispatcher.view.comum.Ui;
import io.github.luanfrombrazil.overtimedispatcher.view.tema.Tema;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Pop-up de criação e edição de Task: título, descrição, provedor e esforço.
 */
public class TaskDialog extends Dialogo {

    private final Task existente;
    private final JTextField titulo;
    private final JScrollPane descricao;
    private final JComboBox<Provedor> provedor;
    private final JComboBox<Esforco> esforco;

    /**
     * @param projeto   dá os padrões de provedor e esforço para uma Task nova
     * @param existente Task a editar, ou null para criar uma nova
     * @param aoDecidir recebe o novo estado quando o usuário manda refazer a Task executada inteira
     */
    public TaskDialog(JFrame dono, Projeto projeto, Task existente, Consumer<Task> aoSalvar, Runnable aoExcluir,
                      Consumer<Estado> aoDecidir) {
        super(dono, Tema.BORDA);
        this.existente = existente;
        boolean nova = existente == null;

        JPanel corpo = Formulario.corpo();
        corpo.add(Ui.cabecalho(Ui.titulo(nova ? "Nova task" : "Editar task"), null));
        corpo.add(Box.createVerticalStrut(14));
        if (!nova && existente.inteira() && existente.execucao() != Estado.PENDENTE) {
            corpo.add(SubtarefaDialog.situacao(this, existente.execucao(), existente.resultado(), aoDecidir));
            corpo.add(Box.createVerticalStrut(14));
        }

        titulo = Formulario.texto(nova ? "" : existente.titulo());
        corpo.add(Formulario.campo("Título", titulo));
        corpo.add(Box.createVerticalStrut(12));
        descricao = Formulario.area(nova ? "" : existente.descricao(), 4);
        corpo.add(Formulario.campo("Descrição", descricao));

        Formulario.secao(corpo, "Execução");
        provedor = Formulario.opcoes(244, Provedor.values());
        esforco = Formulario.opcoes(244, Esforco.values());
        provedor.setSelectedItem(nova ? projeto.provedor() : existente.provedor());
        esforco.setSelectedItem(nova ? projeto.esforco() : existente.esforco());
        corpo.add(Ui.cabecalho(Ui.linha(12,
                Formulario.campo("Provedor", provedor),
                Formulario.campo("Esforço", esforco)), null));

        JPanel raiz = (JPanel) getContentPane();
        raiz.add(corpo, BorderLayout.CENTER);
        raiz.add(Formulario.rodape(this, nova ? "Criar task" : "Salvar", () -> aoSalvar.accept(task()),
                nova ? null : aoExcluir, "Excluir a task e todas as suas subtarefas?"), BorderLayout.SOUTH);
        raiz.setPreferredSize(new Dimension(560, raiz.getPreferredSize().height));
    }

    private Task task() {
        Provedor p = (Provedor) provedor.getSelectedItem();
        Esforco e = (Esforco) esforco.getSelectedItem();
        if (existente == null) {
            return Task.nova(titulo.getText().strip(), Formulario.textoDa(descricao), p, e);
        }
        return new Task(existente.id(), titulo.getText().strip(), Formulario.textoDa(descricao), p, e,
                existente.origem(), existente.subtarefas(), existente.sessao(), existente.execucao(),
                existente.resultado());
    }
}
