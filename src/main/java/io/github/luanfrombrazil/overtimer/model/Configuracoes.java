package io.github.luanfrombrazil.overtimer.model;

import java.time.LocalTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configurações salvas da aplicação: horário de funcionamento, limite de uso por provedor, execução e notificações.
 */
public record Configuracoes(HorarioFuncionamento horarioFuncionamento, Map<Provedor, LimiteDeUso> limitesDeUso,
                            Execucao execucao, Notificacoes notificacoes) {

    /** Provedores que têm limite de uso próprio (o automático usa o de quem for escolhido). */
    public static final List<Provedor> PROVEDORES_COM_LIMITE =
            List.of(Provedor.CLAUDE_CODE, Provedor.CODEX, Provedor.GEMINI);

    public Configuracoes {
        horarioFuncionamento = horarioFuncionamento.copia();
        EnumMap<Provedor, LimiteDeUso> completos = new EnumMap<>(Provedor.class);
        Map<Provedor, LimiteDeUso> salvos = limitesDeUso == null ? Map.of() : limitesDeUso;
        PROVEDORES_COM_LIMITE.forEach(p -> completos.put(p, salvos.getOrDefault(p, LimiteDeUso.padrao())));
        limitesDeUso = Map.copyOf(completos);
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

    public LimiteDeUso limiteDeUso(Provedor provedor) {
        return limitesDeUso.getOrDefault(provedor, LimiteDeUso.padrao());
    }

    /**
     * Divisão da janela de uso de um provedor, em porcentagem: até {@code automacao} o agendador pode começar
     * Tasks; a faixa seguinte é do Oráculo e o que sobra fica para você.
     */
    public record LimiteDeUso(int automacao, int oraculo) {

        public LimiteDeUso {
            if (automacao < 0 || oraculo < 0 || automacao + oraculo > 100) {
                throw new IllegalArgumentException("limite de uso inválido: " + automacao + "% + " + oraculo + "%");
            }
        }

        /** Automação até 90% da janela, 5% para o Oráculo e 5% para você. */
        public static LimiteDeUso padrao() {
            return new LimiteDeUso(90, 5);
        }

        /**
         * Se a automação pode começar algo com o uso medido (0 a 100); uso desconhecido (negativo) não impede.
         */
        public boolean permite(int uso) {
            return uso < 0 || uso < automacao;
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
