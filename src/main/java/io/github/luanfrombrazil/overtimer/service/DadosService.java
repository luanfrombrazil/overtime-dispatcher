package io.github.luanfrombrazil.overtimer.service;

import io.github.luanfrombrazil.overtimer.model.BlocoAgendado;
import io.github.luanfrombrazil.overtimer.model.Consumo;
import io.github.luanfrombrazil.overtimer.model.Estado;
import io.github.luanfrombrazil.overtimer.model.Estatistica;
import io.github.luanfrombrazil.overtimer.model.ItemResumo;
import io.github.luanfrombrazil.overtimer.model.Sugestao;
import io.github.luanfrombrazil.overtimer.model.Trilha;

import java.util.List;

/**
 * Fonte dos dados das telas que ainda não têm serviço próprio (os projetos e os protocolos já estão no
 * {@link ProjetoService} e no {@link ProtocoloService}).
 * Por enquanto começa vazia e nada é salvo; cada módulo vai substituindo uma parte deste serviço.
 */
public class DadosService {

    private static final int SEMANAS_NO_ANO = 53;

    public int sugestoesDoOraculo() {
        return 0;
    }

    public List<Trilha> trilhas() {
        return List.of();
    }

    public List<Estatistica> estatisticas() {
        int[] vazio = new int[10];
        return List.of(
                new Estatistica("0", "concluídas · 7 dias", Estado.EM_ANDAMENTO, vazio),
                new Estatistica("0", "pendências", Estado.ATENCAO, vazio),
                new Estatistica("0", "falhas", Estado.ERRO, vazio),
                new Estatistica("0h", "overtime · 7 dias", Estado.CONCLUIDO, vazio));
    }

    /** Nível de atividade (0 a 4) de cada dia do último ano, semana a semana. */
    public int[][] atividade() {
        return new int[SEMANAS_NO_ANO][7];
    }

    public String resumoAtividade() {
        return "0 subtarefas no último ano";
    }

    public String horasDeOvertime() {
        return "0h de overtime";
    }

    /** Horário da última sessão de overtime, ou null se nunca houve uma. */
    public String horarioUltimaSessao() {
        return null;
    }

    public List<Consumo> consumoUltimaSessao() {
        return List.of();
    }

    public List<ItemResumo> ultimaSessao() {
        return List.of();
    }

    public int concluidasForaDoResumo() {
        return 0;
    }

    public List<ItemResumo> precisaDeVoce() {
        return List.of();
    }

    public List<BlocoAgendado> blocosDeHoje() {
        return List.of();
    }

    public List<String> prazos() {
        return List.of();
    }

    public List<Sugestao> sugestoes() {
        return List.of();
    }

    /** O que o Oráculo leu na última análise, ou null se ainda não analisou nada. */
    public String leituraDoOraculo() {
        return null;
    }
}
