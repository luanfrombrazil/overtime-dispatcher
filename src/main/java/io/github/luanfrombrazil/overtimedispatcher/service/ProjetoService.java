package io.github.luanfrombrazil.overtimedispatcher.service;

import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.Origem;
import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Subtarefa;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import io.github.luanfrombrazil.overtimedispatcher.service.persistence.RepositorioProjetos;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Cria, altera e exclui projetos, Tasks e subtarefas, salvando cada mudança.
 * <p>
 * As entidades são imutáveis: cada operação devolve o projeto atualizado. Erros de validação
 * lançam {@link IllegalArgumentException} com uma mensagem para mostrar ao usuário.
 * <p>
 * É usado pela tela e pelo agendador ao mesmo tempo, por isso as operações são sincronizadas. O estado das
 * subtarefas só muda pelo agendador ({@link #marcar}) ou por uma decisão do usuário ({@link #definirEstado});
 * editar uma subtarefa não mexe no estado dela.
 */
public class ProjetoService {

    /** Subtarefa pronta para ir a um agente, com a Task e o projeto dela. */
    public record Proxima(Projeto projeto, Task task, Subtarefa subtarefa) {
    }

    private final RepositorioProjetos repositorio;
    private final Map<String, Projeto> projetos = new HashMap<>();
    private final List<Consumer<Projeto>> ouvintes = new CopyOnWriteArrayList<>();

    /** Carrega os projetos. Subtarefas que estavam em andamento quando a app fechou voltam para a fila. */
    public ProjetoService(RepositorioProjetos repositorio) {
        this.repositorio = repositorio;
        for (Projeto p : repositorio.carregar()) {
            Projeto recuperado = p.comTasks(p.tasks().stream().map(t -> t.comSubtarefas(t.subtarefas().stream()
                    .map(s -> s.estado() == Estado.EM_ANDAMENTO ? s.comEstado(Estado.PENDENTE, s.resultado()) : s)
                    .toList())).toList());
            projetos.put(p.id(), p);
            if (!recuperado.equals(p)) {
                gravar(recuperado);
            }
        }
    }

    /** Avisado (na thread de quem mudou) quando o agendador altera um projeto. */
    public void aoMudar(Consumer<Projeto> ouvinte) {
        ouvintes.add(ouvinte);
    }

    /** Projetos em ordem alfabética. */
    public synchronized List<Projeto> projetos() {
        return projetos.values().stream()
                .sorted(Comparator.comparing(Projeto::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized Optional<Projeto> buscar(String id) {
        return Optional.ofNullable(projetos.get(id));
    }

    /** Cria o projeto ou substitui o de mesmo id, mantendo as Tasks já existentes. */
    public synchronized Projeto salvar(Projeto projeto) {
        exigir(projeto.nome(), "Dê um nome ao projeto.");
        exigir(projeto.pasta(), "Escolha a pasta do projeto.");
        if (!pastaExiste(projeto.pasta())) {
            throw new IllegalArgumentException("A pasta \"" + projeto.pasta() + "\" não existe.");
        }
        if (projeto.branch() != null) {
            exigir(projeto.branch(), "Informe a branch padrão ou desmarque \"Usar git\".");
            List<String> branches = Git.branches(Path.of(projeto.pasta()));
            if (!branches.isEmpty() && !branches.contains(projeto.branch())) {
                throw new IllegalArgumentException("A branch \"" + projeto.branch() + "\" não existe na pasta do projeto.");
            }
        }
        Projeto existente = projetos.get(projeto.id());
        return gravar(existente == null ? projeto : projeto.comTasks(existente.tasks()));
    }

    public synchronized void excluir(Projeto projeto) {
        repositorio.excluir(projeto.id());
        projetos.remove(projeto.id());
    }

    /** Adiciona a Task ao fim do projeto, ou substitui a de mesmo id mantendo as subtarefas e a sessão. */
    public synchronized Projeto salvarTask(Projeto projeto, Task task) {
        exigir(task.titulo(), "Dê um título à task.");
        Projeto atual = atual(projeto);
        List<Task> tasks = new ArrayList<>(atual.tasks());
        int indice = indiceDaTask(tasks, task.id());
        if (indice < 0) {
            tasks.add(task);
        } else {
            Task antiga = tasks.get(indice);
            tasks.set(indice, task.comSubtarefas(antiga.subtarefas()).comSessao(antiga.sessao()));
        }
        return gravar(atual.comTasks(tasks));
    }

    public synchronized Projeto excluirTask(Projeto projeto, Task task) {
        Projeto atual = atual(projeto);
        return gravar(atual.comTasks(atual.tasks().stream().filter(t -> !t.id().equals(task.id())).toList()));
    }

    /**
     * Move a Task {@code deslocamento} posições (-1 sobe, +1 desce), limitado às pontas da lista. As Tasks rodam
     * na ordem da lista, a mesma mostrada na tela.
     */
    public synchronized Projeto moverTask(Projeto projeto, Task task, int deslocamento) {
        Projeto atual = atual(projeto);
        List<Task> tasks = new ArrayList<>(atual.tasks());
        int indice = indiceDaTask(tasks, task.id());
        if (indice < 0) {
            throw new IllegalArgumentException("Task não encontrada.");
        }
        int destino = Math.max(0, Math.min(tasks.size() - 1, indice + deslocamento));
        if (destino == indice) {
            return atual;
        }
        tasks.add(destino, tasks.remove(indice));
        return gravar(atual.comTasks(tasks));
    }

    /** Adiciona a subtarefa ao fim da Task, ou substitui a de mesmo id mantendo o estado e o resultado. */
    public synchronized Projeto salvarSubtarefa(Projeto projeto, Task task, Subtarefa subtarefa) {
        exigir(subtarefa.titulo(), "Dê um título à subtarefa.");
        return alterarSubtarefas(projeto, task, subtarefas -> {
            List<Subtarefa> novas = new ArrayList<>(subtarefas);
            int indice = indiceDaSubtarefa(novas, subtarefa.id());
            if (indice < 0) {
                novas.add(subtarefa.comEstado(Estado.PENDENTE, null));
            } else {
                Subtarefa antiga = novas.get(indice);
                novas.set(indice, subtarefa.comEstado(antiga.estado(), antiga.resultado()));
            }
            return novas;
        });
    }

    /**
     * Decisão do usuário sobre uma subtarefa: aprovar a verificação (concluída) ou mandar refazer (de volta à
     * fila). Uma subtarefa em andamento não pode ser alterada.
     */
    public synchronized Projeto definirEstado(Projeto projeto, Task task, Subtarefa subtarefa, Estado estado) {
        return alterarSubtarefas(projeto, task, subtarefas -> subtarefas.stream().map(s -> {
            if (!s.id().equals(subtarefa.id())) {
                return s;
            }
            if (s.estado() == Estado.EM_ANDAMENTO) {
                throw new IllegalArgumentException("A subtarefa está em andamento; espere o agente terminar.");
            }
            return s.comEstado(estado, s.resultado());
        }).toList());
    }

    /** Devolve à fila todas as subtarefas da Task que falharam. */
    public synchronized Projeto tentarDeNovo(Projeto projeto, Task task) {
        return alterarSubtarefas(projeto, task, subtarefas -> subtarefas.stream()
                .map(s -> s.estado() == Estado.ERRO ? s.comEstado(Estado.PENDENTE, s.resultado()) : s)
                .toList());
    }

    /**
     * Primeira subtarefa na fila cujas dependências já foram concluídas, seguindo a ordem dos projetos,
     * das Tasks e das subtarefas. Tasks do Oráculo esperam ser aceitas.
     */
    public synchronized Optional<Proxima> proximaSubtarefa() {
        for (Projeto projeto : projetos()) {
            for (Task task : projeto.tasks()) {
                if (task.origem() == Origem.ORACULO) {
                    continue;
                }
                Set<String> concluidas = new HashSet<>();
                task.subtarefas().stream().filter(s -> s.estado() == Estado.CONCLUIDO).forEach(s -> concluidas.add(s.id()));
                for (Subtarefa s : task.subtarefas()) {
                    if (s.estado() == Estado.PENDENTE && concluidas.containsAll(s.dependeDe())) {
                        return Optional.of(new Proxima(projeto, task, s));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Registra o estado vindo de uma execução. Devolve vazio se, nesse meio-tempo, o usuário excluiu a
     * subtarefa, a Task ou o projeto.
     */
    public synchronized Optional<Projeto> marcar(String projetoId, String taskId, String subtarefaId,
                                                 Estado estado, String resultado) {
        return alterarSePossivel(projetoId, taskId, task -> task.comSubtarefas(task.subtarefas().stream()
                .map(s -> s.id().equals(subtarefaId) ? s.comEstado(estado, resultado) : s)
                .toList()));
    }

    /** Guarda a sessão do agente da Task, para as próximas subtarefas continuarem a mesma conversa. */
    public synchronized Optional<Projeto> definirSessao(String projetoId, String taskId, String sessao) {
        return alterarSePossivel(projetoId, taskId, task -> task.comSessao(sessao));
    }

    /** Acrescenta uma linha ao log da Task, guardado na pasta dela. */
    public void registrarLog(String projetoId, String taskId, String linha) {
        repositorio.registrarLog(projetoId, taskId, linha);
    }

    private Optional<Projeto> alterarSePossivel(String projetoId, String taskId, UnaryOperator<Task> alteracao) {
        Projeto atual = projetos.get(projetoId);
        if (atual == null) {
            return Optional.empty();
        }
        List<Task> tasks = new ArrayList<>(atual.tasks());
        int indice = indiceDaTask(tasks, taskId);
        if (indice < 0) {
            return Optional.empty();
        }
        tasks.set(indice, alteracao.apply(tasks.get(indice)));
        Projeto alterado = gravar(atual.comTasks(tasks));
        ouvintes.forEach(o -> o.accept(alterado));
        return Optional.of(alterado);
    }

    /** Remove a subtarefa e as dependências que apontavam para ela. */
    public synchronized Projeto excluirSubtarefa(Projeto projeto, Task task, Subtarefa subtarefa) {
        return alterarSubtarefas(projeto, task, subtarefas -> subtarefas.stream()
                .filter(s -> !s.id().equals(subtarefa.id()))
                .map(s -> s.comDependencias(s.dependeDe().stream().filter(d -> !d.equals(subtarefa.id())).toList()))
                .toList());
    }

    /**
     * Amarra ou desamarra duas subtarefas: a que está mais abaixo na lista passa a depender
     * (ou deixa de depender) da que está mais acima.
     */
    public synchronized Projeto alternarDependencia(Projeto projeto, Task task, Subtarefa uma, Subtarefa outra) {
        if (uma.id().equals(outra.id())) {
            return atual(projeto);
        }
        return alterarSubtarefas(projeto, task, subtarefas -> {
            int a = indiceDaSubtarefa(subtarefas, uma.id());
            int b = indiceDaSubtarefa(subtarefas, outra.id());
            if (a < 0 || b < 0) {
                throw new IllegalArgumentException("Subtarefa não encontrada.");
            }
            Subtarefa anterior = subtarefas.get(Math.min(a, b));
            Subtarefa posterior = subtarefas.get(Math.max(a, b));
            List<String> dependencias = new ArrayList<>(posterior.dependeDe());
            if (!dependencias.remove(anterior.id())) {
                dependencias.add(anterior.id());
            }
            List<Subtarefa> novas = new ArrayList<>(subtarefas);
            novas.set(Math.max(a, b), posterior.comDependencias(dependencias));
            return novas;
        });
    }

    private Projeto alterarSubtarefas(Projeto projeto, Task task, UnaryOperator<List<Subtarefa>> alteracao) {
        Projeto atual = atual(projeto);
        List<Task> tasks = new ArrayList<>(atual.tasks());
        int indice = indiceDaTask(tasks, task.id());
        if (indice < 0) {
            throw new IllegalArgumentException("Task não encontrada.");
        }
        List<Subtarefa> subtarefas = alteracao.apply(tasks.get(indice).subtarefas());
        validarDependencias(subtarefas);
        tasks.set(indice, tasks.get(indice).comSubtarefas(subtarefas));
        return gravar(atual.comTasks(tasks));
    }

    /** As dependências precisam apontar para subtarefas da mesma Task e não podem formar um ciclo. */
    static void validarDependencias(List<Subtarefa> subtarefas) {
        Map<String, Subtarefa> porId = new HashMap<>();
        subtarefas.forEach(s -> porId.put(s.id(), s));
        for (Subtarefa s : subtarefas) {
            for (String dependencia : s.dependeDe()) {
                if (!porId.containsKey(dependencia)) {
                    throw new IllegalArgumentException("\"" + s.titulo() + "\" depende de uma subtarefa que não existe.");
                }
            }
        }
        Set<String> concluidas = new HashSet<>();
        for (Subtarefa s : subtarefas) {
            if (temCiclo(s, porId, new HashSet<>(), concluidas)) {
                throw new IllegalArgumentException("As dependências de \"" + s.titulo() + "\" formam um ciclo.");
            }
        }
    }

    private static boolean temCiclo(Subtarefa s, Map<String, Subtarefa> porId, Set<String> caminho, Set<String> concluidas) {
        if (concluidas.contains(s.id())) {
            return false;
        }
        if (!caminho.add(s.id())) {
            return true;
        }
        for (String dependencia : s.dependeDe()) {
            if (temCiclo(porId.get(dependencia), porId, caminho, concluidas)) {
                return true;
            }
        }
        caminho.remove(s.id());
        concluidas.add(s.id());
        return false;
    }

    private Projeto atual(Projeto projeto) {
        Projeto atual = projetos.get(projeto.id());
        if (atual == null) {
            throw new IllegalArgumentException("Projeto não encontrado.");
        }
        return atual;
    }

    private Projeto gravar(Projeto projeto) {
        repositorio.salvar(projeto);
        projetos.put(projeto.id(), projeto);
        return projeto;
    }

    private static int indiceDaTask(List<Task> tasks, String id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static int indiceDaSubtarefa(List<Subtarefa> subtarefas, String id) {
        for (int i = 0; i < subtarefas.size(); i++) {
            if (subtarefas.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static void exigir(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private static boolean pastaExiste(String pasta) {
        try {
            return Files.isDirectory(Path.of(pasta));
        } catch (InvalidPathException e) {
            return false;
        }
    }
}
