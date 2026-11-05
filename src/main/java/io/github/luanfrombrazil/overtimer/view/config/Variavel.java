package io.github.luanfrombrazil.overtimer.view.config;

/**
 * Variável que pode entrar no texto de um comando ou prompt, escrita como {@code {nome}}.
 *
 * @param regra como a variável vira texto, para as modificáveis; null nas fixas
 */
public record Variavel(String nome, String descricao, String regra) {

    public boolean modificavel() {
        return regra != null;
    }

    public Variavel comRegra(String novaRegra) {
        return new Variavel(nome, descricao, novaRegra);
    }

    /** Como a variável aparece no texto. */
    public String marcador() {
        return "{" + nome + "}";
    }
}
