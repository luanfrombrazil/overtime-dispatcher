package io.github.luanfrombrazil.overtimer.service;

import io.github.luanfrombrazil.overtimer.model.ExecucaoProtocolo;
import io.github.luanfrombrazil.overtimer.model.ItemFila;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProtocolos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    /** Execuções guardadas no histórico de cada protocolo, pelo id. */
    public static final int EXECUCOES_NO_HISTORICO = 20;

    private final Map<String, List<ExecucaoProtocolo>> historicos = new HashMap<>();

    public ProtocoloService(RepositorioProtocolos repositorio) {
        this.repositorio = repositorio;
        for (Protocolo p : repositorio.carregar()) {
            protocolos.put(p.id(), p);
            historicos.put(p.id(), repositorio.carregarHistorico(p.id()));
        }
    }

    /** Últimas execuções do protocolo, da mais recente para a mais antiga. */
    public synchronized List<ExecucaoProtocolo> historico(String id) {
        return historicos.getOrDefault(id, List.of());
    }

    /** A última execução do protocolo, se ele já rodou. */
    public synchronized Optional<ExecucaoProtocolo> ultimaExecucao(String id) {
        return historico(id).stream().findFirst();
    }

    /**
     * Guarda a execução no começo do histórico, que fica com as {@link #EXECUCOES_NO_HISTORICO} mais recentes. Não
     * faz nada se o protocolo foi excluído durante a execução.
     */
    public void registrarExecucao(String id, ExecucaoProtocolo execucao) {
        Protocolo protocolo;
        synchronized (this) {
            protocolo = protocolos.get(id);
            if (protocolo == null) {
                return;
            }
            List<ExecucaoProtocolo> novo = new ArrayList<>();
            novo.add(execucao);
            novo.addAll(historico(id));
            List<ExecucaoProtocolo> guardado = List.copyOf(novo.subList(0, Math.min(novo.size(),
                    EXECUCOES_NO_HISTORICO)));
            repositorio.salvarHistorico(id, guardado);
            historicos.put(id, guardado);
        }
        avisar(protocolo);
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

    /**
     * Grava o que se edita no pop-up por cima do protocolo salvo, mantendo o estado ligado e as filas atuais: o
     * agendador pode ter consumido a fila ou alguém ter desligado o protocolo com o pop-up aberto.
     */
    public Protocolo editar(Protocolo editado) {
        Protocolo atual = buscar(editado.id())
                .orElseThrow(() -> new IllegalArgumentException("O protocolo não existe mais."));
        return salvar(atual.comDefinicao(editado.nome(), editado.sigla(), editado.passos(), editado.agenda(),
                editado.provedor(), editado.esforco(), editado.acessos(), editado.aceitaDiretoDe()));
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

    /** Tira da fila de entrada os itens já tratados por uma execução; vazio se o protocolo não existe mais. */
    public Optional<Protocolo> tirarDaFila(String id, Set<String> itens) {
        Protocolo alterado;
        synchronized (this) {
            Protocolo atual = protocolos.get(id);
            if (atual == null) {
                return Optional.empty();
            }
            List<ItemFila> restantes = atual.filaDeEntrada().stream().filter(i -> !itens.contains(i.id())).toList();
            if (restantes.size() == atual.filaDeEntrada().size()) {
                return Optional.of(atual);
            }
            alterado = gravar(atual.comFilas(restantes, atual.propostas()));
        }
        avisar(alterado);
        return Optional.of(alterado);
    }

    public void excluir(String id) {
        Protocolo removido;
        synchronized (this) {
            repositorio.excluir(id);
            removido = protocolos.remove(id);
            historicos.remove(id);
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
