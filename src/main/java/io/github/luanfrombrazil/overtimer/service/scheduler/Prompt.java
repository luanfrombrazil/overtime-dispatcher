package io.github.luanfrombrazil.overtimer.service.scheduler;

import io.github.luanfrombrazil.overtimer.model.Configuracoes.Prompts;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Prompts.Uso;
import io.github.luanfrombrazil.overtimer.model.Direcionamento;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Subtarefa;
import io.github.luanfrombrazil.overtimer.model.Task;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Monta o pedido de uma subtarefa, ou da Task inteira, para o agente a partir dos prompts das configurações e
 * interpreta a resposta.
 */
final class Prompt {

    /** O agente começa a resposta com isto quando não consegue concluir o trabalho. */
    static final String MARCA_DE_FALHA = "FALHOU:";

    /** Linha que abre, no fim da resposta, o bloco de perguntas do agente que precisa de direcionamento. */
    static final String MARCA_DE_DIRECIONAMENTO = "DIRECIONAMENTO:";

    private static final Pattern PERGUNTA = Pattern.compile("(?i)^(?:\\d+[.)]\\s*)?pergunta(?:\\s*\\d+)?\\s*:\\s*(.+)$");
    private static final Pattern OPCAO = Pattern.compile("^(?:[-*•]|\\d+[.)]|[a-zA-Z][.)])\\s+(.+)$");

    private Prompt() {
    }

    /** Variável no texto de um prompt: {@code {nome}}. */
    private static final Pattern VARIAVEL = Pattern.compile("\\{([\\w.]+)}");

    /**
     * Preenche o modelo de prompt das configurações usado em subtarefas ou em Tasks inteiras.
     *
     * @param subtarefa subtarefa a executar, ou null para executar a Task inteira (a descrição dela é a instrução)
     * @param branch    branch do worktree, usada só quando o projeto usa git
     */
    static String montar(Projeto projeto, Task task, Subtarefa subtarefa, String branch, Prompts prompts) {
        Uso uso = subtarefa == null ? Uso.TASK_INTEIRA : Uso.SUBTAREFAS;
        return preencher(prompts.modelo(uso).texto(), valores(projeto, task, subtarefa, branch, prompts));
    }

    /**
     * Mensagem que retoma a sessão em que o agente pediu direcionamento: as suas respostas e o pedido para
     * continuar. As regras do projeto já estão na conversa; só as de como terminar são repetidas.
     */
    static String continuar(Projeto projeto, Task task, Subtarefa subtarefa, Direcionamento respondido, String branch,
                            Prompts prompts) {
        StringBuilder p = new StringBuilder("Resposta ao seu pedido de direcionamento:\n\n");
        respostas(p, respondido);
        p.append("\nContinue ").append(subtarefa == null
                ? "a task \"" + task.titulo() + "\""
                : "a subtarefa \"" + subtarefa.titulo() + "\"").append(" de onde parou, com as mesmas regras.\n");
        p.append(valores(projeto, task, subtarefa, branch, prompts).get("regras.resposta")).append('\n');
        return p.toString();
    }

    /**
     * Texto de cada variável do prompt. As regras embutidas sem versão nas configurações são geradas do projeto e
     * da subtarefa; as editadas e as criadas pela pessoa são preenchidas com as demais variáveis.
     */
    private static Map<String, String> valores(Projeto projeto, Task task, Subtarefa subtarefa, String branch,
                                               Prompts prompts) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("projeto.nome", texto(projeto.nome()));
        v.put("projeto.descricao", texto(projeto.descricao()));
        v.put("branch", projeto.usaGit() ? texto(branch) : "");
        v.put("task.titulo", texto(task.titulo()));
        v.put("task.descricao", texto(task.descricao()));
        v.put("subtarefa.titulo", subtarefa == null ? "" : texto(subtarefa.titulo()));
        v.put("subtarefa.instrucao", subtarefa == null ? "" : texto(subtarefa.instrucao()));
        v.put("subtarefa.criterio", subtarefa == null ? "" : texto(subtarefa.criterioDeAceite()));
        v.put("concluidas", subtarefa == null ? "" : concluidas(task, subtarefa));
        Direcionamento dado = subtarefa == null ? task.direcionamento() : subtarefa.direcionamento();
        // A sessão em que o agente perguntou se perdeu: as respostas vão junto com o pedido completo.
        v.put("direcionamento", dado != null && dado.respondido() ? direcionamento(dado) : "");
        Set<String> fixas = Set.copyOf(v.keySet());

