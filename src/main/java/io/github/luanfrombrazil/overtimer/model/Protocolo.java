package io.github.luanfrombrazil.overtimer.model;

import java.util.List;
import java.util.UUID;

/**
 * Rotina contínua: o agente segue o passo a passo de tempos em tempos, conforme a agenda, e não termina.
 *
 * @param passos         passo a passo que o agente segue a cada execução
 * @param aceitaDiretoDe ids dos protocolos e projetos cujas tasks entram direto na fila de entrada; as dos demais
 *                       viram propostas
 * @param ativo          se o agendador roda o protocolo; desligado, só roda quando pedido
 * @param filaDeEntrada  tasks injetadas que o agente trata na próxima execução
 * @param propostas      tasks injetadas que esperam você aceitar ou recusar
 */
public record Protocolo(String id, String nome, String sigla, String passos, Agenda agenda, Provedor provedor,
                        Esforco esforco, Acessos acessos, List<String> aceitaDiretoDe, boolean ativo,
                        List<ItemFila> filaDeEntrada, List<ItemFila> propostas) {

    public Protocolo {
        agenda = agenda == null ? Agenda.vazia() : agenda;
        provedor = provedor == null ? Provedor.AUTOMATICO : provedor;
        esforco = esforco == null ? Esforco.MEDIUM : esforco;
        acessos = acessos == null ? Acessos.nenhum() : acessos;
        aceitaDiretoDe = aceitaDiretoDe == null ? List.of() : List.copyOf(aceitaDiretoDe);
        filaDeEntrada = filaDeEntrada == null ? List.of() : List.copyOf(filaDeEntrada);
        propostas = propostas == null ? List.of() : List.copyOf(propostas);
    }

    public static Protocolo novo(String nome, String sigla, String passos, Agenda agenda, Provedor provedor,
                                 Esforco esforco, Acessos acessos, List<String> aceitaDiretoDe) {
        return new Protocolo(UUID.randomUUID().toString(), nome, sigla, passos, agenda, provedor, esforco, acessos,
                aceitaDiretoDe, true, List.of(), List.of());
    }

    /** Troca o que se edita no pop-up, mantendo o id, o estado ligado e as filas. */
    public Protocolo comDefinicao(String nome, String sigla, String passos, Agenda agenda, Provedor provedor,
                                  Esforco esforco, Acessos acessos, List<String> aceitaDiretoDe) {
        return new Protocolo(id, nome, sigla, passos, agenda, provedor, esforco, acessos, aceitaDiretoDe, ativo,
                filaDeEntrada, propostas);
    }

    public Protocolo comAtivo(boolean ativo) {
        return new Protocolo(id, nome, sigla, passos, agenda, provedor, esforco, acessos, aceitaDiretoDe, ativo,
                filaDeEntrada, propostas);
    }

    public Protocolo comFilas(List<ItemFila> filaDeEntrada, List<ItemFila> propostas) {
        return new Protocolo(id, nome, sigla, passos, agenda, provedor, esforco, acessos, aceitaDiretoDe, ativo,
                filaDeEntrada, propostas);
    }

    /** Se as tasks injetadas pela origem entram direto na fila de entrada, sem virar proposta. */
    public boolean aceitaDireto(String origem) {
        return aceitaDiretoDe.contains(origem);
    }

    /** Propostas esperando resposta pedem atenção; o resto fica pendente até o histórico de execuções. */
    public Estado estado() {
        return propostas.isEmpty() ? Estado.PENDENTE : Estado.ATENCAO;
    }

    /** Texto curto sob o nome, na barra lateral. */
    public String detalhe() {
        if (!propostas.isEmpty()) {
            return propostas.size() + (propostas.size() == 1 ? " proposta" : " propostas");
        }
        return ativo ? null : "Desligado";
    }
}
