package io.github.luanfrombrazil.overtimer.model;

import java.util.List;
import java.util.UUID;

/**
 * Trabalho para um agente. A Task traz as próprias instruções e pode ser executada inteira; as subtarefas são
 * opcionais e quebram o trabalho em passos menores (D-19).
 *
 * @param subtarefas     passos da Task, na ordem de execução; vazia quando a Task é executada inteira
 * @param sessao         id da sessão do agente compartilhada pelas execuções, ou null antes da primeira
 * @param execucao       estado da Task executada inteira; só vale enquanto ela não tem subtarefas, exceto
 *                       {@link Estado#RASCUNHO}, que marca a Task toda como rascunho
 * @param resultado      resposta do agente na última execução da Task inteira, ou o motivo da falha
 * @param direcionamento perguntas do agente à Task executada inteira, como em {@link Subtarefa#direcionamento()}
 */
public record Task(String id, String titulo, String descricao, Provedor provedor, Esforco esforco,
                   Origem origem, List<Subtarefa> subtarefas, String sessao, Estado execucao, String resultado,
                   Direcionamento direcionamento) {

    public Task {
        subtarefas = subtarefas == null ? List.of() : List.copyOf(subtarefas);
        execucao = execucao == null ? Estado.PENDENTE : execucao;
    }

    public static Task nova(String titulo, String descricao, Provedor provedor, Esforco esforco) {
        return new Task(UUID.randomUUID().toString(), titulo, descricao, provedor, esforco, Origem.USUARIO,
                List.of(), null, Estado.PENDENTE, null, null);
    }

    public Task comSubtarefas(List<Subtarefa> subtarefas) {
        return new Task(id, titulo, descricao, provedor, esforco, origem, subtarefas, sessao, execucao, resultado,
                direcionamento);
    }

    public Task comSessao(String sessao) {
        return new Task(id, titulo, descricao, provedor, esforco, origem, subtarefas, sessao, execucao, resultado,
                direcionamento);
    }

    /** Muda o estado e o resultado da Task executada inteira, mantendo o direcionamento. */
    public Task comExecucao(Estado execucao, String resultado) {
        return new Task(id, titulo, descricao, provedor, esforco, origem, subtarefas, sessao, execucao, resultado,
                direcionamento);
    }

    public Task comDirecionamento(Direcionamento direcionamento) {
        return new Task(id, titulo, descricao, provedor, esforco, origem, subtarefas, sessao, execucao, resultado,
                direcionamento);
    }

    /**
     * Se é um rascunho: marcado por {@link Estado#RASCUNHO} na execução, com ou sem subtarefas. O rascunho não é
     * executado.
     */
    public boolean rascunho() {
        return execucao == Estado.RASCUNHO;
    }

    /** Se é executada inteira, sem subtarefas. */
    public boolean inteira() {
        return subtarefas.isEmpty();
    }

    /**
     * Se a Task terminou: a executada inteira concluída ou todas as subtarefas concluídas. A Task finalizada vai
     * para a seção de finalizadas e não pode mais ser editada nem mudar de ordem.
     */
    public boolean finalizada() {
        return origem != Origem.ORACULO && estado() == Estado.CONCLUIDO;
    }

    /**
     * Mensagem de conclusão: a resposta do agente à Task inteira ou, quando ela tem subtarefas, à última delas,
     * que fecha a sessão compartilhada. Null se não houver.
     */
    public String conclusao() {
        return inteira() ? resultado : subtarefas.getLast().resultado();
    }

    /**
     * Tasks do Oráculo aparecem em roxo; o rascunho e a Task executada inteira têm o próprio estado; as demais herdam o
     * estado das subtarefas.
     */
    public Estado estado() {
        if (origem == Origem.ORACULO) {
            return Estado.ORACULO;
        }
        if (rascunho() || inteira()) {
            return execucao;
        }
        return Estado.resumir(subtarefas.stream().map(Subtarefa::estado).toList());
    }

    /**
     * Se a Task executada inteira parou com perguntas sem resposta, ou se uma das subtarefas parou. Enquanto isso, a
     * Task inteira fica fora da fila: as subtarefas compartilham a sessão do agente e seguem em ordem.
     */
    public boolean aguardaDirecionamento() {
        return inteira() ? Estado.aguardaDirecionamento(execucao, direcionamento)
                : subtarefas.stream().anyMatch(Subtarefa::aguardaDirecionamento);
    }

    /** Subtarefas esperando a sua verificação manual. */
    public long verificacoes() {
        return subtarefas.stream().filter(s -> s.estado() == Estado.ATENCAO && !s.aguardaDirecionamento()).count();
    }

    /** Itens parados com perguntas sem resposta: as subtarefas ou, se ela roda inteira, a própria Task. */
    public long direcionamentos() {
        return inteira() ? (aguardaDirecionamento() ? 1 : 0)
                : subtarefas.stream().filter(Subtarefa::aguardaDirecionamento).count();
    }

    /** Texto curto sob o título. */
    public String detalhe() {
        return switch (estado()) {
            case ATENCAO -> Textos.pendencias(verificacoes(), direcionamentos());
            case ERRO -> "Falhou";
            default -> null;
        };
    }
}
