package io.github.luanfrombrazil.overtimer.service.rpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.ProjetoService;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Ferramentas que o servidor MCP oferece aos agentes: ler os projetos e as Tasks e sugerir Tasks e subtarefas, que
 * só entram na fila quando você as aceita.
 */
final class FerramentasMcp {

    /** Erro na chamada que o agente pode corrigir: volta para ele como resultado com {@code isError}. */
    static final class ErroDaFerramenta extends RuntimeException {
        ErroDaFerramenta(String mensagem) {
            super(mensagem);
        }
    }

    private static final String PROJETO = """
            {"type": "string", "description": "Id ou nome do projeto. Se omitido, vale o projeto da execução que \
            está chamando (só para agentes rodando pela app)."}""";

    private static final String SUBTAREFA = """
            {
              "type": "object",
              "properties": {
                "titulo": {"type": "string", "description": "Título curto da subtarefa."},
                "instrucao": {"type": "string", "description": "O que o agente que executar a subtarefa deve \
            fazer, completo: ele terá só este texto, o da Task e o projeto."},
                "criterioDeAceite": {"type": "string", "description": "Como saber que a subtarefa está pronta."},
                "requerVerificacao": {"type": "boolean", "description": "Se a pessoa precisa verificar o \
            resultado antes de a subtarefa contar como concluída. Padrão: false."},
                "esforco": %s,
                "dependeDe": {"type": "array", "items": {"type": "integer", "minimum": 0}, "description": \
            "Posições (a partir de 0), nesta mesma lista, das subtarefas que precisam terminar antes desta."}%s
              },
              "required": ["titulo", "instrucao"]
            }""";

    private static final String ESFORCO = """
            {"type": "string", "enum": ["low", "medium", "high", "xhigh", "max"], "description": "Esforço do \
            agente. Padrão: o do projeto, na Task; medium, na subtarefa."}""";

    private static final String DEPENDE_DE_EXISTENTES = """
            ,
                "dependeDeExistentes": {"type": "array", "items": {"type": "string"}, "description": "Ids de \
            subtarefas que já existem na Task e precisam terminar antes desta."}""";

    private final ProjetoService projetos;
    private final ObjectMapper json;
    private final ArrayNode definicoes;

    FerramentasMcp(ProjetoService projetos, ObjectMapper json) {
        this.projetos = projetos;
        this.json = json;
        this.definicoes = json.createArrayNode();
        definir("listar_projetos", "Lista os projetos do Overtimer: id, nome, descrição e pasta.", """
                {"type": "object", "properties": {}}""");
        definir("listar_tasks", "Lista as Tasks de um projeto, com as subtarefas, os estados e o que ainda é "
                + "sugestão. Use antes de sugerir, para não repetir o que já existe e para achar o id de uma Task.",
                """
                        {"type": "object", "properties": {"projeto": %s}}""".formatted(PROJETO));
        definir("sugerir_task", "Sugere uma Task nova no projeto, opcionalmente já quebrada em subtarefas "
                + "executadas em ordem pela mesma sessão do agente. Ela aparece como sugestão e só entra na fila "
                + "quando a pessoa aceitar; a pessoa pode editar antes e aceitar ou recusar cada subtarefa. Escreva "
                + "título, descrição e instruções completos: o agente que executar terá só esse texto e o projeto.",
                """
                        {
                          "type": "object",
                          "properties": {
                            "projeto": %s,
                            "titulo": {"type": "string", "description": "Título curto da Task."},
                            "descricao": {"type": "string", "description": "Objetivo e contexto da Task, \
                        incluindo por que ela é necessária."},
                            "esforco": %s,
                            "subtarefas": {"type": "array", "items": %s}
                          },
                          "required": ["titulo", "descricao"]
                        }""".formatted(PROJETO, ESFORCO, SUBTAREFA.formatted(ESFORCO, "")));
        definir("sugerir_subtarefas", "Sugere subtarefas novas no fim de uma Task que já existe (não "
                + "finalizada). Até a pessoa aceitar todas, nenhuma subtarefa da Task roda.", """
                {
                  "type": "object",
                  "properties": {
                    "projeto": %s,
                    "task": {"type": "string", "description": "Id da Task, como em listar_tasks."},
                    "subtarefas": {"type": "array", "minItems": 1, "items": %s}
                  },
                  "required": ["task", "subtarefas"]
                }""".formatted(PROJETO, SUBTAREFA.formatted(ESFORCO, DEPENDE_DE_EXISTENTES)));
    }

