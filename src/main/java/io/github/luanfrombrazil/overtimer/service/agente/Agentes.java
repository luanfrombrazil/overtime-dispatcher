package io.github.luanfrombrazil.overtimer.service.agente;

import io.github.luanfrombrazil.overtimer.model.Provedor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Os agentes que a app sabe executar. Codex e Gemini entram aqui quando os seus CLIs forem integrados.
 */
public class Agentes {

    private final Map<Provedor, Agente> porProvedor = new LinkedHashMap<>();

    /** @param agentes em ordem de preferência para as Tasks com provedor automático */
    public Agentes(List<Agente> agentes) {
        agentes.forEach(a -> porProvedor.put(a.provedor(), a));
    }

    public static Agentes instalados() {
        return new Agentes(List.of(new ClaudeCodeAgente()));
    }

    /** O agente do provedor; para "Automático", o primeiro disponível. */
    public Optional<Agente> para(Provedor provedor) {
        if (provedor == null || provedor == Provedor.AUTOMATICO) {
            return porProvedor.values().stream().filter(Agente::disponivel).findFirst();
        }
        return Optional.ofNullable(porProvedor.get(provedor)).filter(Agente::disponivel);
    }

    public Collection<Agente> todos() {
        return porProvedor.values();
    }
}
