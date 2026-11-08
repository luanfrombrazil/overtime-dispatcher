package io.github.luanfrombrazil.overtimer.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Quando um protocolo roda: em horários fixos todos os dias ou a cada intervalo.
 *
 * @param horarios         minutos depois da meia-noite de cada horário fixo, em ordem; só vale no modo
 *                         {@link Modo#HORARIOS}
 * @param intervaloMinutos tempo entre uma execução e a próxima; só vale no modo {@link Modo#INTERVALO}
 */
public record Agenda(Modo modo, List<Integer> horarios, int intervaloMinutos) {

    public enum Modo {
        HORARIOS,
        INTERVALO
    }

    private static final int MINUTOS_NO_DIA = 24 * 60;

    public Agenda {
        modo = modo == null ? Modo.HORARIOS : modo;
        horarios = horarios == null ? List.of() : horarios.stream()
                .filter(m -> m >= 0 && m < MINUTOS_NO_DIA).distinct().sorted().toList();
        if (modo == Modo.INTERVALO && intervaloMinutos <= 0) {
            throw new IllegalArgumentException("Intervalo inválido: " + intervaloMinutos);
        }
    }

    public static Agenda nosHorarios(List<LocalTime> horarios) {
        return new Agenda(Modo.HORARIOS, horarios.stream().map(h -> h.getHour() * 60 + h.getMinute()).toList(), 0);
    }

    public static Agenda aCada(int minutos) {
        return new Agenda(Modo.INTERVALO, List.of(), minutos);
    }

    /** Sem horário nenhum: o protocolo só roda quando pedido. */
    public static Agenda vazia() {
        return new Agenda(Modo.HORARIOS, List.of(), 0);
    }

    public List<LocalTime> horas() {
        return horarios.stream().map(m -> LocalTime.of(m / 60, m % 60)).toList();
    }

    /**
     * Próxima execução estritamente depois de {@code instante} (a última execução ou, se nunca rodou, agora): o
     * primeiro horário fixo seguinte, passando para o dia seguinte se os de hoje já passaram, ou o instante mais o
     * intervalo. Vazio se a agenda não tem horários.
     */
    public Optional<LocalDateTime> proxima(LocalDateTime instante) {
        if (modo == Modo.INTERVALO) {
            return Optional.of(instante.plusMinutes(intervaloMinutos));
        }
        LocalDateTime base = instante.truncatedTo(ChronoUnit.MINUTES);
        for (int dia = 0; dia <= 1; dia++) {
            for (LocalTime hora : horas()) {
                LocalDateTime candidato = base.toLocalDate().plusDays(dia).atTime(hora);
                if (candidato.isAfter(base)) {
                    return Optional.of(candidato);
                }
            }
        }
        return Optional.empty();
    }

    /** Texto curto sob o nome, na tela do protocolo. Ex.: "Todo dia às 08:00 e 18:00", "A cada 2h". */
    public String descricao() {
        if (modo == Modo.INTERVALO) {
            int h = intervaloMinutos / 60;
            int m = intervaloMinutos % 60;
            return "A cada " + (h > 0 ? h + "h" : "") + (m > 0 ? m + "min" : "");
        }
        if (horarios.isEmpty()) {
            return "Sem horário";
        }
        List<String> horas = horas().stream().map(LocalTime::toString).toList();
        String texto = horas.size() == 1 ? horas.getFirst()
                : String.join(", ", horas.subList(0, horas.size() - 1)) + " e " + horas.getLast();
        return "Todo dia às " + texto;
    }
}
