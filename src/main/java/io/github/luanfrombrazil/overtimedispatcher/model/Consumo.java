package io.github.luanfrombrazil.overtimedispatcher.model;

/**
 * Uso de um provedor, em porcentagem.
 *
 * @param sessao  uso da janela da sessão atual
 * @param semanal uso do limite semanal
 */
public record Consumo(String provedor, int sessao, int semanal) {
}
