package io.github.luanfrombrazil.overtimer.service.scheduler;

import io.github.luanfrombrazil.overtimer.model.Acessos;
import io.github.luanfrombrazil.overtimer.model.ItemFila;
import io.github.luanfrombrazil.overtimer.model.Protocolo;

import java.util.function.Function;

/**
 * Monta o pedido de uma execução de protocolo: o passo a passo, as tasks da fila de entrada e as regras dos acessos
 * (D-14). A lista de ferramentas do agente já fecha o que dá; as regras dizem o que ela não consegue fechar dentro de
 * cada site e conector.
 */
final class PromptProtocolo {

    /** Agenda própria do agente no calendário, a única em que ele pode mexer. */
    static final String AGENDA_DO_AGENTE = "Overtimer";

    private PromptProtocolo() {
    }

    /** @param nomeDe nome do protocolo ou projeto com o id dado, para dizer de onde veio cada task da fila */
    static String montar(Protocolo protocolo, Function<String, String> nomeDe) {
        StringBuilder p = new StringBuilder();
        p.append("Você é o agente do protocolo \"").append(protocolo.nome().strip())
                .append("\", uma rotina do Overtimer que roda de tempos em tempos. Siga o passo a passo abaixo.\n\n");

        p.append("## Passo a passo\n").append(texto(protocolo.passos())).append("\n\n");

        if (!protocolo.filaDeEntrada().isEmpty()) {
            p.append("## Fila de entrada\n")
                    .append("Trate também estas tasks, enviadas para esta execução:\n");
            for (ItemFila item : protocolo.filaDeEntrada()) {
                p.append("- ").append(texto(item.titulo()));
                if (item.origem() != null) {
                    p.append(" (de ").append(nomeDe.apply(item.origem())).append(')');
                }
                String descricao = texto(item.descricao());
                if (!descricao.isEmpty()) {
                    p.append(": ").append(descricao.replace("\n", "\n  "));
                }
                p.append('\n');
            }
            p.append('\n');
        }

        p.append("## Regras\n").append(regras(protocolo.acessos())).append("\n\n");

        p.append("## Ao terminar\n")
                .append("- Responda com um resumo curto do que encontrou e do que fez.\n")
                .append("- Se não conseguir concluir, comece a resposta com \"").append(Prompt.MARCA_DE_FALHA)
                .append("\" e explique o motivo.\n");
        return p.toString();
    }

    /** Regras da pasta de trabalho e de cada acesso: o que fazer com os marcados e a proibição dos demais. */
    static String regras(Acessos acessos) {
        StringBuilder p = new StringBuilder();
        p.append("- Você roda na pasta de trabalho do protocolo: leia e edite arquivos só dentro dela. ")
                .append("Guarde nela o que precisar consultar na próxima execução.\n");
        p.append("- Não rode comandos de terminal.\n");

        if (acessos.sites().isEmpty()) {
            p.append("- Não acesse sites.\n");
        } else {
            p.append("- Sites, só leitura: você pode ler ").append(String.join(", ", acessos.sites()))
                    .append(". Não envie formulários, não faça login, não publique nem altere nada neles, ")
                    .append("e não acesse outros sites.\n");
        }

        if (acessos.email() && acessos.conectorEmail() != null) {
            p.append("- E-mail, só leitura e rascunho: pelo conector `").append(acessos.conectorEmail())
                    .append("` você pode ler e-mails e criar rascunhos. Nunca envie, responda, encaminhe, apague, ")
                    .append("arquive nem modifique e-mails, marcadores ou rascunhos existentes.\n");
        } else {
            p.append("- Não acesse e-mail.\n");
        }

        if (acessos.calendario() && acessos.conectorCalendario() != null) {
            p.append("- Calendário, só a agenda do agente: pelo conector `").append(acessos.conectorCalendario())
                    .append("` você pode criar, editar e apagar eventos apenas na agenda \"").append(AGENDA_DO_AGENTE)
                    .append("\". Não crie, altere, apague nem responda eventos de outras agendas.\n");
        } else {
            p.append("- Não acesse o calendário.\n");
        }
        return p.toString().strip();
    }

    private static String texto(String texto) {
        return texto == null ? "" : texto.strip();
    }
}
