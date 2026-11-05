package io.github.luanfrombrazil.overtimer.controller;

import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.Git;
import io.github.luanfrombrazil.overtimer.service.ProjetoService;
import io.github.luanfrombrazil.overtimer.view.comum.Iniciais;
import io.github.luanfrombrazil.overtimer.view.projeto.ProjetoDialog;
import io.github.luanfrombrazil.overtimer.view.projeto.ProjetoPanel;
import io.github.luanfrombrazil.overtimer.view.projeto.SubtarefaDialog;
import io.github.luanfrombrazil.overtimer.view.projeto.TaskDialog;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Liga a tela de projeto e os pop-ups de projeto, Task e subtarefa ao {@link ProjetoService}.
 */
public class ProjetoController implements ProjetoPanel.Ouvinte {

    private final JFrame frame;
    private final ProjetoService servico;
    private final Function<String, List<String>> logDe;
    private final Consumer<Projeto> aoAlterar;
    private final Runnable aoExcluir;
    private final ProjetoPanel painel = new ProjetoPanel(this);
    private Projeto projeto;

    /**
     * @param logDe     log do agente de um projeto, pelo id
     * @param aoAlterar chamado com o projeto depois de criado ou alterado, para atualizar a janela
     * @param aoExcluir chamado depois que o projeto aberto é excluído
     */
    public ProjetoController(JFrame frame, ProjetoService servico, Function<String, List<String>> logDe,
                             Consumer<Projeto> aoAlterar, Runnable aoExcluir) {
        this.frame = frame;
        this.servico = servico;
        this.logDe = logDe;
        this.aoAlterar = aoAlterar;
        this.aoExcluir = aoExcluir;
    }

    /** Mostra o projeto no painel e devolve o painel. */
    public ProjetoPanel abrir(Projeto projeto) {
        this.projeto = projeto;
        painel.mostrar(projeto, logDe.apply(projeto.id()));
        return painel;
    }

    /** Leva uma linha nova do log ao console, se o projeto dela estiver no painel. */
    public void adicionarLog(String projetoId, String linha) {
        if (projeto != null && projeto.id().equals(projetoId)) {
            painel.adicionarLog(linha);
        }
    }

    public void novoProjeto() {
        new ProjetoDialog(frame, null, p -> aoAlterar.accept(servico.salvar(p)), null, Git::branches).abrir();
    }

    @Override
    public void editarProjeto() {
        new ProjetoDialog(frame, projeto, p -> aoAlterar.accept(servico.salvar(p)), () -> {
            servico.excluir(projeto);
            aoExcluir.run();
        }, Git::branches).abrir();
    }

    @Override
    public void renomearProjeto(String nome) {
        aoAlterar.accept(servico.salvar(projeto.comNome(nome, Iniciais.de(nome))));
    }

    @Override
    public void trocarImagemProjeto(String imagem) {
        try {
            aoAlterar.accept(servico.salvar(projeto.comImagem(imagem)));
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Imagem do projeto", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void novaTask() {
        new TaskDialog(frame, projeto, null, t -> alterado(servico.salvarTask(projeto, t)), null, null,
                t -> alterado(servico.salvarRascunho(projeto, t))).abrir();
    }

    @Override
    public void editarTask(Task task) {
        new TaskDialog(frame, projeto, task, t -> alterado(servico.salvarTask(projeto, t)),
                () -> alterado(servico.excluirTask(projeto, task)),
                estado -> alterado(servico.definirEstado(projeto, task, estado)),
                t -> alterado(servico.salvarRascunho(projeto, t))).abrir();
    }

    @Override
    public void moverTask(Task task, int deslocamento) {
        try {
            alterado(servico.moverTask(projeto, task, deslocamento));
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Ordenar tasks", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void moverSubtarefa(Task task, Subtarefa subtarefa, int deslocamento) {
        try {
            alterado(servico.moverSubtarefa(projeto, task, subtarefa, deslocamento));
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Ordenar subtarefas", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void novaSubtarefa(Task task) {
        // Enquanto a subtarefa é escrita, a Task não desce para as finalizadas, mesmo que as outras terminem.
        servico.segurarAberta(task);
        try {
            new SubtarefaDialog(frame, task, null, s -> alterado(servico.salvarSubtarefa(projeto, task, s)), null,
                    null, s -> alterado(servico.salvarRascunho(projeto, task, s))).abrir();
        } finally {
            servico.soltar(task);
        }
        // Sem subtarefa nova, a Task que terminou nesse meio-tempo desce agora.
        servico.buscar(projeto.id()).ifPresent(this::alterado);
    }

    @Override
    public boolean seguraAberta(Task task) {
        return servico.seguraAberta(task);
    }

    @Override
    public void editarSubtarefa(Task task, Subtarefa subtarefa) {
        new SubtarefaDialog(frame, task, subtarefa, s -> alterado(servico.salvarSubtarefa(projeto, task, s)),
                () -> alterado(servico.excluirSubtarefa(projeto, task, subtarefa)),
                estado -> alterado(servico.definirEstado(projeto, task, subtarefa, estado)),
                s -> alterado(servico.salvarRascunho(projeto, task, s))).abrir();
    }

    @Override
    public void amarrar(Task task, Subtarefa origem, Subtarefa destino) {
        try {
            alterado(servico.alternarDependencia(projeto, task, origem, destino));
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Amarrar subtarefas", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void tentarDeNovo(Task task, Subtarefa subtarefa) {
        alterado(servico.definirEstado(projeto, task, subtarefa, Estado.PENDENTE));
    }

    @Override
    public void tentarDeNovo(Task task) {
        alterado(servico.tentarDeNovo(projeto, task));
    }

    /** As respostas voltam a Task para a fila; {@link #alterado} faz o agendador olhar já. */
    @Override
    public void responder(Task task, Subtarefa subtarefa, List<String> respostas) {
        alterado(servico.responder(projeto, task, subtarefa, respostas));
    }

    private void alterado(Projeto atualizado) {
        projeto = atualizado;
        aoAlterar.accept(atualizado);
    }
}
