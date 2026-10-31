package io.github.luanfrombrazil.overtimedispatcher.service.prototipo;

import io.github.luanfrombrazil.overtimedispatcher.model.BlocoAgendado;
import io.github.luanfrombrazil.overtimedispatcher.model.Consumo;
import io.github.luanfrombrazil.overtimedispatcher.model.Estado;
import io.github.luanfrombrazil.overtimedispatcher.model.Estatistica;
import io.github.luanfrombrazil.overtimedispatcher.model.ItemFila;
import io.github.luanfrombrazil.overtimedispatcher.model.ItemResumo;
import io.github.luanfrombrazil.overtimedispatcher.model.Projeto;
import io.github.luanfrombrazil.overtimedispatcher.model.Protocolo;
import io.github.luanfrombrazil.overtimedispatcher.model.Subtarefa;
import io.github.luanfrombrazil.overtimedispatcher.model.Sugestao;
import io.github.luanfrombrazil.overtimedispatcher.model.TarefaPessoal;
import io.github.luanfrombrazil.overtimedispatcher.model.Task;
import io.github.luanfrombrazil.overtimedispatcher.model.Trilha;

import java.util.List;
import java.util.Random;

/**
 * Dados de exemplo do protótipo, iguais aos do Figma. Será substituído pela persistência real.
 */
public class DadosPrototipo {

    public List<Consumo> consumo() {
        return List.of(new Consumo("Claude", 52), new Consumo("Codex", 18), new Consumo("Gemini", 31));
    }

    public int notificacoesNaoLidas() {
        return 3;
    }

    public int sugestoesDoOraculo() {
        return 3;
    }

    public List<Projeto> projetos() {
        Projeto meuApp = new Projeto("Meu App", "MA", "1 verificação", Estado.ATENCAO,
                "C:\\code\\meu-app", "main", "Automático",
                List.of(
                        new Task("Corrigir login com e-mail em maiúsculas", "1 verificação", Estado.ATENCAO, List.of(
                                new Subtarefa("Reproduzir o bug com um teste", null, Estado.CONCLUIDO, -1),
                                new Subtarefa("Normalizar o e-mail no login", "Verificação", Estado.ATENCAO, -1),
                                new Subtarefa("Atualizar a documentação", null, Estado.PENDENTE, 1),
                                new Subtarefa("Revisar mensagens de erro", null, Estado.PENDENTE, -1))),
                        new Task("Tela de recuperação de senha", null, Estado.EM_ANDAMENTO, List.of()),
                        new Task("Remover dependências antigas", "Merge", Estado.ATENCAO, List.of()),
                        new Task("Cobrir o fluxo de login com testes",
                                "O login mudou 3 vezes este mês e não tem testes", Estado.ORACULO, List.of())),
                List.of("16:40 Corrigir login › Normalizar o e-mail · Claude Code",
                        "> equalsIgnoreCase após trim()",
                        "> mvn test -Dtest=LoginServiceTest",
                        "  Tests run: 6, Failures: 0",
                        "16:41 Aguardando verificação"));
        return List.of(meuApp,
                vazio("API de Pagamentos", "AP", null, Estado.EM_ANDAMENTO),
                vazio("Migração para JUnit 5", "JU", "Direcionamento", Estado.ATENCAO),
                vazio("Backup Pessoal", "BP", "Falhou", Estado.ERRO),
                vazio("Pesquisa de Crawlers", "PC", null, Estado.CONCLUIDO));
    }

    private static Projeto vazio(String nome, String sigla, String detalhe, Estado estado) {
        return new Projeto(nome, sigla, detalhe, estado, "C:\\code\\" + nome.toLowerCase().replace(' ', '-'),
                null, "Automático", List.of(), List.of());
    }

    public List<Protocolo> protocolos() {
        Protocolo agenda = new Protocolo("Agenda", "AG", "1 proposta", Estado.ATENCAO,
                "07:00 · 19:00 · próxima às 19:00",
                "1. Processe a fila de entrada: cada entrega vira um evento no dia e hora do prazo, com a descrição e o link.\n"
                        + "2. Lembrete 1 dia antes para entregas.\n"
                        + "3. Não duplique eventos com a mesma chave.",
                "Claude Code", "Baixo",
                List.of("Calendário · agenda Overtime"),
                List.of("Tarefas da faculdade"),
                List.of(new ItemFila("Entrega: Relatório de Física · 12/11", "Tarefas da faculdade"),
                        new ItemFila("Entrega: Lista 4 de Cálculo · 14/11", "Tarefas da faculdade")),
                List.of(new ItemFila("Reunião do grupo · 30/10 18:00", "Triagem do e-mail")),
                List.of("07:00 · 3 tasks na fila · 3 eventos criados · 1 lembrete",
                        "+ Entrega: Trabalho de Redes · 08/11 23:59",
                        "+ Entrega: Prova de Física · 10/11 08:00",
                        "~ Seminário de IA · horário ajustado"));
        return List.of(agenda,
                new Protocolo("Tarefas da faculdade", "TF", "3 novas", Estado.ATENCAO, "07:00 · 13:00",
                        "Entre no portal, abra cada disciplina e liste as atividades com prazo nos próximos 14 dias.",
                        "Claude Code", "Baixo", List.of("Site · portal da faculdade"), List.of(), List.of(), List.of(),
                        List.of()),
                new Protocolo("Triagem do e-mail", "EM", null, Estado.CONCLUIDO, "a cada 4h",
                        "Leia os e-mails não lidos e monte um resumo.", "Claude Code", "Baixo",
                        List.of("E-mail · leitura e rascunhos"), List.of(), List.of(), List.of(), List.of()));
    }

