package io.github.luanfrombrazil.overtimedispatcher.service.scheduler;

import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilaDeTasksTest {

    private static Task task(String titulo, Estado estado) {
        return new Task(titulo, null, estado, List.of());
    }

    @Test
    void moverTrocaAPosicaoERespeitaAsPontas() {
        Task a = task("A", Estado.PENDENTE);
        Task b = task("B", Estado.PENDENTE);
        Task c = task("C", Estado.PENDENTE);
        List<Task> tasks = new ArrayList<>(List.of(a, b, c));

        assertEquals(0, FilaDeTasks.mover(tasks, 2, -2));
        assertEquals(List.of(c, a, b), tasks);
        assertEquals(0, FilaDeTasks.mover(tasks, 0, -1));
        assertEquals(List.of(c, a, b), tasks);
        assertEquals(2, FilaDeTasks.mover(tasks, 1, 5));
        assertEquals(List.of(c, b, a), tasks);
    }

    @Test
    void proximaSegueAOrdemDeExibicao() {
        Task feita = task("Feita", Estado.CONCLUIDO);
        Task travada = task("Travada", Estado.ATENCAO);
        Task segunda = task("Segunda", Estado.PENDENTE);
        Task primeira = task("Primeira", Estado.PENDENTE);
        List<Task> tasks = new ArrayList<>(List.of(feita, travada, segunda, primeira));

        assertEquals(segunda, FilaDeTasks.proxima(tasks).orElseThrow());

        FilaDeTasks.mover(tasks, 3, -3);
        assertEquals(primeira, FilaDeTasks.proxima(tasks).orElseThrow());
    }

    @Test
    void semTaskExecutavelNaoHaProxima() {
        assertTrue(FilaDeTasks.proxima(List.of(task("Sugestão", Estado.ORACULO))).isEmpty());
    }
}
