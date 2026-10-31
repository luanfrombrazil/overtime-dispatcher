package io.github.luanfrombrazil.overtimedispatcher.model;

/**
 * Tarefa de uma Trilha, planejada em blocos.
 */
public record TarefaPessoal(String titulo, String detalhe, Estado estado,
                            int blocosFeitos, int blocosNaAgenda, int blocosTotal, String andamento) {
}
