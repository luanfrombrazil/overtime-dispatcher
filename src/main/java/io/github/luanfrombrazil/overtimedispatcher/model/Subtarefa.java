package io.github.luanfrombrazil.overtimedispatcher.model;

import java.util.List;
import java.util.UUID;

/**
 * Passo pequeno de uma Task.
 *
 * @param dependeDe ids das subtarefas da mesma Task que precisam terminar antes desta
 * @param resultado resposta final do agente na última execução, ou o motivo da falha; null se nunca rodou
 */
public record Subtarefa(String id, String titulo, String instrucao, String criterioDeAceite,
                        boolean requerVerificacao, Estado estado, List<String> dependeDe, String resultado) {

    public Subtarefa {
        dependeDe = dependeDe == null ? List.of() : List.copyOf(dependeDe);
        estado = estado == null ? Estado.PENDENTE : estado;
    }

    public static Subtarefa nova(String titulo, String instrucao, String criterioDeAceite,
                                 boolean requerVerificacao, List<String> dependeDe) {
        return new Subtarefa(UUID.randomUUID().toString(), titulo, instrucao, criterioDeAceite,
                requerVerificacao, Estado.PENDENTE, dependeDe, null);
    }

    public Subtarefa comDependencias(List<String> dependeDe) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado);
    }

    public Subtarefa comEstado(Estado estado, String resultado) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado);
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
