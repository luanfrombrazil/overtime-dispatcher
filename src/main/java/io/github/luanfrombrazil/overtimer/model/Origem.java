package io.github.luanfrombrazil.overtimer.model;

/**
 * De onde veio uma Task.
 */
public enum Origem {
    USUARIO,
    PROTOCOLO,
    ORACULO,
    /** Sugerida por um agente pelo servidor MCP da app. */
    AGENTE
}
