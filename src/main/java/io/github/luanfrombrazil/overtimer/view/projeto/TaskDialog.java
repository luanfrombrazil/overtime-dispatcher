package io.github.luanfrombrazil.overtimer.view.projeto;

import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.view.comum.Dialogo;
import io.github.luanfrombrazil.overtimer.view.comum.Formulario;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

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
 * Pop-up de criação e edição de Task: título, descrição, provedor e esforço. A Task finalizada abre travada,
 * só para leitura.
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
     * @param aoDecidir   recebe o novo estado quando o usuário manda refazer a Task executada inteira
     * @param aoRascunho  recebe a Task como está quando o usuário clica fora do pop-up de uma Task nova ou de um
     *                    rascunho, que então fecha; null mantém o pop-up aberto
     */
    public TaskDialog(JFrame dono, Projeto projeto, Task existente, Consumer<Task> aoSalvar, Runnable aoExcluir,
                      Consumer<Estado> aoDecidir, Consumer<Task> aoRascunho) {
        super(dono, Tema.BORDA);
        this.existente = existente;
        boolean nova = existente == null;
        boolean rascunho = !nova && existente.rascunho();
        boolean travada = !nova && existente.finalizada();

        JPanel corpo = Formulario.corpo();
        corpo.add(Ui.cabecalho(Ui.titulo(nova ? "Nova task" : rascunho ? "Rascunho de task"
                : travada ? "Task finalizada" : "Editar task"), null));
        corpo.add(Box.createVerticalStrut(14));
        // A finalizada não mostra a situação: a mensagem de conclusão fica no botão verde do cartão.
        if (!nova && !rascunho && !travada && existente.inteira() && existente.execucao() != Estado.PENDENTE) {
            corpo.add(SubtarefaDialog.situacao(this, existente.execucao(), existente.resultado(),
                    existente.aguardaDirecionamento(), aoDecidir));
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
        String confirmarExclusao = "Excluir a task e todas as suas subtarefas?";
        if (travada) {
            // A Task finalizada só pode ser vista ou excluída.
            Formulario.travar(titulo, descricao, provedor, esforco);
            raiz.add(Formulario.rodapeSomenteLeitura(this, aoExcluir, confirmarExclusao), BorderLayout.SOUTH);
        } else {
            raiz.add(Formulario.rodape(this, nova || rascunho ? "Criar task" : "Salvar", () -> aoSalvar.accept(task()),
                    nova ? null : aoExcluir, confirmarExclusao), BorderLayout.SOUTH);
        }
        if ((nova || rascunho) && aoRascunho != null) {
            aoClicarFora(() -> {
                Task atual = nova && vazia() ? null : comTituloDeRascunho(task());
                fechar();
                if (atual != null) {
                    Formulario.rascunhar(dono, () -> aoRascunho.accept(atual));
                }
            });
        }
        raiz.setPreferredSize(new Dimension(560, raiz.getPreferredSize().height));
    }

    /** Se nada foi preenchido; uma Task nova vazia é descartada em vez de virar rascunho. */
    private boolean vazia() {
        return titulo.getText().isBlank() && Formulario.textoDa(descricao).isBlank();
    }

    private static Task comTituloDeRascunho(Task task) {
        if (!task.titulo().isBlank()) {
            return task;
        }
        return new Task(task.id(), "Rascunho", task.descricao(), task.provedor(), task.esforco(), task.origem(),
                task.subtarefas(), task.sessao(), task.execucao(), task.resultado(), task.direcionamento());
    }

    private Task task() {
        Provedor p = (Provedor) provedor.getSelectedItem();
        Esforco e = (Esforco) esforco.getSelectedItem();
        if (existente == null) {
            return Task.nova(titulo.getText().strip(), Formulario.textoDa(descricao), p, e);
        }
        return new Task(existente.id(), titulo.getText().strip(), Formulario.textoDa(descricao), p, e,
                existente.origem(), existente.subtarefas(), existente.sessao(), existente.execucao(),
                existente.resultado(), existente.direcionamento());
    }
}
