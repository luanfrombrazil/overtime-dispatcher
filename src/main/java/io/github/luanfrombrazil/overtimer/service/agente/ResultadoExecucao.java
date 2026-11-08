package io.github.luanfrombrazil.overtimer.service.agente;

/**
 * Como terminou uma execução.
 *
 * @param resposta resposta final do agente, ou o motivo da falha
 * @param sessao   id da sessão que o agente usou, ou null se ela não chegou a ser criada
 */
public record ResultadoExecucao(boolean sucesso, String resposta, String sessao) {

    public static ResultadoExecucao falha(String motivo) {
        return new ResultadoExecucao(false, motivo, null);
    }
}
