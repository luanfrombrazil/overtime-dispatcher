package io.github.luanfrombrazil.overtimer.model;

/**
 * Configurações salvas da aplicação. Por enquanto, só o horário de funcionamento.
 */
public record Configuracoes(HorarioFuncionamento horarioFuncionamento) {

    public static Configuracoes padrao() {
        return new Configuracoes(HorarioFuncionamento.padrao());
    }
}
