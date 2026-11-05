package io.github.luanfrombrazil.overtimer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * O que o agente de um protocolo pode acessar (D-14): sites por crawling, só leitura; e-mail por conector, lendo e
 * criando rascunhos; calendário por conector, só na agenda própria do agente.
 *
 * @param sites              endereços que o agente pode ler; vazia quando o protocolo não acessa sites
 * @param conectorEmail      servidor MCP do e-mail, pelo nome que o CLI dá a ele (ex.: {@code claude_ai_Gmail}); só
 *                           vale com {@code email} marcado
 * @param conectorCalendario servidor MCP do calendário; só vale com {@code calendario} marcado
 */
public record Acessos(List<String> sites, boolean email, boolean calendario,
                      String conectorEmail, String conectorCalendario) {

    public Acessos {
        sites = sites == null ? List.of() : sites.stream().map(String::strip).filter(s -> !s.isEmpty())
                .distinct().toList();
        conectorEmail = conector(conectorEmail);
        conectorCalendario = conector(conectorCalendario);
    }

    /** Sem conectores escolhidos. */
    public Acessos(List<String> sites, boolean email, boolean calendario) {
        this(sites, email, calendario, null, null);
    }

    public static Acessos nenhum() {
        return new Acessos(List.of(), false, false);
    }

    /** Nome digitado ou colado ({@code mcp__} na frente) do servidor; null se vazio. */
    private static String conector(String nome) {
        List<String> lidos = nome == null ? List.of() : Permissoes.conectoresDe(nome);
        if (lidos.size() > 1) {
            throw new IllegalArgumentException("Informe um só conector: " + nome);
        }
        // Valida o nome como os conectores dos projetos.
        return lidos.isEmpty() ? null : new Permissoes(false, false, false, false, lidos).conectores().getFirst();
    }

    /** Servidores MCP liberados: os dos acessos marcados que têm conector escolhido. */
    public List<String> conectores() {
        List<String> conectores = new ArrayList<>();
        if (email && conectorEmail != null) {
            conectores.add(conectorEmail);
        }
        if (calendario && conectorCalendario != null && !conectores.contains(conectorCalendario)) {
            conectores.add(conectorCalendario);
        }
        return conectores;
    }

    /** Rótulos dos acessos liberados, na ordem da tela. */
    public List<String> rotulos() {
        List<String> rotulos = new ArrayList<>(sites);
        if (email) {
            rotulos.add("E-mail");
        }
        if (calendario) {
            rotulos.add("Calendário");
        }
        return rotulos;
    }
}
