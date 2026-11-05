package io.github.luanfrombrazil.overtimer.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Horário em que a automação pode rodar: para cada dia da semana (segunda a domingo),
 * 48 segmentos de 30 minutos.
 */
public final class HorarioFuncionamento {

    public static final int DIAS = 7;
    public static final int SEGMENTOS = 48;

    /** Faixa contínua de segmentos de um dia, no intervalo [inicio, fim). */
    public record Faixa(int inicio, int fim) {
    }

    private final boolean[][] segmentos = new boolean[DIAS][SEGMENTOS];

    /** Nenhum horário liberado. */
    public static HorarioFuncionamento vazio() {
        return new HorarioFuncionamento();
    }

    /** Todos os dias, o dia inteiro. */
    public static HorarioFuncionamento sempre() {
        HorarioFuncionamento horario = new HorarioFuncionamento();
        for (int d = 0; d < DIAS; d++) {
            horario.definirHoras(d, 0, 24);
        }
        return horario;
    }

    /** Dias úteis das 22h às 7h e fins de semana inteiros. */
    public static HorarioFuncionamento noitesEFinsDeSemana() {
        HorarioFuncionamento horario = new HorarioFuncionamento();
        for (int d = 0; d < 5; d++) {
            horario.definirHoras(d, 0, 7);
            horario.definirHoras(d, 22, 24);
        }
        horario.definirHoras(5, 0, 24);
        horario.definirHoras(6, 0, 24);
        return horario;
    }

    /** Horário usado quando ainda não há nada salvo: o tempo todo. */
    public static HorarioFuncionamento padrao() {
        return sempre();
    }

    public boolean liberado(int dia, int segmento) {
        return segmentos[dia][segmento];
    }

    public void definir(int dia, int segmento, boolean liberado) {
        segmentos[dia][segmento] = liberado;
    }

    /** Libera as horas [horaInicio, horaFim) do dia. */
    public void definirHoras(int dia, int horaInicio, int horaFim) {
        definirFaixa(dia, new Faixa(horaInicio * 2, horaFim * 2));
    }

    public void definirFaixa(int dia, Faixa faixa) {
        for (int s = faixa.inicio(); s < faixa.fim(); s++) {
            segmentos[dia][s] = true;
        }
    }

    /** Faixas liberadas do dia, em ordem. */
    public List<Faixa> faixas(int dia) {
        List<Faixa> faixas = new ArrayList<>();
        int inicio = -1;
        for (int s = 0; s <= SEGMENTOS; s++) {
            boolean livre = s < SEGMENTOS && segmentos[dia][s];
            if (livre && inicio < 0) {
                inicio = s;
            } else if (!livre && inicio >= 0) {
                faixas.add(new Faixa(inicio, s));
                inicio = -1;
            }
        }
        return faixas;
    }

    /** Se a automação pode rodar no instante indicado. */
    public boolean permite(LocalDateTime instante) {
        int dia = instante.getDayOfWeek().getValue() - 1;
        int segmento = instante.getHour() * 2 + instante.getMinute() / 30;
        return segmentos[dia][segmento];
    }

    /**
     * Início do primeiro segmento liberado depois de {@code instante} (o segmento em que ele cai não conta);
     * vazio se nenhum horário está liberado.
     */
    public Optional<LocalDateTime> proximaLiberacao(LocalDateTime instante) {
        LocalDateTime segmento = instante.truncatedTo(ChronoUnit.HOURS)
                .plusMinutes(instante.getMinute() / 30 * 30L);
        for (int i = 1; i <= DIAS * SEGMENTOS; i++) {
            LocalDateTime candidato = segmento.plusMinutes(30L * i);
            if (permite(candidato)) {
                return Optional.of(candidato);
            }
        }
        return Optional.empty();
    }

    public HorarioFuncionamento copia() {
        HorarioFuncionamento copia = new HorarioFuncionamento();
        for (int d = 0; d < DIAS; d++) {
            System.arraycopy(segmentos[d], 0, copia.segmentos[d], 0, SEGMENTOS);
        }
        return copia;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof HorarioFuncionamento h && Arrays.deepEquals(segmentos, h.segmentos);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(segmentos);
    }
}
