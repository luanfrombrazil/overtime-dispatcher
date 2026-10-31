package io.github.luanfrombrazil.overtimedispatcher.service.scheduler;

import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;

import java.util.List;
import java.util.Optional;

/**
 * Ordem de execução das Tasks de um projeto: é a ordem da lista, a mesma mostrada na tela.
 */
public final class FilaDeTasks {

    private FilaDeTasks() {
    }

    /**
     * Move a Task da posição {@code indice} para {@code indice + deslocamento}, limitado às pontas da lista.
     *
     * @return a nova posição da Task
     */
    public static int mover(List<Task> tasks, int indice, int deslocamento) {
        int destino = Math.max(0, Math.min(tasks.size() - 1, indice + deslocamento));
        if (destino != indice) {
            tasks.add(destino, tasks.remove(indice));
        }
        return destino;
    }

    /**
     * Próxima Task a entregar a um agente: a primeira, na ordem de exibição, que pode rodar sozinha.
     * Pula as concluídas, as que precisam de você, as que falharam e as sugestões do Oráculo ainda não aceitas.
     */
    public static Optional<Task> proxima(List<Task> tasks) {
        return tasks.stream()
                .filter(t -> t.estado() == Estado.EM_ANDAMENTO || t.estado() == Estado.PENDENTE)
                .findFirst();
    }
}