        Map<String, String> editadas = new LinkedHashMap<>();
        prompts.variaveis().stream()
                .filter(e -> !fixas.contains(e.nome()))
                .forEach(e -> editadas.putIfAbsent(e.nome(), e.regra()));
        Map<String, String> base = Map.copyOf(v);
        v.put("regras.permissoes", regra(editadas.get("regras.permissoes"), base,
                () -> permissoes(projeto.permissoes())));
        // Sem git não há worktree nem commit: a regra fica de fora mesmo editada.
        v.put("regras.git", !projeto.usaGit() ? ""
                : regra(editadas.get("regras.git"), base, () -> git(subtarefa, branch)));
        v.put("regras.resposta", regra(editadas.get("regras.resposta"), base,
                () -> fim(subtarefa != null && temTexto(subtarefa.criterioDeAceite()))));
        Map<String, String> comRegras = Map.copyOf(v);
        editadas.forEach((nome, regra) -> {
            if (!Prompts.REGRAS.contains(nome)) {
                v.put(nome, preencher(regra, comRegras).strip());
            }
        });
        return v;
    }

    private static String regra(String editada, Map<String, String> valores, Supplier<String> padrao) {
        return editada == null ? padrao.get() : preencher(editada, valores).strip();
    }

    /**
     * Troca as variáveis conhecidas pelo texto delas; {@code {...}} que não é variável fica como está. Some a linha
     * que só tinha variáveis vazias e o parágrafo cujas variáveis estão todas vazias (ex.: "Instrução:" sem
     * instrução).
     */
    static String preencher(String modelo, Map<String, String> valores) {
        StringBuilder saida = new StringBuilder();
        for (String paragrafo : modelo.replace("\r\n", "\n").strip().split("\n[ \t]*\n")) {
            List<String> linhas = new ArrayList<>();
            boolean temVariavel = false;
            boolean algumaPreenchida = false;
            for (String linha : paragrafo.lines().toList()) {
                Matcher m = VARIAVEL.matcher(linha);
                StringBuilder nova = new StringBuilder();
                boolean daLinha = false;
                boolean preenchidaNaLinha = false;
                while (m.find()) {
                    String valor = valores.get(m.group(1));
                    if (valor == null) {
                        m.appendReplacement(nova, Matcher.quoteReplacement(m.group()));
                        continue;
                    }
                    daLinha = true;
                    preenchidaNaLinha |= !valor.isEmpty();
                    m.appendReplacement(nova, Matcher.quoteReplacement(valor));
                }
                m.appendTail(nova);
                temVariavel |= daLinha;
                algumaPreenchida |= preenchidaNaLinha;
                if (!(daLinha && !preenchidaNaLinha && nova.toString().isBlank())) {
                    linhas.add(nova.toString());
                }
            }
            if (linhas.isEmpty() || (temVariavel && !algumaPreenchida)) {
                continue;
            }
            if (!saida.isEmpty()) {
                saida.append("\n\n");
            }
            saida.append(String.join("\n", linhas));
        }
        return saida.append('\n').toString();
    }

    private static String concluidas(Task task, Subtarefa subtarefa) {
        StringBuilder p = new StringBuilder();
        task.subtarefas().stream()
                .filter(s -> s.estado() == Estado.CONCLUIDO && !s.id().equals(subtarefa.id()))
                .forEach(s -> p.append("- ").append(s.titulo()).append('\n'));
        return p.toString().strip();
    }

    private static String direcionamento(Direcionamento dado) {
        StringBuilder p = new StringBuilder("## Direcionamento já dado\n");
        respostas(p, dado);
        return p.toString().strip();
    }

    private static String git(Subtarefa subtarefa, String branch) {
        // A Task inteira segue os padrões da subtarefa: commit sim, push não.
        boolean commit = subtarefa == null || subtarefa.commit();
        boolean push = subtarefa != null && subtarefa.push();
        StringBuilder p = new StringBuilder();
        p.append("- Você está num worktree separado, na branch `").append(branch).append("`. ");
        if (commit) {
            p.append("Ao terminar, faça commit das suas mudanças nesta branch com uma mensagem clara. ");
        } else {
            p.append("Não faça commit: deixe as mudanças no worktree. ");
        }
        if (push) {
            p.append("Depois do commit, faça push desta branch para o remoto. ")
                    .append("Não troque de branch nem faça merge.");
        } else {
            p.append("Não troque de branch, não faça merge nem push.");
        }
        return p.toString();
    }

    /** Pedido de direcionamento lido da resposta do agente. */
    record Pedido(Direcionamento direcionamento) {
    }

    /**
     * Lê o bloco {@link #MARCA_DE_DIRECIONAMENTO} do fim da resposta: o texto antes dele vira o resumo, e cada
     * "Pergunta:" seguida de linhas "- " vira uma pergunta com as opções. Vazio se a resposta não pede
     * direcionamento ou se o bloco não tem nenhuma pergunta.
     */
    static Optional<Pedido> pedido(String resposta) {
        if (resposta == null) {
            return Optional.empty();
        }
        List<String> linhas = resposta.lines().toList();
        int marca = -1;
        for (int i = linhas.size() - 1; i >= 0; i--) {
            if (limpar(linhas.get(i)).toUpperCase(Locale.ROOT).startsWith(MARCA_DE_DIRECIONAMENTO)) {
                marca = i;
                break;
            }
        }
        if (marca < 0) {
            return Optional.empty();
        }
        List<Direcionamento.Pergunta> perguntas = new ArrayList<>();
        StringBuilder texto = null;
        List<String> opcoes = new ArrayList<>();
        String depoisDaMarca = limpar(linhas.get(marca)).substring(MARCA_DE_DIRECIONAMENTO.length()).strip();
        List<String> resto = new ArrayList<>();
        if (!depoisDaMarca.isEmpty()) {
            resto.add(depoisDaMarca);
        }
        resto.addAll(linhas.subList(marca + 1, linhas.size()));
        for (String bruta : resto) {
            String linha = limpar(bruta);
            if (linha.isEmpty() || linha.startsWith("```")) {
                continue;
            }
            Matcher opcao = OPCAO.matcher(linha);
            Matcher pergunta = PERGUNTA.matcher(linha);
            if (pergunta.matches()) {
                fechar(perguntas, texto, opcoes);
                texto = new StringBuilder(pergunta.group(1).strip());
                opcoes = new ArrayList<>();
            } else if (opcao.matches() && texto != null) {
                opcoes.add(opcao.group(1).strip());
            } else if (texto == null) {
                texto = new StringBuilder(linha);
            } else if (opcoes.isEmpty()) {
                texto.append(' ').append(linha);
            } else {
                // Linha solta depois das opções: continuação da última.
                opcoes.set(opcoes.size() - 1, opcoes.getLast() + " " + linha);
            }
        }
        fechar(perguntas, texto, opcoes);
        if (perguntas.isEmpty()) {
            return Optional.empty();
        }
        String resumo = String.join("\n", linhas.subList(0, marca)).strip();
        return Optional.of(new Pedido(new Direcionamento(resumo.isEmpty() ? null : resumo, perguntas)));
    }

    private static void fechar(List<Direcionamento.Pergunta> perguntas, StringBuilder texto, List<String> opcoes) {
        if (texto != null && !texto.isEmpty()) {
            perguntas.add(new Direcionamento.Pergunta(texto.toString(), opcoes.stream().distinct().toList(), null));
        }
    }

    /** Tira a ênfase do Markdown (**negrito**, `código` em volta) que o agente às vezes põe no bloco. */
    private static String limpar(String linha) {
        return linha.replace("**", "").strip();
    }

    private static void respostas(StringBuilder p, Direcionamento direcionamento) {
        for (Direcionamento.Pergunta pergunta : direcionamento.perguntas()) {
            p.append("Pergunta: ").append(pergunta.texto()).append('\n');
            p.append("Resposta: ").append(pergunta.resposta()).append("\n\n");
        }
    }

    /** Regras de como terminar a resposta: resumo, pedido de direcionamento ou falha. */
    private static String fim(boolean comCriterio) {
        StringBuilder p = new StringBuilder();
        p.append(comCriterio
                ? "- Ao terminar, responda com um resumo curto do que fez e de como o critério de aceite foi atendido.\n"
                : "- Ao terminar, responda com um resumo curto do que fez.\n");
        p.append("- Dúvidas pequenas e fáceis de desfazer (nomes, estilo, detalhes internos): decida com bom senso e ")
                .append("registre a suposição no resumo.\n");
        p.append("- Se faltar uma informação que muda o resultado e seria difícil de desfazer (modelo de dados, API ")
                .append("pública, dependência nova, apagar dados, sair do escopo, ou uma instrução com mais de uma ")
                .append("leitura razoável), não suponha: faça o que não depende da resposta e pare pedindo ")
                .append("direcionamento. A resposta virá numa próxima mensagem desta mesma conversa.\n");
        p.append("- Para pedir direcionamento, termine a resposta com um resumo curto do que já fez e, por último, ")
                .append("este bloco (até 3 perguntas, cada uma com 2 a 4 opções; a pessoa também pode responder ")
                .append("outra coisa):\n");
        p.append(MARCA_DE_DIRECIONAMENTO).append('\n')
                .append("Pergunta: <pergunta objetiva>\n")
                .append("- <opção>\n")
                .append("- <opção>\n");
        p.append("- Se não conseguir concluir, comece a resposta com \"").append(MARCA_DE_FALHA)
                .append("\" e explique o motivo.");
        return p.toString();
    }

    /** Se a resposta do agente diz que ele não conseguiu concluir. */
    static boolean falhou(String resposta) {
        return resposta != null && resposta.strip().toUpperCase(Locale.ROOT).startsWith(MARCA_DE_FALHA);
    }

    private static String permissoes(Permissoes permissoes) {
        StringBuilder p = new StringBuilder();
        p.append(permissoes.editarArquivos()
                ? "- Você pode criar e editar arquivos do projeto.\n"
                : "- Não edite arquivos: apenas leia e analise.\n");
        p.append(permissoes.rodarTestesEBuild()
                ? "- Você pode rodar testes e build.\n"
                : "- Não rode comandos de teste nem de build.\n");
        if (!permissoes.instalarDependencias()) {
            p.append("- Não instale nem atualize dependências.\n");
        }
        if (!permissoes.acessarInternet()) {
            p.append("- Não acesse a internet.\n");
        }
        return p.toString().strip();
    }

    /** O texto sem espaços nas pontas; vazio se não houver. */
    private static String texto(String texto) {
        return texto == null ? "" : texto.strip();
    }

    private static boolean temTexto(String texto) {
        return texto != null && !texto.isBlank();
    }
}
