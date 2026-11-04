package io.github.luanfrombrazil.overtimedispatcher.model;

import java.util.List;
import java.util.UUID;

/**
 * Trabalho para um agente. A Task traz as próprias instruções e pode ser executada inteira; as subtarefas são
 * opcionais e quebram o trabalho em passos menores (D-19).
 *
 * @param subtarefas passos da Task, na ordem de execução; vazia quando a Task é executada inteira
 * @param sessao     id da sessão do agente compartilhada pelas execuções, ou null antes da primeira
 * @param execucao   estado da Task executada inteira; só vale enquanto ela não tem subtarefas, exceto
 *                   {@link Estado#RASCUNHO}, que marca a Task toda como rascunho
 * @param resultado  resposta do agente na última execução da Task inteira, ou o motivo da falha
 */
public record Task(String id, String titulo, String descricao, Provedor provedor, Esforco esforco,
                   Origem origem, List<Subtarefa> subtarefas, String sessao, Estado execucao, String resultado) {

    public Task {
        subtarefas = subtarefas == null ? List.of() : List.copyOf(subtarefas);
        execucao = execucao == null ? Estado.PENDENTE : execucao;
    }

    public static Task nova(String titulo, String descricao, Provedor provedor, Esforco esforco) {
        return new Task(UUID.randomUUID().toString(), titulo, descricao, provedor, esforco, Origem.USUARIO,
                List.of(), null, Estado.PENDENTE, null);
    }

    public Task comSubtarefas(List<Subtarefa> subtarefas) {
        return new Task(id, titulo, descricao, provedor, esforco, origem, subtarefas, sessao, execucao, resultado);
    }

    public Task comSessao(String sessao) {
        return new Task(id, titulo, descricao, provedor, esforco, origem, subtarefas, sessao, execucao, resultado);
    }

    public Task comExecucao(Estado execucao, String resultado) {
        return new Task(id, titulo, descricao, provedor, esforco, origem, subtarefas, sessao, execucao, resultado);
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

    public long verificacoes() {
        return subtarefas.stream().filter(s -> s.estado() == Estado.ATENCAO).count();
    }

    /** Texto curto sob o título. */
    public String detalhe() {
        return switch (estado()) {
            case ATENCAO -> Textos.verificacoes(verificacoes());
            case ERRO -> "Falhou";
            default -> null;
        };
    }
}
