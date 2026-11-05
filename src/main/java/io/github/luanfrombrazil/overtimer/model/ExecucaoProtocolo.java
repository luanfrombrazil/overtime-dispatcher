package io.github.luanfrombrazil.overtimer.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Uma execução de protocolo, para o histórico.
 *
 * @param inicio    quando começou, em ISO ({@code 2026-11-05T08:00:12})
 * @param fim       quando terminou, em ISO
 * @param resultado resposta do agente ou o motivo da falha
 * @param log       o que o agente fez, linha a linha
 */
public record ExecucaoProtocolo(String inicio, String fim, boolean sucesso, String resultado, List<String> log) {

    private static final DateTimeFormatter DIA_E_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public ExecucaoProtocolo {
        log = log == null ? List.of() : List.copyOf(log);
    }

    public static ExecucaoProtocolo de(LocalDateTime inicio, LocalDateTime fim, boolean sucesso, String resultado,
                                       List<String> log) {
        return new ExecucaoProtocolo(inicio.truncatedTo(ChronoUnit.SECONDS).toString(),
                fim.truncatedTo(ChronoUnit.SECONDS).toString(), sucesso, resultado, log);
    }

    public LocalDateTime inicioEm() {
        return LocalDateTime.parse(inicio);
    }

    public LocalDateTime fimEm() {
        return LocalDateTime.parse(fim);
    }

    /** Ex.: "05/11 08:00 → 08:03 · Concluída". */
    public String resumo() {
        return inicioEm().format(DIA_E_HORA) + " → " + fimEm().format(HORA) + " · " + (sucesso ? "Concluída" : "Falhou");
    }

    /** Primeira linha do resultado, ou vazio. */
    public String primeiraLinha() {
        return resultado == null ? "" : resultado.strip().lines().findFirst().orElse("");
    }
}
