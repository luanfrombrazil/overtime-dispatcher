package io.github.luanfrombrazil.overtimer.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * O que o agente pode fazer dentro da pasta de um projeto.
 *
 * @param conectores servidores MCP que o agente pode usar, pelo nome que o CLI dá a eles (ex.: {@code claude_ai_Figma})
 */
public record Permissoes(boolean editarArquivos, boolean rodarTestesEBuild,
                         boolean instalarDependencias, boolean acessarInternet, List<String> conectores) {

    /** Nome de servidor MCP: só letras, números, _ e -, para não virar outra regra de ferramenta. */
    private static final Pattern NOME_DE_CONECTOR = Pattern.compile("[A-Za-z0-9_-]+");

    public Permissoes {
        // Projetos salvos antes dos conectores não têm o campo.
        conectores = conectores == null ? List.of() : List.copyOf(conectores);
        for (String conector : conectores) {
            if (!NOME_DE_CONECTOR.matcher(conector).matches()) {
                throw new IllegalArgumentException("Nome de conector inválido: " + conector);
            }
        }
    }

    public Permissoes(boolean editarArquivos, boolean rodarTestesEBuild,
                      boolean instalarDependencias, boolean acessarInternet) {
        this(editarArquivos, rodarTestesEBuild, instalarDependencias, acessarInternet, List.of());
    }

    public static Permissoes padrao() {
        return new Permissoes(true, true, false, false);
    }

    /**
     * Lê a lista digitada (separada por vírgula, espaço ou quebra de linha), tirando o prefixo {@code mcp__} de
     * quem colou o nome da ferramenta e as repetições.
     */
    public static List<String> conectoresDe(String texto) {
        if (texto == null) {
            return List.of();
        }
        return List.copyOf(Arrays.stream(texto.split("[,\\s]+"))
                .map(String::strip)
                .map(c -> c.startsWith("mcp__") ? c.substring("mcp__".length()) : c)
                .filter(c -> !c.isEmpty())
                .collect(LinkedHashSet<String>::new, LinkedHashSet::add, LinkedHashSet::addAll));
    }
}