    /** As ferramentas, no formato de {@code tools/list}. */
    ArrayNode definicoes() {
        return definicoes;
    }

    boolean existe(String nome) {
        for (JsonNode definicao : definicoes) {
            if (definicao.path("name").asText().equals(nome)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executa a ferramenta e devolve o texto da resposta ao agente.
     *
     * @param projetoPadrao projeto da execução que chamou, usado quando o agente não diz qual; null para agentes de
     *                      fora da app
     * @throws ErroDaFerramenta se a chamada não pode ser atendida como veio
     */
    String chamar(String nome, JsonNode argumentos, String projetoPadrao) {
        JsonNode args = argumentos == null || argumentos.isNull() ? json.createObjectNode() : argumentos;
        try {
            return switch (nome) {
                case "listar_projetos" -> listarProjetos();
                case "listar_tasks" -> listarTasks(projeto(args, projetoPadrao));
                case "sugerir_task" -> sugerirTask(projeto(args, projetoPadrao), args);
                case "sugerir_subtarefas" -> sugerirSubtarefas(projeto(args, projetoPadrao), args);
                default -> throw new ErroDaFerramenta("Ferramenta desconhecida: " + nome);
            };
        } catch (IllegalArgumentException e) {
            throw new ErroDaFerramenta(e.getMessage());
        }
    }

    private String listarProjetos() {
        ArrayNode lista = json.createArrayNode();
        for (Projeto p : projetos.projetos()) {
            lista.addObject()
                    .put("id", p.id())
                    .put("nome", p.nome())
                    .put("descricao", p.descricao())
                    .put("pasta", p.pasta());
        }
        return escrever(lista);
    }

    private String listarTasks(Projeto projeto) {
        ArrayNode lista = json.createArrayNode();
        for (Task t : projeto.tasks()) {
            ObjectNode task = lista.addObject()
                    .put("id", t.id())
                    .put("titulo", t.titulo())
                    .put("descricao", t.descricao())
                    .put("estado", nomeDoEstado(t.estado().name()))
                    .put("sugerida", t.sugerida())
                    .put("finalizada", t.finalizada());
            ArrayNode subtarefas = task.putArray("subtarefas");
            for (Subtarefa s : t.subtarefas()) {
                subtarefas.addObject()
                        .put("id", s.id())
                        .put("titulo", s.titulo())
                        .put("estado", nomeDoEstado(s.estado().name()));
            }
        }
        ObjectNode resposta = json.createObjectNode().put("projeto", projeto.nome());
        resposta.set("tasks", lista);
        return escrever(resposta);
    }

    private String sugerirTask(Projeto projeto, JsonNode args) {
        String titulo = texto(args, "titulo");
        if (titulo.isBlank()) {
            throw new ErroDaFerramenta("Informe o título da Task.");
        }
        Esforco esforco = esforco(args, projeto.esforco());
        List<Subtarefa> subtarefas = subtarefas(args.path("subtarefas"), List.of());
        Task task = Task.sugerida(titulo.strip(), texto(args, "descricao").strip(), projeto.provedor(), esforco,
                subtarefas);
        projetos.sugerirTask(projeto.id(), task);
        ObjectNode resposta = json.createObjectNode()
                .put("mensagem", "Task sugerida em \"" + projeto.nome() + "\"; espera ser aceita.")
                .put("task", task.id());
        ArrayNode ids = resposta.putArray("subtarefas");
        task.subtarefas().forEach(s -> ids.add(s.id()));
        return escrever(resposta);
    }

    private String sugerirSubtarefas(Projeto projeto, JsonNode args) {
        String taskId = texto(args, "task");
        Task task = projeto.tasks().stream().filter(t -> t.id().equals(taskId)).findFirst()
                .orElseThrow(() -> new ErroDaFerramenta("Task \"" + taskId + "\" não encontrada em \""
                        + projeto.nome() + "\". Use listar_tasks para ver os ids."));
        List<String> existentes = task.subtarefas().stream().map(Subtarefa::id).toList();
        List<Subtarefa> subtarefas = subtarefas(args.path("subtarefas"), existentes);
        if (subtarefas.isEmpty()) {
            throw new ErroDaFerramenta("Sugira ao menos uma subtarefa.");
        }
        projetos.sugerirSubtarefas(projeto.id(), task.id(), subtarefas);
        ObjectNode resposta = json.createObjectNode()
                .put("mensagem", subtarefas.size() + (subtarefas.size() == 1 ? " subtarefa sugerida" :
                        " subtarefas sugeridas") + " em \"" + task.titulo() + "\"; esperam ser aceitas.");
        ArrayNode ids = resposta.putArray("subtarefas");
        subtarefas.forEach(s -> ids.add(s.id()));
        return escrever(resposta);
    }

    /**
     * Lê as subtarefas da chamada, já com ids. As dependências por posição viram ids desta lista; as por id
     * ({@code dependeDeExistentes}) precisam estar em {@code existentes}.
     */
    private List<Subtarefa> subtarefas(JsonNode lista, List<String> existentes) {
        if (lista.isMissingNode() || lista.isNull()) {
            return List.of();
        }
        if (!lista.isArray()) {
            throw new ErroDaFerramenta("\"subtarefas\" precisa ser uma lista.");
        }
        List<Subtarefa> sem = new ArrayList<>();
        for (JsonNode s : lista) {
            String titulo = texto(s, "titulo");
            if (titulo.isBlank()) {
                throw new ErroDaFerramenta("Informe o título de cada subtarefa.");
            }
            sem.add(Subtarefa.nova(titulo.strip(), texto(s, "instrucao").strip(), texto(s, "criterioDeAceite").strip(),
                    s.path("requerVerificacao").asBoolean(false), List.of()));
        }
        List<Subtarefa> com = new ArrayList<>();
        for (int i = 0; i < sem.size(); i++) {
            JsonNode s = lista.get(i);
            List<String> dependencias = new ArrayList<>();
            for (JsonNode posicao : s.path("dependeDe")) {
                int p = posicao.asInt(-1);
                if (!posicao.canConvertToInt() || p < 0 || p >= sem.size() || p == i) {
                    throw new ErroDaFerramenta("\"" + sem.get(i).titulo() + "\": dependeDe tem a posição "
                            + posicao + ", que não é outra subtarefa desta lista.");
                }
                dependencias.add(sem.get(p).id());
            }
            for (JsonNode id : s.path("dependeDeExistentes")) {
                if (!existentes.contains(id.asText())) {
                    throw new ErroDaFerramenta("\"" + sem.get(i).titulo() + "\": a subtarefa \"" + id.asText()
                            + "\" não existe nesta Task.");
                }
                dependencias.add(id.asText());
            }
            Subtarefa subtarefa = sem.get(i).comDependencias(dependencias);
            Esforco esforco = esforco(s, subtarefa.esforco());
            com.add(subtarefa.comModelo(esforco, subtarefa.modelo()));
        }
        return com;
    }

    private Projeto projeto(JsonNode args, String projetoPadrao) {
        String pedido = texto(args, "projeto").strip();
        if (pedido.isEmpty()) {
            if (projetoPadrao == null) {
                throw new ErroDaFerramenta("Informe o projeto (id ou nome). Use listar_projetos para ver os projetos.");
            }
            return projetos.buscar(projetoPadrao)
                    .orElseThrow(() -> new ErroDaFerramenta("O projeto desta execução não existe mais."));
        }
        Optional<Projeto> porId = projetos.buscar(pedido);
        if (porId.isPresent()) {
            return porId.get();
        }
        List<Projeto> porNome = projetos.projetos().stream().filter(p -> p.nome().equalsIgnoreCase(pedido)).toList();
        if (porNome.size() == 1) {
            return porNome.getFirst();
        }
        throw new ErroDaFerramenta(porNome.isEmpty()
                ? "Projeto \"" + pedido + "\" não encontrado. Use listar_projetos para ver os projetos."
                : "Há mais de um projeto chamado \"" + pedido + "\"; informe o id.");
    }

    private static Esforco esforco(JsonNode args, Esforco padrao) {
        String valor = texto(args, "esforco").strip();
        if (valor.isEmpty()) {
            return padrao;
        }
        try {
            return Esforco.valueOf(valor.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ErroDaFerramenta("Esforço \"" + valor + "\" inválido; use low, medium, high, xhigh ou max.");
        }
    }

    private static String texto(JsonNode no, String campo) {
        JsonNode valor = no.path(campo);
        return valor.isValueNode() && !valor.isNull() ? valor.asText() : "";
    }

    private static String nomeDoEstado(String estado) {
        return estado.toLowerCase(Locale.ROOT);
    }

    private void definir(String nome, String descricao, String esquema) {
        try {
            definicoes.addObject()
                    .put("name", nome)
                    .put("description", descricao)
                    .set("inputSchema", json.readTree(esquema));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("esquema inválido em " + nome, e);
        }
    }

    private String escrever(JsonNode no) {
        try {
            return json.writerWithDefaultPrettyPrinter().writeValueAsString(no);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
