package io.github.luanfrombrazil.overtimer.service.comunicacao;

import io.github.luanfrombrazil.overtimer.model.Configuracoes.Aviso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Notificacoes;
import io.github.luanfrombrazil.overtimer.model.Notificacao;
import io.github.luanfrombrazil.overtimer.service.persistence.NotificacoesRepositorio;

import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Notificações da app: registra os avisos marcados nas configurações, guarda em {@code notificacoes.json} e avisa
 * quem mostra (sino, desktop, som).
 */
public class NotificacaoService {

    /** Avisado na thread de quem registrou ou marcou; quem atualiza a tela deve passar para a thread do Swing. */
    public interface Ouvinte {
        /** Uma notificação nova foi registrada. */
        default void registrada(Notificacao notificacao) {
        }

        /** A lista mudou (nova notificação ou marcada como lida). */
        default void mudou(int naoLidas) {
        }
    }

    private final NotificacoesRepositorio repositorio;
    private final Clock relogio;
    private final List<Notificacao> notificacoes;
    private final List<Ouvinte> ouvintes = new CopyOnWriteArrayList<>();
    private volatile Notificacoes configuracoes;

    public NotificacaoService(NotificacoesRepositorio repositorio, Notificacoes configuracoes) {
        this(repositorio, configuracoes, Clock.systemDefaultZone());
    }

    NotificacaoService(NotificacoesRepositorio repositorio, Notificacoes configuracoes, Clock relogio) {
        this.repositorio = repositorio;
        this.configuracoes = configuracoes;
        this.relogio = relogio;
        this.notificacoes = new ArrayList<>(repositorio.carregar());
    }

    /** Passa a valer o que foi salvo nas configurações. */
    public void aplicar(Notificacoes configuracoes) {
        this.configuracoes = configuracoes;
    }

    public Notificacoes configuracoes() {
        return configuracoes;
    }

    public void aoMudar(Ouvinte ouvinte) {
        ouvintes.add(ouvinte);
    }

    /**
     * Registra o aviso, se o tipo estiver marcado nas configurações; senão, não guarda nada.
     *
     * @param projetoId null se o aviso não é de um projeto
     * @param taskId    null se o aviso não é de uma Task
     * @return a notificação registrada, ou vazio se o tipo está desmarcado
     */
    public Optional<Notificacao> registrar(Aviso tipo, String titulo, String texto, String projetoId, String taskId) {
        if (!configuracoes.avisos().contains(tipo)) {
            return Optional.empty();
        }
        Notificacao nova = new Notificacao(UUID.randomUUID().toString(), tipo, titulo, texto, projetoId, taskId,
                LocalDateTime.now(relogio), false);
        int naoLidas;
        synchronized (this) {
            notificacoes.add(nova);
            gravar();
            naoLidas = naoLidas();
        }
        ouvintes.forEach(o -> o.registrada(nova));
        ouvintes.forEach(o -> o.mudou(naoLidas));
        return Optional.of(nova);
    }

    /** Todas as notificações, da mais nova para a mais antiga. */
    public synchronized List<Notificacao> listar() {
        return notificacoes.stream().sorted(Comparator.comparing(Notificacao::instante).reversed()).toList();
    }

    public synchronized int naoLidas() {
        return (int) notificacoes.stream().filter(n -> !n.lida()).count();
    }

    /** Marca como lidas todas as notificações ainda não lidas. */
    public void marcarComoLidas() {
        marcar(null);
    }

    /** Marca como lida a notificação de id dado, se existir. */
    public void marcarComoLida(String id) {
        marcar(id);
    }

    /** Com {@code id} null, marca todas. */
    private void marcar(String id) {
        int naoLidas;
        synchronized (this) {
            boolean mudou = false;
            for (int i = 0; i < notificacoes.size(); i++) {
                Notificacao n = notificacoes.get(i);
                if (!n.lida() && (id == null || n.id().equals(id))) {
                    notificacoes.set(i, n.comoLida());
                    mudou = true;
                }
            }
            if (!mudou) {
                return;
            }
            gravar();
            naoLidas = naoLidas();
        }
        ouvintes.forEach(o -> o.mudou(naoLidas));
    }

    /** Sem conseguir gravar, a notificação vale para esta sessão; não derruba quem registrou. */
    private void gravar() {
        try {
            repositorio.salvar(notificacoes);
        } catch (UncheckedIOException e) {
            System.err.println(e.getMessage() + ": " + e.getCause());
        }
    }
}
