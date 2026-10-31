package io.github.luanfrombrazil.overtimedispatcher.model;

/**
 * O que o agente pode fazer dentro da pasta de um projeto.
 */
public record Permissoes(boolean editarArquivos, boolean rodarTestesEBuild,
                         boolean instalarDependencias, boolean acessarInternet) {

    public static Permissoes padrao() {
        return new Permissoes(true, true, false, false);
    }
}
