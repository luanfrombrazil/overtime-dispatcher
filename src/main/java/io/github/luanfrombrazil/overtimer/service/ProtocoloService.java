package io.github.luanfrombrazil.overtimer.service;

import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProtocolos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lista, cria, altera, liga/desliga e exclui protocolos, salvando cada mudança.
 * <p>
 * Usado pela tela e pelo agendador ao mesmo tempo, por isso as operações são sincronizadas. Erros de validação
 * lançam {@link IllegalArgumentException} com uma mensagem para mostrar ao usuário.
 */
public class ProtocoloService {

    private final RepositorioProtocolos repositorio;
    private final Map<String, Protocolo> protocolos = new HashMap<>();
    private final List<Consumer<Protocolo>> ouvintes = new CopyOnWriteArrayList<>();

    public ProtocoloService(RepositorioProtocolos repositorio) {
        this.repositorio = repositorio;
        for (Protocolo p : repositorio.carregar()) {
            protocolos.put(p.id(), p);
        }
    }

    /**
     * Avisado (na thread de quem mudou, fora do bloqueio do serviço) quando um protocolo é salvo, ligado, desligado
     * ou excluído; na exclusão recebe o protocolo como estava.
     */
    public void aoMudar(Consumer<Protocolo> ouvinte) {
        ouvintes.add(ouvinte);
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
    public Protocolo salvar(Protocolo protocolo) {
        if (protocolo.nome() == null || protocolo.nome().isBlank()) {
            throw new IllegalArgumentException("Dê um nome ao protocolo.");
        }
        if (protocolo.passos() == null || protocolo.passos().isBlank()) {
            throw new IllegalArgumentException("Escreva o passo a passo do protocolo.");
        }
        synchronized (this) {
            gravar(protocolo);
        }
        avisar(protocolo);
        return protocolo;
    }

    /** Liga ou desliga o protocolo no agendador; vazio se ele não existe mais. */
    public Optional<Protocolo> definirAtivo(String id, boolean ativo) {
        Protocolo alterado;
        synchronized (this) {
            Protocolo atual = protocolos.get(id);
            if (atual == null) {
                return Optional.empty();
            }
            if (atual.ativo() == ativo) {
                return Optional.of(atual);
            }
            alterado = gravar(atual.comAtivo(ativo));
        }
        avisar(alterado);
        return Optional.of(alterado);
    }

    public void excluir(String id) {
        Protocolo removido;
        synchronized (this) {
            repositorio.excluir(id);
            removido = protocolos.remove(id);
        }
        if (removido != null) {
            avisar(removido);
        }
    }

    private Protocolo gravar(Protocolo protocolo) {
        repositorio.salvar(protocolo);
        protocolos.put(protocolo.id(), protocolo);
        return protocolo;
    }

    private void avisar(Protocolo protocolo) {
        for (Consumer<Protocolo> ouvinte : ouvintes) {
            ouvinte.accept(protocolo);
        }
    }
}
