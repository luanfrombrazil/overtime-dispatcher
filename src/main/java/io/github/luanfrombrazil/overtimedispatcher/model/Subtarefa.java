package io.github.luanfrombrazil.overtimedispatcher.model;

import java.util.List;
import java.util.UUID;

/**
 * Passo pequeno e opcional de uma Task.
 *
 * @param dependeDe ids das subtarefas da mesma Task que precisam terminar antes desta
 * @param resultado resposta final do agente na última execução, ou o motivo da falha; null se nunca rodou
 * @param commit    se o agente faz commit ao terminar; ligado quando ausente (arquivos antigos)
 * @param push      se o agente faz push depois do commit; só vale com commit
 */
public record Subtarefa(String id, String titulo, String instrucao, String criterioDeAceite,
                        boolean requerVerificacao, Estado estado, List<String> dependeDe, String resultado,
                        Boolean commit, Boolean push) {

    public Subtarefa {
        dependeDe = dependeDe == null ? List.of() : List.copyOf(dependeDe);
        estado = estado == null ? Estado.PENDENTE : estado;
        commit = commit == null || commit;
        push = commit && push != null && push;
    }

    /** Subtarefa na fila, com commit ligado e push desligado. */
    public static Subtarefa nova(String titulo, String instrucao, String criterioDeAceite,
                                 boolean requerVerificacao, List<String> dependeDe) {
        return new Subtarefa(UUID.randomUUID().toString(), titulo, instrucao, criterioDeAceite,
                requerVerificacao, Estado.PENDENTE, dependeDe, null, true, false);
    }

    public Subtarefa comDependencias(List<String> dependeDe) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push);
    }

    public Subtarefa comEstado(Estado estado, String resultado) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push);
    }

    public Subtarefa comGit(boolean commit, boolean push) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push);
    }

    /** Texto curto sob o título. */
    public String detalhe() {
        return switch (estado) {
            case ERRO -> "Falhou";
            case ATENCAO -> "Verificação";
            default -> requerVerificacao ? "Verificação" : null;
        };
    }
}
