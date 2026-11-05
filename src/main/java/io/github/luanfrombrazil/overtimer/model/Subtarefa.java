package io.github.luanfrombrazil.overtimer.model;

import java.util.List;
import java.util.UUID;

/**
 * Passo pequeno e opcional de uma Task.
 *
 * @param dependeDe      ids das subtarefas da mesma Task que precisam terminar antes desta
 * @param resultado      resposta final do agente na última execução, ou o motivo da falha; null se nunca rodou
 * @param commit         se o agente faz commit ao terminar; ligado quando ausente (arquivos antigos)
 * @param push           se o agente faz push depois do commit; só vale com commit
 * @param esforco        esforço do agente nesta subtarefa; null (arquivos antigos) usa o da Task
 * @param modelo         modelo que o agente usa nesta subtarefa; {@link Modelo#PADRAO} quando ausente
 * @param direcionamento perguntas que o agente deixou ao parar: sem resposta, a subtarefa espera por você (amarela)
 *                       e segura a Task; respondidas, vão para o agente na próxima execução. Null se não há
 */
public record Subtarefa(String id, String titulo, String instrucao, String criterioDeAceite,
                        boolean requerVerificacao, Estado estado, List<String> dependeDe, String resultado,
                        Boolean commit, Boolean push, Esforco esforco, Modelo modelo,
                        Direcionamento direcionamento) {

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
                requerVerificacao, Estado.PENDENTE, dependeDe, null, true, false, Esforco.MEDIUM, Modelo.PADRAO,
                null);
    }

    public Subtarefa comDependencias(List<String> dependeDe) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push, esforco, modelo, direcionamento);
    }

    /** Muda o estado e o resultado, mantendo o direcionamento. */
    public Subtarefa comEstado(Estado estado, String resultado) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push, esforco, modelo, direcionamento);
    }

    public Subtarefa comGit(boolean commit, boolean push) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push, esforco, modelo, direcionamento);
    }

    public Subtarefa comModelo(Esforco esforco, Modelo modelo) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push, esforco, modelo, direcionamento);
    }

    public Subtarefa comDirecionamento(Direcionamento direcionamento) {
        return new Subtarefa(id, titulo, instrucao, criterioDeAceite, requerVerificacao, estado, dependeDe, resultado,
                commit, push, esforco, modelo, direcionamento);
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

    /** Se o agente parou com perguntas que ainda não foram respondidas. */
    public boolean aguardaDirecionamento() {
        return Estado.aguardaDirecionamento(estado, direcionamento);
    }

    /** Texto curto sob o título. */
    public String detalhe() {
        return switch (estado) {
            case ERRO -> "Falhou";
            case ATENCAO -> aguardaDirecionamento() ? "Direcionamento" : "Verificação";
            default -> requerVerificacao ? "Verificação" : null;
        };
    }
}
