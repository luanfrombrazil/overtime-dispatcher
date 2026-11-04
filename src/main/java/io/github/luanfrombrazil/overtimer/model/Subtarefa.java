package io.github.luanfrombrazil.overtimer.model;

import java.util.List;
import java.util.UUID;

/**
 * Passo pequeno e opcional de uma Task.
 *
 * @param dependeDe ids das subtarefas da mesma Task que precisam terminar antes desta
 * @param resultado resposta final do agente na última execução, ou o motivo da falha; null se nunca rodou
 * @param commit    se o agente faz commit ao terminar; ligado quando ausente (arquivos antigos)
 * @param push      se o agente faz push depois do commit; só vale com commit
 * @param esforco   esforço do agente nesta subtarefa; null (arquivos antigos) usa o da Task
 * @param modelo    modelo que o agente usa nesta subtarefa; {@link Modelo#PADRAO} quando ausente
 */
public record Subtarefa(String id, String titulo, String instrucao, String criterioDeAceite,
                        boolean requerVerificacao, Estado estado, List<String> dependeDe, String resultado,
                        Boolean commit, Boolean push, Esforco esforco, Modelo modelo) {

    public Subtarefa {
        dependeDe = dependeDe == null ? List.of() : List.copyOf(dependeDe);
        estado = estado == null ? Estado.PENDENTE : estado;
        commit = commit == null || commit;
        push = commit && push != null && push;
        modelo = modelo == null ? Modelo.PADRAO : modelo;
    }

    /** Subtarefa na fila, com commit ligado, push desligado, esforço médio e o modelo padrão do agente. */
    public static Subtarefa nova(String titulo, String instrucao, String criterioDeAceite,
                                 boolean requerVerificacao, List<String> dependeDe) {
        return new Subtarefa(UUID.randomUUID().toString(), titulo, instrucao, criterioDeAceite,
                requerVerificacao, Estado.PENDENTE, dependeDe, null, true, false, Esforco.MEDIUM, Modelo.PADRAO);
    }

    public Subtarefa comDependencias(List<String> dependeDe) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push, esforco, modelo);
    }

    public Subtarefa comEstado(Estado estado, String resultado) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push, esforco, modelo);
    }

    public Subtarefa comGit(boolean commit, boolean push) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push, esforco, modelo);
    }

    public Subtarefa comModelo(Esforco esforco, Modelo modelo) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push, esforco, modelo);
    }

    /** Esforço com que o agente roda esta subtarefa: o dela ou, se não tiver, o da Task. */
    public Esforco esforcoOu(Esforco daTask) {
        return esforco == null ? daTask : esforco;
    }

    /** Se a subtarefa foi concluída; daí em diante ela não pode mais ser editada, excluída nem mudar de ordem. */
    public boolean finalizada() {
        return estado == Estado.CONCLUIDO;
    }

    public boolean rascunho() {
        return estado == Estado.RASCUNHO;
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