    public List<Trilha> trilhas() {
        Trilha trabalhos = new Trilha("Trabalhos da faculdade", "TF", "Entrega 12/11", Estado.ATENCAO,
                "Vault › Trilhas › Trabalhos da faculdade.md",
                List.of(
                        new TarefaPessoal("Trabalho de Redes", "Entrega 12/11", Estado.ATENCAO, 2, 2, 6,
                                "2/6 · próximo hoje 19:00"),
                        new TarefaPessoal("Lista 4 de Cálculo", null, Estado.EM_ANDAMENTO, 1, 2, 3,
                                "1/3 · próximo amanhã 14:00"),
                        new TarefaPessoal("Revisar sub-redes (vídeo de 20 min)",
                                "Você errou sub-redes na Lista 3", Estado.ORACULO, 0, 0, 0, null),
                        new TarefaPessoal("Relatório de Física", null, Estado.PENDENTE, 0, 0, 4, "0/4 · a agendar"),
                        new TarefaPessoal("Seminário de IA", null, Estado.CONCLUIDO, 4, 0, 4, "4/4")));
        return List.of(trabalhos,
                new Trilha("Cálculo II", "C2", null, Estado.EM_ANDAMENTO, "Vault › Trilhas › Cálculo II.md", List.of()),
                new Trilha("TCC", "TC", null, Estado.PENDENTE, "Vault › Trilhas › TCC.md", List.of()));
    }

    public List<Estatistica> estatisticas() {
        return List.of(
                new Estatistica("12", "concluídas · 7 dias", Estado.EM_ANDAMENTO, new int[]{2, 3, 1, 4, 3, 5, 4, 6, 5, 7}),
                new Estatistica("3", "pendências", Estado.ATENCAO, new int[]{1, 2, 2, 1, 3, 2, 4, 3, 2, 3}),
                new Estatistica("1", "falha", Estado.ERRO, new int[]{0, 1, 0, 0, 2, 1, 0, 0, 1, 1}),
                new Estatistica("31h", "overtime · 7 dias", Estado.CONCLUIDO, new int[]{3, 4, 2, 5, 4, 6, 5, 4, 6, 5}));
    }

    /** Nível de atividade (0 a 4) de cada dia do último ano, semana a semana. */
    public int[][] atividade() {
        Random aleatorio = new Random(7);
        int semanas = 53;
        int[][] niveis = new int[semanas][7];
        for (int s = 0; s < semanas; s++) {
            double recencia = s / (double) semanas;
            double chance = recencia > 0.65 ? 0.55 : recencia > 0.4 ? 0.2 : 0.08;
            for (int d = 0; d < 7; d++) {
                if (aleatorio.nextDouble() < chance) {
                    niveis[s][d] = 1 + aleatorio.nextInt(recencia > 0.65 ? 4 : 2);
                }
            }
        }
        return niveis;
    }

    public String resumoAtividade() {
        return "247 subtarefas no último ano";
    }

    public String horasDeOvertime() {
        return "312h de overtime";
    }

    public String horarioUltimaSessao() {
        return "ontem 22:00–06:10 · 8h10";
    }

    public List<Consumo> consumoUltimaSessao() {
        return List.of(new Consumo("Claude", 38), new Consumo("Codex", 12), new Consumo("Gemini", 5));
    }

    public List<ItemResumo> ultimaSessao() {
        return List.of(
                new ItemResumo("API de Pagamentos › Documentar endpoints", null, Estado.CONCLUIDO),
                new ItemResumo("Pesquisa de Crawlers › Comparar bibliotecas", null, Estado.CONCLUIDO),
                new ItemResumo("Meu App › Corrigir login", "1 verificação", Estado.ATENCAO),
                new ItemResumo("Migração para JUnit 5 › Migrar testes", "Direcionamento", Estado.ATENCAO));
    }

    public int concluidasForaDoResumo() {
        return 2;
    }

    public List<ItemResumo> precisaDeVoce() {
        return List.of(
                new ItemResumo("Backup Pessoal", "Falhou", Estado.ERRO),
                new ItemResumo("Meu App", "1 verificação", Estado.ATENCAO),
                new ItemResumo("Migração para JUnit 5", "Direcionamento", Estado.ATENCAO));
    }

    public List<BlocoAgendado> blocosDeHoje() {
        return List.of(new BlocoAgendado("14:00", "Cálculo II › Lista 4 · 1h"),
                new BlocoAgendado("19:00", "Redes › Trabalho · 1h"));
    }

    public List<String> prazos() {
        return List.of("Redes 12/11", "Física 15/11");
    }

    public List<Sugestao> sugestoes() {
        return List.of(
                new Sugestao("Sugestão", "Meu App › Cobrir o fluxo de login com testes",
                        "O login mudou 3 vezes este mês e não tem testes", "Recusar", "Aceitar", 0, 0),
                new Sugestao("Diagnóstico", "API de Pagamentos está parado há 12 dias",
                        "Nenhuma task concluída desde 17/10; a fila está vazia", "Ver projeto", "Sugerir tasks", 0, 0),
                new Sugestao("Estudo", "Cálculo II › Integrais por partes: 3 vídeos + 10 exercícios",
                        "Prova em 05/11; a Lista 3 teve 4 erros nesse tema", "Recusar", "Aceitar", 0, 0),
                new Sugestao("Roadmap", "Cálculo II · etapa 4 de 9",
                        "Próxima etapa: Integrais trigonométricas", "Ver roadmap", null, 4, 9),
                new Sugestao("Protocolo", "Novo protocolo: \"Avisos da coordenação\"",
                        "O portal tem uma página de avisos que muda toda semana", "Recusar", "Criar", 0, 0));
    }

    public String leituraDoOraculo() {
        return "Leu 5 projetos, 3 protocolos e 3 trilhas · há 20 min";
    }
}
