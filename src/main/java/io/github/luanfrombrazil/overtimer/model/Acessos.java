package io.github.luanfrombrazil.overtimer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * O que o agente de um protocolo pode acessar (D-14): sites por crawling, só leitura; e-mail por conector, lendo e
 * criando rascunhos; calendário por conector, só na agenda própria do agente.
 *
 * @param sites endereços que o agente pode ler; vazia quando o protocolo não acessa sites
 */
public record Acessos(List<String> sites, boolean email, boolean calendario) {

    public Acessos {
        sites = sites == null ? List.of() : sites.stream().map(String::strip).filter(s -> !s.isEmpty())
                .distinct().toList();
    }

    public static Acessos nenhum() {
        return new Acessos(List.of(), false, false);
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
