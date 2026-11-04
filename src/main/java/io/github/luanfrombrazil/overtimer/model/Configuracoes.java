package io.github.luanfrombrazil.overtimer.model;

import java.time.LocalTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configurações salvas da aplicação: horário de funcionamento, reserva por provedor, execução e notificações.
 */
public record Configuracoes(HorarioFuncionamento horarioFuncionamento, Map<Provedor, Reserva> reservas,
                            Execucao execucao, Notificacoes notificacoes) {

    /** Provedores que têm reserva própria (o automático usa a de quem for escolhido). */
    public static final List<Provedor> PROVEDORES_COM_RESERVA =
            List.of(Provedor.CLAUDE_CODE, Provedor.CODEX, Provedor.GEMINI);

    public Configuracoes {
        horarioFuncionamento = horarioFuncionamento.copia();
        EnumMap<Provedor, Reserva> completas = new EnumMap<>(Provedor.class);
        PROVEDORES_COM_RESERVA.forEach(p -> completas.put(p, reservas.getOrDefault(p, Reserva.padrao())));
        reservas = Map.copyOf(completas);
    }

    /** Só o horário escolhido; o resto com os valores padrão. */
    public Configuracoes(HorarioFuncionamento horarioFuncionamento) {
        this(horarioFuncionamento, Map.of(), Execucao.padrao(), Notificacoes.padrao());
    }

    public static Configuracoes padrao() {
        return new Configuracoes(HorarioFuncionamento.padrao());
    }

    @Override
    public HorarioFuncionamento horarioFuncionamento() {
        return horarioFuncionamento.copia();
    }

    public Reserva reserva(Provedor provedor) {
        return reservas.getOrDefault(provedor, Reserva.padrao());
    }

    /**
     * Divisão da janela de uso de um provedor, em porcentagem: automação, Oráculo e, no que sobra, você.
     */
    public record Reserva(int automacao, int oraculo) {

        public Reserva {
            if (automacao < 0 || oraculo < 0 || automacao + oraculo > 100) {
                throw new IllegalArgumentException("reserva inválida: " + automacao + "% + " + oraculo + "%");
            }
        }

        public static Reserva padrao() {
            return new Reserva(0, 5);
        }

        public int voce() {
            return 100 - automacao - oraculo;
        }
    }

    /**
     * Parâmetros de execução do agendador, em minutos onde for tempo. {@code maximoPorIntervalo} é quantas
     * execuções podem começar em cada intervalo; {@link #SEM_LIMITE} não limita.
     */
    public record Execucao(int intervaloMinutos, int tentativas, int timeoutMinutos, int maximoPorIntervalo) {

        public static final int SEM_LIMITE = 0;

        public Execucao {
            if (intervaloMinutos <= 0 || tentativas <= 0 || timeoutMinutos <= 0 || maximoPorIntervalo < 0) {
                throw new IllegalArgumentException("execução inválida: " + intervaloMinutos + " min, "
                        + tentativas + " tentativas, " + timeoutMinutos + " min, máximo " + maximoPorIntervalo);
            }
        }

        public static Execucao padrao() {
            return new Execucao(5, 3, 30, SEM_LIMITE);
        }
    }

    /** Tipos de evento que podem gerar notificação. */
    public enum Aviso {
        VERIFICACAO("Verificação"),
        DIRECIONAMENTO("Direcionamento"),
        MERGE("Merge"),
        FALHAS("Falhas"),
        LIMITE_DE_USO("Limite de uso"),
        SESSAO_CONCLUIDA("Sessão concluída");

        private final String rotulo;

        Aviso(String rotulo) {
            this.rotulo = rotulo;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    /** Modo silencioso, canais e o que avisar. */
    public record Notificacoes(boolean silenciarAgora, boolean silenciarAutomaticamente, LocalTime silencioInicio,
                               LocalTime silencioFim, boolean falhasFuramSilencio, boolean desktop, boolean whatsapp,
                               boolean som, Set<Aviso> avisos) {

        public Notificacoes {
            avisos = Set.copyOf(avisos);
        }

        public static Notificacoes padrao() {
            return new Notificacoes(false, true, LocalTime.of(23, 0), LocalTime.of(7, 0), true, true, false, false,
                    EnumSet.of(Aviso.VERIFICACAO, Aviso.DIRECIONAMENTO, Aviso.MERGE, Aviso.FALHAS));
        }
    }
}
