package io.github.luanfrombrazil.overtimer.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configurações salvas da aplicação: horário de funcionamento, limite de uso por provedor, execução, notificações e
 * prompts dos agentes.
 */
public record Configuracoes(HorarioFuncionamento horarioFuncionamento, Map<Provedor, LimiteDeUso> limitesDeUso,
                            Execucao execucao, Notificacoes notificacoes, Prompts prompts) {

    /** Provedores que têm limite de uso próprio (o automático usa o de quem for escolhido). */
    public static final List<Provedor> PROVEDORES_COM_LIMITE =
            List.of(Provedor.CLAUDE_CODE, Provedor.CODEX, Provedor.GEMINI);

    public Configuracoes {
        horarioFuncionamento = horarioFuncionamento.copia();
        EnumMap<Provedor, LimiteDeUso> completos = new EnumMap<>(Provedor.class);
        Map<Provedor, LimiteDeUso> salvos = limitesDeUso == null ? Map.of() : limitesDeUso;
        PROVEDORES_COM_LIMITE.forEach(p -> completos.put(p, salvos.getOrDefault(p, LimiteDeUso.padrao())));
        limitesDeUso = Map.copyOf(completos);
        prompts = prompts == null ? Prompts.padrao() : prompts;
    }

    /** Só o horário escolhido; o resto com os valores padrão. */
    public Configuracoes(HorarioFuncionamento horarioFuncionamento) {
        this(horarioFuncionamento, Map.of(), Execucao.padrao(), Notificacoes.padrao(), Prompts.padrao());
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

        /**
         * Se o aviso fica em silêncio nesse instante: com "silenciar agora" ou dentro da faixa automática (início
         * incluído, fim não; a faixa pode passar da meia-noite). Falhas furam os dois quando
         * {@code falhasFuramSilencio} está ligado. Silenciado não é perdido: só não alerta.
         */
        public boolean silenciado(LocalDateTime instante, Aviso aviso) {
            if (aviso == Aviso.FALHAS && falhasFuramSilencio) {
                return false;
            }
            return silenciarAgora || silenciarAutomaticamente && naFaixaDeSilencio(instante.toLocalTime());
        }

        private boolean naFaixaDeSilencio(LocalTime hora) {
            if (silencioInicio.equals(silencioFim)) {
                return false;
            }
            if (silencioInicio.isBefore(silencioFim)) {
                return !hora.isBefore(silencioInicio) && hora.isBefore(silencioFim);
            }
            // Vira o dia: ex.: 23:00-07:00 é das 23:00 até a meia-noite e da meia-noite até as 07:00.
            return !hora.isBefore(silencioInicio) || hora.isBefore(silencioFim);
        }
    }

    /**
     * Textos que o agente recebe. Cada modelo de prompt é usado por padrão em subtarefas ou em Tasks inteiras; as
     * variáveis, escritas como {@code {nome}}, são trocadas pelo texto delas ao montar o pedido.
     *
     * @param variaveis regras das variáveis editáveis: as que mudam uma das {@link #REGRAS} e as criadas pela pessoa.
     *                  Uma regra embutida sem entrada aqui é gerada a partir do projeto e da subtarefa.
     */
    public record Prompts(List<ModeloDePrompt> modelos, List<VariavelEditavel> variaveis) {

        /** Variáveis editáveis que já vêm com a app. */
        public static final List<String> REGRAS = List.of("regras.permissoes", "regras.git", "regras.resposta");

        public static final String PADRAO_SUBTAREFAS = """
                Você é um agente executando trabalho automatizado no projeto "{projeto.nome}".
                {projeto.descricao}

                ## Task: {task.titulo}
                {task.descricao}

                Já concluído nesta task:
                {concluidas}

                ## Subtarefa atual: {subtarefa.titulo}

                Instrução:
                {subtarefa.instrucao}

                Critério de aceite:
                {subtarefa.criterio}

                {direcionamento}

                ## Regras
                - Faça só esta subtarefa; as próximas chegarão em mensagens seguintes desta mesma conversa.
                {regras.permissoes}
                {regras.git}
                {regras.resposta}""";

        public static final String PADRAO_TASK_INTEIRA = """
                Você é um agente executando trabalho automatizado no projeto "{projeto.nome}".
                {projeto.descricao}

                ## Task: {task.titulo}
                {task.descricao}

                {direcionamento}

                ## Regras
                - Faça a task inteira: o título e a descrição acima são a instrução.
                {regras.permissoes}
                {regras.git}
                {regras.resposta}""";

        public Prompts {
            modelos = List.copyOf(modelos);
            variaveis = List.copyOf(variaveis);
        }

        public static Prompts padrao() {
            return new Prompts(List.of(
                    new ModeloDePrompt("Padrão · subtarefas", PADRAO_SUBTAREFAS, Uso.SUBTAREFAS),
                    new ModeloDePrompt("Padrão · task inteira", PADRAO_TASK_INTEIRA, Uso.TASK_INTEIRA)), List.of());
        }

        /** O primeiro modelo usado em {@code uso}; sem nenhum, o padrão da app. */
        public ModeloDePrompt modelo(Uso uso) {
            return modelos.stream().filter(m -> m.usadoEm() == uso).findFirst()
                    .orElseGet(() -> padrao().modelo(uso));
        }

        /** Onde um modelo de prompt é usado por padrão. */
        public enum Uso {
            SUBTAREFAS("Subtarefas"),
            TASK_INTEIRA("Tasks inteiras");

            private final String rotulo;

            Uso(String rotulo) {
                this.rotulo = rotulo;
            }

            @Override
            public String toString() {
                return rotulo;
            }
        }

        public record ModeloDePrompt(String nome, String texto, Uso usadoEm) {

            public ModeloDePrompt {
                if (nome == null || nome.isBlank() || texto == null || usadoEm == null) {
                    throw new IllegalArgumentException("modelo de prompt inválido: " + nome);
                }
            }
        }

        /** @param regra texto da variável, que pode usar as outras variáveis do prompt */
        public record VariavelEditavel(String nome, String descricao, String regra) {

            public VariavelEditavel {
                if (nome == null || nome.isBlank() || regra == null) {
                    throw new IllegalArgumentException("variável inválida: " + nome);
                }
                descricao = descricao == null ? "" : descricao;
            }
        }
    }
}
