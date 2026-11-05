package io.github.luanfrombrazil.overtimer.service;

import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProtocolos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda os protocolos em memória e salva cada mudança. Usado pela tela e pelo agendador ao mesmo tempo, por isso as
 * operações são sincronizadas. Erros de validação lançam {@link IllegalArgumentException} com uma mensagem para
 * mostrar ao usuário.
 */
public class ProtocoloService {

    private final RepositorioProtocolos repositorio;
    private final Map<String, Protocolo> protocolos = new HashMap<>();

    public ProtocoloService(RepositorioProtocolos repositorio) {
        this.repositorio = repositorio;
        for (Protocolo p : repositorio.carregar()) {
            protocolos.put(p.id(), p);
        }
    }

    /** Protocolos em ordem alfabética. */
    public synchronized List<Protocolo> protocolos() {
        return protocolos.values().stream()
                .sorted(Comparator.comparing(Protocolo::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized Optional<Protocolo> buscar(String id) {
        return Optional.ofNullable(protocolos.get(id));
    }

    /** Cria o protocolo ou substitui o de mesmo id. */
    public synchronized Protocolo salvar(Protocolo protocolo) {
        if (protocolo.nome() == null || protocolo.nome().isBlank()) {
            throw new IllegalArgumentException("Dê um nome ao protocolo.");
        }
        repositorio.salvar(protocolo);
        protocolos.put(protocolo.id(), protocolo);
        return protocolo;
    }

    public synchronized void excluir(String id) {
        repositorio.excluir(id);
        protocolos.remove(id);
    }
}
