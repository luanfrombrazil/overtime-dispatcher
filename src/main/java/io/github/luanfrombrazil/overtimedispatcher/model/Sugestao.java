package io.github.luanfrombrazil.overtimedispatcher.model;

/**
 * Item do feed do Oráculo.
 *
 * @param etapa  etapa atual de um roadmap (0 quando não é roadmap)
 * @param etapas total de etapas do roadmap
 */
public record Sugestao(String tipo, String titulo, String porque,
                       String acaoSecundaria, String acaoPrincipal, int etapa, int etapas) {
}
