package io.github.luanfrombrazil.overtimer.service.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.luanfrombrazil.overtimer.model.Configuracoes;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Aviso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Execucao;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.LimiteDeUso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Notificacoes;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Prompts;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Prompts.ModeloDePrompt;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Prompts.Uso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Prompts.VariavelEditavel;
import io.github.luanfrombrazil.overtimer.model.HorarioFuncionamento;
import io.github.luanfrombrazil.overtimer.model.HorarioFuncionamento.Faixa;
import io.github.luanfrombrazil.overtimer.model.Provedor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lê e grava as configurações em {@code configuracoes.json}, na pasta central da app.
 *
 * <p>O horário de funcionamento fica como faixas legíveis por dia:
 * <pre>
 * { "horarioFuncionamento": { "seg": ["00:00-07:00", "22:00-24:00"], "sab": ["00:00-24:00"], ... } }
 * </pre>
 * As demais seções ({@code limitesDeUso}, {@code execucao}, {@code notificacoes}, {@code prompts}) estão em
 * docs/modulos/persistencia.md.
 */
public class ConfiguracoesRepositorio {

    private static final String ARQUIVO = "configuracoes.json";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
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
            if (raiz == null || !raiz.isObject()) {
                throw new IllegalArgumentException("o arquivo não contém um objeto JSON");
            }
            // Cada seção ausente (ex.: arquivo de uma versão anterior) fica com o padrão dela.
            JsonNode horario = raiz.path("horarioFuncionamento");
            return new Configuracoes(
                    horario.isObject() ? lerHorario(horario) : HorarioFuncionamento.padrao(),
                    // A antiga "reservas" (protótipo, com automação em 0%) é ignorada de propósito.
                    lerLimitesDeUso(raiz.path("limitesDeUso")),
                    lerExecucao(raiz.path("execucao")),
                    lerNotificacoes(raiz.path("notificacoes")),
                    lerPrompts(raiz.path("prompts")));
        } catch (IOException | IllegalArgumentException | DateTimeParseException e) {
            System.err.println("Configurações inválidas em " + arquivo + "; usando os valores padrão: " + e.getMessage());
            return Configuracoes.padrao();
        }
    }

    public void salvar(Configuracoes configuracoes) {
        ObjectNode raiz = json.createObjectNode();
        raiz.set("horarioFuncionamento", escreverHorario(configuracoes.horarioFuncionamento()));
        raiz.set("limitesDeUso", escreverLimitesDeUso(configuracoes));
        raiz.set("execucao", escreverExecucao(configuracoes.execucao()));
        raiz.set("notificacoes", escreverNotificacoes(configuracoes.notificacoes()));
        raiz.set("prompts", escreverPrompts(configuracoes.prompts()));
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

    private static Map<Provedor, LimiteDeUso> lerLimitesDeUso(JsonNode no) {
        Map<Provedor, LimiteDeUso> limites = new EnumMap<>(Provedor.class);
        for (Provedor provedor : Configuracoes.PROVEDORES_COM_LIMITE) {
            JsonNode limite = no.path(provedor.name());
            if (limite.isObject()) {
                LimiteDeUso padrao = LimiteDeUso.padrao();
                limites.put(provedor, new LimiteDeUso(limite.path("automacao").asInt(padrao.automacao()),
                        limite.path("oraculo").asInt(padrao.oraculo())));
            }
        }
        return limites;
    }

    private ObjectNode escreverLimitesDeUso(Configuracoes configuracoes) {
        ObjectNode no = json.createObjectNode();
        for (Provedor provedor : Configuracoes.PROVEDORES_COM_LIMITE) {
            LimiteDeUso limite = configuracoes.limiteDeUso(provedor);
            no.putObject(provedor.name())
                    .put("automacao", limite.automacao())
                    .put("oraculo", limite.oraculo());
        }
        return no;
    }

    private static Execucao lerExecucao(JsonNode no) {
        Execucao padrao = Execucao.padrao();
        return new Execucao(no.path("intervaloMinutos").asInt(padrao.intervaloMinutos()),
                no.path("tentativas").asInt(padrao.tentativas()),
                no.path("timeoutMinutos").asInt(padrao.timeoutMinutos()),
                no.path("maximoPorIntervalo").asInt(padrao.maximoPorIntervalo()));
    }

    private ObjectNode escreverExecucao(Execucao execucao) {
        return json.createObjectNode()
                .put("intervaloMinutos", execucao.intervaloMinutos())
                .put("tentativas", execucao.tentativas())
                .put("timeoutMinutos", execucao.timeoutMinutos())
                .put("maximoPorIntervalo", execucao.maximoPorIntervalo());
    }

    private static Notificacoes lerNotificacoes(JsonNode no) {
        Notificacoes padrao = Notificacoes.padrao();
        JsonNode canais = no.path("canais");
        JsonNode avisos = no.path("avisos");
        Set<Aviso> lidos = EnumSet.noneOf(Aviso.class);
        if (avisos.isArray()) {
            // Um aviso que não existe mais é ignorado, sem invalidar o resto.
            for (JsonNode aviso : avisos) {
                for (Aviso conhecido : Aviso.values()) {
                    if (conhecido.name().equals(aviso.asText())) {
                        lidos.add(conhecido);
                    }
                }
            }
        } else {
            lidos.addAll(padrao.avisos());
        }
        return new Notificacoes(
                no.path("silenciarAgora").asBoolean(padrao.silenciarAgora()),
                no.path("silenciarAutomaticamente").asBoolean(padrao.silenciarAutomaticamente()),
                no.has("silencioInicio") ? LocalTime.parse(no.path("silencioInicio").asText()) : padrao.silencioInicio(),
                no.has("silencioFim") ? LocalTime.parse(no.path("silencioFim").asText()) : padrao.silencioFim(),
                no.path("falhasFuramSilencio").asBoolean(padrao.falhasFuramSilencio()),
                canais.path("desktop").asBoolean(padrao.desktop()),
                canais.path("whatsapp").asBoolean(padrao.whatsapp()),
                canais.path("som").asBoolean(padrao.som()),
                lidos);
    }

    private ObjectNode escreverNotificacoes(Notificacoes notificacoes) {
        ObjectNode no = json.createObjectNode()
                .put("silenciarAgora", notificacoes.silenciarAgora())
                .put("silenciarAutomaticamente", notificacoes.silenciarAutomaticamente())
                .put("silencioInicio", notificacoes.silencioInicio().format(HORA))
                .put("silencioFim", notificacoes.silencioFim().format(HORA))
                .put("falhasFuramSilencio", notificacoes.falhasFuramSilencio());
        no.putObject("canais")
                .put("desktop", notificacoes.desktop())
                .put("whatsapp", notificacoes.whatsapp())
                .put("som", notificacoes.som());
        ArrayNode avisos = no.putArray("avisos");
        // Na ordem do enum, para o arquivo não mudar à toa entre um salvamento e outro.
        for (Aviso aviso : Aviso.values()) {
            if (notificacoes.avisos().contains(aviso)) {
                avisos.add(aviso.name());
            }
        }
        return no;
    }

    private static Prompts lerPrompts(JsonNode no) {
        if (!no.isObject()) {
            return Prompts.padrao();
        }
        List<ModeloDePrompt> modelos = new ArrayList<>();
        JsonNode lidos = no.path("modelos");
        if (lidos.isArray()) {
            for (JsonNode modelo : lidos) {
                modelos.add(new ModeloDePrompt(modelo.path("nome").asText(), modelo.path("texto").asText(),
                        Uso.valueOf(modelo.path("usadoEm").asText())));
            }
        } else {
            modelos.addAll(Prompts.padrao().modelos());
        }
        List<VariavelEditavel> variaveis = new ArrayList<>();
        for (JsonNode variavel : no.path("variaveis")) {
            variaveis.add(new VariavelEditavel(variavel.path("nome").asText(), variavel.path("descricao").asText(),
                    variavel.path("regra").asText()));
        }
        return new Prompts(modelos, variaveis);
    }

    private ObjectNode escreverPrompts(Prompts prompts) {
        ObjectNode no = json.createObjectNode();
        ArrayNode modelos = no.putArray("modelos");
        prompts.modelos().forEach(m -> modelos.addObject()
                .put("nome", m.nome())
                .put("texto", m.texto())
                .put("usadoEm", m.usadoEm().name()));
        ArrayNode variaveis = no.putArray("variaveis");
        prompts.variaveis().forEach(v -> variaveis.addObject()
                .put("nome", v.nome())
                .put("descricao", v.descricao())
                .put("regra", v.regra()));
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
