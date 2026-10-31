package io.github.luanfrombrazil.overtimedispatcher.service.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.luanfrombrazil.overtimedispatcher.model.Configuracoes;
import io.github.luanfrombrazil.overtimedispatcher.model.HorarioFuncionamento;
import io.github.luanfrombrazil.overtimedispatcher.model.HorarioFuncionamento.Faixa;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Lê e grava as configurações em {@code configuracoes.json}, na pasta central da app.
 *
 * <p>O horário de funcionamento fica como faixas legíveis por dia:
 * <pre>
 * { "horarioFuncionamento": { "seg": ["00:00-07:00", "22:00-24:00"], "sab": ["00:00-24:00"], ... } }
 * </pre>
 */
public class ConfiguracoesRepositorio {

    private static final String ARQUIVO = "configuracoes.json";
    private static final String[] CHAVES_DIAS = {"seg", "ter", "qua", "qui", "sex", "sab", "dom"};

    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path arquivo;

    public ConfiguracoesRepositorio() {
        this(PastaDados.padrao());
    }

    public ConfiguracoesRepositorio(Path pasta) {
        this.arquivo = pasta.resolve(ARQUIVO);
    }

    /** Configurações salvas; se não houver arquivo ou ele estiver inválido, os valores padrão. */
    public Configuracoes carregar() {
        if (!Files.exists(arquivo)) {
            return Configuracoes.padrao();
        }
        try {
            JsonNode raiz = json.readTree(arquivo.toFile());
            JsonNode horario = raiz.path("horarioFuncionamento");
            return new Configuracoes(horario.isObject() ? lerHorario(horario) : HorarioFuncionamento.padrao());
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Configurações inválidas em " + arquivo + "; usando os valores padrão: " + e.getMessage());
            return Configuracoes.padrao();
        }
    }

    public void salvar(Configuracoes configuracoes) {
        ObjectNode raiz = json.createObjectNode();
        raiz.set("horarioFuncionamento", escreverHorario(configuracoes.horarioFuncionamento()));
        try {
            Files.createDirectories(arquivo.getParent());
            Path temporario = arquivo.resolveSibling(ARQUIVO + ".tmp");
            json.writeValue(temporario.toFile(), raiz);
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar as configurações em " + arquivo, e);
        }
    }

    private static HorarioFuncionamento lerHorario(JsonNode no) {
        HorarioFuncionamento horario = HorarioFuncionamento.vazio();
        for (int d = 0; d < CHAVES_DIAS.length; d++) {
            for (JsonNode faixa : no.path(CHAVES_DIAS[d])) {
                horario.definirFaixa(d, lerFaixa(faixa.asText()));
            }
        }
        return horario;
    }

    private ObjectNode escreverHorario(HorarioFuncionamento horario) {
        ObjectNode no = json.createObjectNode();
        for (int d = 0; d < CHAVES_DIAS.length; d++) {
            ArrayNode faixas = no.putArray(CHAVES_DIAS[d]);
            horario.faixas(d).forEach(f -> faixas.add(hora(f.inicio()) + "-" + hora(f.fim())));
        }
        return no;
    }

    /** Converte "HH:MM-HH:MM" (minutos 00 ou 30, fim até 24:00) em segmentos. */
    private static Faixa lerFaixa(String texto) {
        String[] partes = texto.split("-");
        if (partes.length != 2) {
            throw new IllegalArgumentException("faixa inválida: " + texto);
        }
        int inicio = segmento(partes[0].trim());
        int fim = segmento(partes[1].trim());
        if (inicio >= fim) {
            throw new IllegalArgumentException("faixa vazia ou invertida: " + texto);
        }
        return new Faixa(inicio, fim);
    }

    private static int segmento(String hora) {
        String[] partes = hora.split(":");
        try {
            int h = Integer.parseInt(partes[0]);
            int m = partes.length == 2 ? Integer.parseInt(partes[1]) : -1;
            int segmento = h * 2 + m / 30;
            if (partes.length != 2 || (m != 0 && m != 30) || segmento < 0 || segmento > HorarioFuncionamento.SEGMENTOS) {
                throw new IllegalArgumentException("hora inválida: " + hora);
            }
            return segmento;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("hora inválida: " + hora, e);
        }
    }

    private static String hora(int segmento) {
        return String.format("%02d:%02d", segmento / 2, segmento % 2 * 30);
    }
}
