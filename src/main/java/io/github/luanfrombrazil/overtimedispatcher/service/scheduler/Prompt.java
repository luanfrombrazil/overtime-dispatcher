package io.github.luanfrombrazil.overtimedispatcher.service.scheduler;

import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.Permissoes;
import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Subtarefa;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;

import java.util.List;
import java.util.Locale;

/**
 * Monta o pedido de uma subtarefa para o agente e interpreta a resposta.
 */
final class Prompt {

    /** O agente começa a resposta com isto quando não consegue concluir a subtarefa. */
    static final String MARCA_DE_FALHA = "FALHOU:";

    private Prompt() {
    }

    /** @param branch branch do worktree, usada só quando o projeto usa git */
    static String montar(Projeto projeto, Task task, Subtarefa subtarefa, String branch) {
        StringBuilder p = new StringBuilder();
        p.append("Você é um agente executando trabalho automatizado no projeto \"").append(projeto.nome()).append("\".\n");
        paragrafo(p, projeto.descricao());

        p.append("\n## Task: ").append(task.titulo()).append('\n');
        paragrafo(p, task.descricao());
        List<Subtarefa> concluidas = task.subtarefas().stream()
                .filter(s -> s.estado() == Estado.CONCLUIDO && !s.id().equals(subtarefa.id()))
                .toList();
        if (!concluidas.isEmpty()) {
            p.append("\nJá concluído nesta task:\n");
            concluidas.forEach(s -> p.append("- ").append(s.titulo()).append('\n'));
        }

        p.append("\n## Subtarefa atual: ").append(subtarefa.titulo()).append('\n');
        if (temTexto(subtarefa.instrucao())) {
            p.append("\nInstrução:\n").append(subtarefa.instrucao().strip()).append('\n');
        }
        if (temTexto(subtarefa.criterioDeAceite())) {
            p.append("\nCritério de aceite:\n").append(subtarefa.criterioDeAceite().strip()).append('\n');
        }

        p.append("\n## Regras\n");
        p.append("- Faça só esta subtarefa; as próximas chegarão em mensagens seguintes desta mesma conversa.\n");
        p.append("- Ninguém vai responder perguntas durante a execução: decida com bom senso e registre as suposições no resumo.\n");
        permissoes(p, projeto.permissoes());
        if (projeto.usaGit()) {
            p.append("- Você está num worktree separado, na branch `").append(branch).append("`. ");
            if (subtarefa.commit()) {
                p.append("Ao terminar, faça commit das suas mudanças nesta branch com uma mensagem clara. ");
            } else {
                p.append("Não faça commit: deixe as mudanças no worktree. ");
            }
            if (subtarefa.push()) {
                p.append("Depois do commit, faça push desta branch para o remoto. ")
                        .append("Não troque de branch nem faça merge.\n");
            } else {
                p.append("Não troque de branch, não faça merge nem push.\n");
            }
        }
        p.append("- Ao terminar, responda com um resumo curto do que fez e de como o critério de aceite foi atendido.\n");
        p.append("- Se não conseguir concluir, comece a resposta com \"").append(MARCA_DE_FALHA)
                .append("\" e explique o motivo.\n");
        return p.toString();
    }

    /** Se a resposta do agente diz que ele não conseguiu concluir. */
    static boolean falhou(String resposta) {
        return resposta != null && resposta.strip().toUpperCase(Locale.ROOT).startsWith(MARCA_DE_FALHA);
    }

    private static void permissoes(StringBuilder p, Permissoes permissoes) {
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
    }

    private static void paragrafo(StringBuilder p, String texto) {
        if (temTexto(texto)) {
            p.append(texto.strip()).append('\n');
        }
    }

    private static boolean temTexto(String texto) {
        return texto != null && !texto.isBlank();
    }
}
