package io.github.luanfrombrazil.overtimer.service;

import io.github.luanfrombrazil.overtimer.model.Direcionamento;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Origem;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProjetos;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
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

    /**
     * Trabalho pronto para ir a um agente: uma subtarefa ou, quando {@code subtarefa} é null, a Task inteira.
     */
    public record Proxima(Projeto projeto, Task task, Subtarefa subtarefa) {
    }

    private final RepositorioProjetos repositorio;
    private final Map<String, Projeto> projetos = new HashMap<>();
    private final List<Consumer<Projeto>> ouvintes = new CopyOnWriteArrayList<>();
    private final List<Consumer<Task>> aoCriar = new CopyOnWriteArrayList<>();
    private final Set<String> seguradas = ConcurrentHashMap.newKeySet();

    /** Carrega os projetos. O que estava em andamento quando a app fechou volta para a fila. */
    public ProjetoService(RepositorioProjetos repositorio) {
        this.repositorio = repositorio;
        for (Projeto p : repositorio.carregar()) {
            Projeto recuperado = p.comTasks(p.tasks().stream()
                    .map(t -> t.execucao() == Estado.EM_ANDAMENTO ? t.comExecucao(Estado.PENDENTE, t.resultado()) : t)
                    .map(t -> t.comSubtarefas(t.subtarefas().stream()
                            .map(s -> s.estado() == Estado.EM_ANDAMENTO ? s.comEstado(Estado.PENDENTE, s.resultado()) : s)
                            .toList()))
                    .toList());
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

    /** Avisado (na thread de quem criou) quando uma Task entra na fila: nova ou rascunho criado de fato. */
    public void aoCriarTask(Consumer<Task> ouvinte) {
        aoCriar.add(ouvinte);
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
        if (projeto.imagem() != null) {
            String nome = projeto.imagem().toLowerCase(java.util.Locale.ROOT);
            if (!nome.endsWith(".png") && !nome.endsWith(".jpg") && !nome.endsWith(".jpeg")) {
                throw new IllegalArgumentException("A imagem do projeto deve ser .png ou .jpg.");
            }
            if (!java.nio.file.Files.isRegularFile(Path.of(projeto.imagem()))) {
                throw new IllegalArgumentException("A imagem \"" + projeto.imagem() + "\" não existe.");
            }
        }
        Projeto existente = projetos.get(projeto.id());
        return gravar(existente == null ? projeto : projeto.comTasks(existente.tasks()));
    }

    public synchronized void excluir(Projeto projeto) {
        repositorio.excluir(projeto.id());
        projetos.remove(projeto.id());
    }

    /**
     * Adiciona a Task ao fim do projeto, ou substitui a de mesmo id mantendo as subtarefas e a sessão. Salvar um
     * rascunho o cria de fato: ele entra na fila.
     */
    public synchronized Projeto salvarTask(Projeto projeto, Task task) {
        exigir(task.titulo(), "Dê um título à task.");
        Projeto atual = atual(projeto);
        List<Task> tasks = new ArrayList<>(atual.tasks());
        int indice = indiceDaTask(tasks, task.id());
        boolean criada;
        if (indice < 0) {
            criada = true;
            tasks.add(task.rascunho() ? task.comExecucao(Estado.PENDENTE, null) : task);
        } else {
            Task antiga = tasks.get(indice);
            exigirAberta(antiga);
            criada = antiga.rascunho();
            tasks.set(indice, task.comSubtarefas(antiga.subtarefas()).comSessao(antiga.sessao())
                    .comExecucao(antiga.rascunho() ? Estado.PENDENTE : antiga.execucao(), antiga.resultado())
                    .comDirecionamento(antiga.direcionamento()));
        }
        Projeto gravado = gravar(atual.comTasks(tasks));
        if (criada) {
            aoCriar.forEach(o -> o.accept(task));
        }
        return gravado;
    }

    /**
     * Guarda a Task como rascunho, que não é executado: adiciona ao fim do projeto ou substitui o rascunho de mesmo
     * id, mantendo as subtarefas. Uma Task que já foi criada não volta a ser rascunho.
     */
    public synchronized Projeto salvarRascunho(Projeto projeto, Task task) {
        Projeto atual = atual(projeto);
        List<Task> tasks = new ArrayList<>(atual.tasks());
        int indice = indiceDaTask(tasks, task.id());
        Task rascunho = task.comExecucao(Estado.RASCUNHO, null);
        if (indice < 0) {
            tasks.add(rascunho.comSubtarefas(List.of()).comSessao(null));
        } else {
            Task antiga = tasks.get(indice);
            if (!antiga.rascunho()) {
                throw new IllegalArgumentException("\"" + antiga.titulo() + "\" já foi criada e não é mais um rascunho.");
            }
            tasks.set(indice, rascunho.comSubtarefas(antiga.subtarefas()).comSessao(antiga.sessao()));
        }
        return gravar(atual.comTasks(tasks));
    }

    public synchronized Projeto excluirTask(Projeto projeto, Task task) {
        Projeto atual = atual(projeto);
        return gravar(atual.comTasks(atual.tasks().stream().filter(t -> !t.id().equals(task.id())).toList()));
    }

    /**
     * Move a Task {@code deslocamento} posições (-1 sobe, +1 desce) entre as Tasks ainda não finalizadas, limitado
     * às pontas. As finalizadas ficam onde estão e não mudam de ordem. As Tasks rodam na ordem da lista, a mesma
     * mostrada na tela.
     */
    public synchronized Projeto moverTask(Projeto projeto, Task task, int deslocamento) {
        Projeto atual = atual(projeto);
        int geral = indiceDaTask(atual.tasks(), task.id());
        if (geral < 0) {
            throw new IllegalArgumentException("Task não encontrada.");
        }
        exigirAberta(atual.tasks().get(geral));
        List<Task> abertas = new ArrayList<>(atual.tasks().stream().filter(t -> !t.finalizada()).toList());
        int indice = indiceDaTask(abertas, task.id());
        int destino = Math.max(0, Math.min(abertas.size() - 1, indice + deslocamento));
        if (destino == indice) {
            return atual;
        }
        abertas.add(destino, abertas.remove(indice));
        List<Task> tasks = new ArrayList<>();
        int proxima = 0;
        for (Task t : atual.tasks()) {
            tasks.add(t.finalizada() ? t : abertas.get(proxima++));
        }
        return gravar(atual.comTasks(tasks));
    }

    /**
     * Move a subtarefa {@code deslocamento} posições dentro da Task, limitado às pontas da lista. Uma subtarefa
     * precisa continuar abaixo das que ela espera. Subtarefas finalizadas não mudam de ordem.
     */
    public synchronized Projeto moverSubtarefa(Projeto projeto, Task task, Subtarefa subtarefa, int deslocamento) {
        return alterarSubtarefas(projeto, task, subtarefas -> {
            List<Subtarefa> novas = new ArrayList<>(subtarefas);
            int indice = indiceDaSubtarefa(novas, subtarefa.id());
            if (indice < 0) {
                throw new IllegalArgumentException("Subtarefa não encontrada.");
            }
            exigirAberta(novas.get(indice));
            int destino = Math.max(0, Math.min(novas.size() - 1, indice + deslocamento));
            novas.add(destino, novas.remove(indice));
            Set<String> acima = new HashSet<>();
            for (Subtarefa s : novas) {
                for (String dependencia : s.dependeDe()) {
                    if (!acima.contains(dependencia)) {
                        String espera = novas.get(indiceDaSubtarefa(novas, dependencia)).titulo();
                        throw new IllegalArgumentException("\"" + s.titulo() + "\" espera \"" + espera
                                + "\" e precisa ficar abaixo dela. Solte o fio antes de mudar a ordem.");
                    }
                }
                acima.add(s.id());
            }
            return novas;
        });
    }

    /**
     * Adiciona a subtarefa ao fim da Task, ou substitui a de mesmo id mantendo o estado e o resultado. A Task
     * finalizada não recebe subtarefas novas, e a subtarefa finalizada não pode ser editada. Salvar um rascunho o
     * cria de fato: ele entra na fila.
     */
    public synchronized Projeto salvarSubtarefa(Projeto projeto, Task task, Subtarefa subtarefa) {
        exigir(subtarefa.titulo(), "Dê um título à subtarefa.");
        exigirAceitaSubtarefa(projeto, task);
        return alterarSubtarefas(projeto, task, subtarefas -> {
            List<Subtarefa> novas = new ArrayList<>(subtarefas);
            int indice = indiceDaSubtarefa(novas, subtarefa.id());
            if (indice < 0) {
                novas.add(subtarefa.comEstado(Estado.PENDENTE, null));
            } else {
                Subtarefa antiga = novas.get(indice);
                exigirAberta(antiga);
                novas.set(indice, antiga.rascunho() ? subtarefa.comEstado(Estado.PENDENTE, null)
                        : subtarefa.comEstado(antiga.estado(), antiga.resultado())
                        .comDirecionamento(antiga.direcionamento()));
            }
            return novas;
        });
    }

    /**
     * Guarda a subtarefa como rascunho, que não é executado: adiciona ao fim da Task ou substitui o rascunho de
     * mesmo id. Uma subtarefa que já foi criada não volta a ser rascunho.
     */
    public synchronized Projeto salvarRascunho(Projeto projeto, Task task, Subtarefa subtarefa) {
        exigirAceitaSubtarefa(projeto, task);
        return alterarSubtarefas(projeto, task, subtarefas -> {
            List<Subtarefa> novas = new ArrayList<>(subtarefas);
            int indice = indiceDaSubtarefa(novas, subtarefa.id());
            Subtarefa rascunho = subtarefa.comEstado(Estado.RASCUNHO, null);
            if (indice < 0) {
                novas.add(rascunho);
            } else if (novas.get(indice).rascunho()) {
                novas.set(indice, rascunho);
            } else {
                throw new IllegalArgumentException("\"" + novas.get(indice).titulo()
                        + "\" já foi criada e não é mais um rascunho.");
            }
            return novas;
        });
    }

    /**
     * Segura a Task entre as abertas enquanto o usuário escreve uma subtarefa nova nela: mesmo que as subtarefas
     * dela terminem nesse meio-tempo, ela ainda aceita a nova. Desfazer com {@link #soltar}.
     */
    public void segurarAberta(Task task) {
        seguradas.add(task.id());
    }

    public void soltar(Task task) {
        seguradas.remove(task.id());
    }

    /** Se a Task está segura entre as abertas por {@link #segurarAberta}. */
    public boolean seguraAberta(Task task) {
        return seguradas.contains(task.id());
    }

    private void exigirAceitaSubtarefa(Projeto projeto, Task task) {
        Task atual = atual(projeto).tasks().stream().filter(t -> t.id().equals(task.id())).findFirst().orElse(task);
        if (atual.inteira() && atual.execucao() == Estado.EM_ANDAMENTO) {
            throw new IllegalArgumentException("A task está em andamento; espere o agente terminar.");
        }
        if (!seguraAberta(atual)) {
            exigirAberta(atual);
        }
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

    /** Decisão do usuário sobre a Task executada inteira, como em {@link #definirEstado(Projeto, Task, Subtarefa, Estado)}. */
    public synchronized Projeto definirEstado(Projeto projeto, Task task, Estado estado) {
        Projeto atual = atual(projeto);
        List<Task> tasks = new ArrayList<>(atual.tasks());
        int indice = indiceDaTask(tasks, task.id());
        if (indice < 0) {
            throw new IllegalArgumentException("Task não encontrada.");
        }
        Task antiga = tasks.get(indice);
        if (antiga.execucao() == Estado.EM_ANDAMENTO) {
            throw new IllegalArgumentException("A task está em andamento; espere o agente terminar.");
        }
        tasks.set(indice, antiga.comExecucao(estado, antiga.resultado()));
        return gravar(atual.comTasks(tasks));
    }

    /** Devolve à fila todas as subtarefas da Task que falharam, ou a própria Task, se ela roda inteira. */
    public synchronized Projeto tentarDeNovo(Projeto projeto, Task task) {
        Task atual = atual(projeto).tasks().stream().filter(t -> t.id().equals(task.id())).findFirst().orElse(task);
        if (atual.inteira()) {
            return atual.execucao() == Estado.ERRO ? definirEstado(projeto, task, Estado.PENDENTE) : atual(projeto);
        }
        return alterarSubtarefas(projeto, task, subtarefas -> subtarefas.stream()
                .map(s -> s.estado() == Estado.ERRO ? s.comEstado(Estado.PENDENTE, s.resultado()) : s)
                .toList());
    }

    /**
     * Próximo trabalho na fila, seguindo a ordem dos projetos, das Tasks e das subtarefas: a primeira subtarefa
     * cujas dependências já foram concluídas ou, se a Task não tem subtarefas, a Task inteira. Tasks do Oráculo
     * esperam ser aceitas, e rascunhos (de Task ou de subtarefa) esperam ser criados. A Task com perguntas sem
     * resposta fica parada até você responder; a fila segue para as outras.
     */
    public synchronized Optional<Proxima> proxima() {
        return proxima(task -> true);
    }

    /** Como {@link #proxima()}, pulando as Tasks que {@code liberada} ainda segura. */
    public synchronized Optional<Proxima> proxima(Predicate<Task> liberada) {
        for (Projeto projeto : projetos()) {
            for (Task task : projeto.tasks()) {
                if (task.origem() == Origem.ORACULO || task.rascunho() || task.aguardaDirecionamento()
                        || !liberada.test(task)) {
                    continue;
                }
                if (task.inteira()) {
                    if (task.execucao() == Estado.PENDENTE) {
                        return Optional.of(new Proxima(projeto, task, null));
                    }
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
     * Registra o estado vindo de uma execução, da subtarefa ou, com {@code subtarefaId} null, da Task inteira.
     * Devolve vazio se, nesse meio-tempo, o usuário excluiu a subtarefa, a Task ou o projeto.
     */
    public synchronized Optional<Projeto> marcar(String projetoId, String taskId, String subtarefaId,
                                                 Estado estado, String resultado) {
        if (subtarefaId == null) {
            return alterarSePossivel(projetoId, taskId, task -> task.comExecucao(estado, resultado));
        }
        return alterarSePossivel(projetoId, taskId, task -> task.comSubtarefas(task.subtarefas().stream()
                .map(s -> s.id().equals(subtarefaId) ? s.comEstado(estado, resultado) : s)
                .toList()));
    }

    /**
     * Como {@link #marcar(String, String, String, Estado, String)}, trocando também o direcionamento: as perguntas
     * novas do agente ou null, depois que ele recebeu as respostas.
     */
    public synchronized Optional<Projeto> marcar(String projetoId, String taskId, String subtarefaId,
                                                 Estado estado, String resultado, Direcionamento direcionamento) {
        if (subtarefaId == null) {
            return alterarSePossivel(projetoId, taskId,
                    task -> task.comExecucao(estado, resultado).comDirecionamento(direcionamento));
        }
        return alterarSePossivel(projetoId, taskId, task -> task.comSubtarefas(task.subtarefas().stream()
                .map(s -> s.id().equals(subtarefaId) ? s.comEstado(estado, resultado).comDirecionamento(direcionamento) : s)
                .toList()));
    }

    /**
     * Sua resposta às perguntas do agente, uma por pergunta e na ordem delas, à subtarefa ou, com {@code subtarefa}
     * null, à Task executada inteira. Ela volta para a fila e, na próxima execução, o agente recebe as respostas na
     * mesma sessão e continua de onde parou.
     */
    public synchronized Projeto responder(Projeto projeto, Task task, Subtarefa subtarefa, List<String> respostas) {
        Projeto atual = atual(projeto);
        List<Task> tasks = new ArrayList<>(atual.tasks());
        int indice = indiceDaTask(tasks, task.id());
        if (indice < 0) {
            throw new IllegalArgumentException("Task não encontrada.");
        }
        Task antiga = tasks.get(indice);
        if (subtarefa == null) {
            if (!antiga.inteira() || !Estado.aguardaDirecionamento(antiga.execucao(), antiga.direcionamento())) {
                throw new IllegalArgumentException("A task não está esperando direcionamento.");
            }
            tasks.set(indice, antiga.comExecucao(Estado.PENDENTE, antiga.resultado())
                    .comDirecionamento(antiga.direcionamento().comRespostas(respostas)));
            return gravar(atual.comTasks(tasks));
        }
        Subtarefa s = antiga.subtarefas().stream().filter(x -> x.id().equals(subtarefa.id())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Subtarefa não encontrada."));
        if (!s.aguardaDirecionamento()) {
            throw new IllegalArgumentException("A subtarefa não está esperando direcionamento.");
        }
        Subtarefa respondida = s.comEstado(Estado.PENDENTE, s.resultado())
                .comDirecionamento(s.direcionamento().comRespostas(respostas));
        tasks.set(indice, antiga.comSubtarefas(antiga.subtarefas().stream()
                .map(x -> x.id().equals(s.id()) ? respondida : x).toList()));
        return gravar(atual.comTasks(tasks));
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

    /** Remove a subtarefa e as dependências que apontavam para ela. A subtarefa finalizada não pode ser excluída. */
    public synchronized Projeto excluirSubtarefa(Projeto projeto, Task task, Subtarefa subtarefa) {
        atual(projeto).tasks().stream().filter(t -> t.id().equals(task.id()))
                .flatMap(t -> t.subtarefas().stream()).filter(s -> s.id().equals(subtarefa.id()))
                .findFirst().ifPresent(ProjetoService::exigirAberta);
        return alterarSubtarefas(projeto, task, subtarefas -> subtarefas.stream()
                .filter(s -> !s.id().equals(subtarefa.id()))
                .map(s -> s.comDependencias(s.dependeDe().stream().filter(d -> !d.equals(subtarefa.id())).toList()))
                .toList());
    }

    /**
     * Amarra ou desamarra duas subtarefas pela corda arrastada da {@code origem} até o {@code destino}: o destino
     * passa a depender da origem. Se a origem está abaixo, o destino desce para logo abaixo dela, levando junto as
     * subtarefas que esperam por ele no caminho. Se as duas já estavam amarradas, em qualquer sentido, a corda é
     * solta. A subtarefa que ganha ou perde a dependência, e as que mudam de lugar, não podem estar finalizadas.
     */
    public synchronized Projeto alternarDependencia(Projeto projeto, Task task, Subtarefa origem, Subtarefa destino) {
        if (origem.id().equals(destino.id())) {
            return atual(projeto);
        }
        return alterarSubtarefas(projeto, task, subtarefas -> {
            int o = indiceDaSubtarefa(subtarefas, origem.id());
            int d = indiceDaSubtarefa(subtarefas, destino.id());
            if (o < 0 || d < 0) {
                throw new IllegalArgumentException("Subtarefa não encontrada.");
            }
            Subtarefa de = subtarefas.get(o);
            Subtarefa para = subtarefas.get(d);
            List<Subtarefa> novas = new ArrayList<>(subtarefas);
            if (para.dependeDe().contains(de.id())) {
                exigirAberta(para);
                novas.set(d, semDependencia(para, de.id()));
                return novas;
            }
            if (de.dependeDe().contains(para.id())) {
                exigirAberta(de);
                novas.set(o, semDependencia(de, para.id()));
                return novas;
            }
            exigirAberta(para);
            List<String> dependencias = new ArrayList<>(para.dependeDe());
            dependencias.add(de.id());
            novas.set(d, para.comDependencias(dependencias));
            if (o < d) {
                return novas;
            }
            // A origem está abaixo: o destino e quem espera por ele até a origem descem para logo abaixo dela.
            Set<String> bloco = new HashSet<>();
            List<Subtarefa> movidas = new ArrayList<>();
            for (int i = d; i < o; i++) {
                Subtarefa s = novas.get(i);
                if (i == d || s.dependeDe().stream().anyMatch(bloco::contains)) {
                    exigirAberta(s);
                    bloco.add(s.id());
                    movidas.add(s);
                }
            }
            if (de.dependeDe().stream().anyMatch(bloco::contains)) {
                throw new IllegalArgumentException("\"" + de.titulo() + "\" já espera por \"" + para.titulo()
                        + "\"; amarrar ao contrário formaria um ciclo.");
            }
            novas.removeIf(s -> bloco.contains(s.id()));
            novas.addAll(indiceDaSubtarefa(novas, de.id()) + 1, movidas);
            return novas;
        });
    }

    private static Subtarefa semDependencia(Subtarefa subtarefa, String dependencia) {
        return subtarefa.comDependencias(subtarefa.dependeDe().stream().filter(id -> !id.equals(dependencia)).toList());
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
        Projeto gravado = repositorio.salvar(projeto);
        projetos.put(gravado.id(), gravado);
        return gravado;
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

    private static void exigirAberta(Task task) {
        if (task.finalizada()) {
            throw new IllegalArgumentException("\"" + task.titulo() + "\" foi finalizada e não pode mais ser alterada.");
        }
    }

    private static void exigirAberta(Subtarefa subtarefa) {
        if (subtarefa.finalizada()) {
            throw new IllegalArgumentException("\"" + subtarefa.titulo()
                    + "\" foi finalizada e não pode mais ser alterada.");
        }
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
