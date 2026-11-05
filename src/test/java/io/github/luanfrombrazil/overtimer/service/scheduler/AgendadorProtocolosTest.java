package io.github.luanfrombrazil.overtimer.service.scheduler;

import io.github.luanfrombrazil.overtimer.model.Acessos;
import io.github.luanfrombrazil.overtimer.model.Agenda;
import io.github.luanfrombrazil.overtimer.model.Configuracoes;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.HorarioFuncionamento;
import io.github.luanfrombrazil.overtimer.model.ItemFila;
import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Protocolo;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.model.Task;
import io.github.luanfrombrazil.overtimer.service.ProjetoService;
import io.github.luanfrombrazil.overtimer.service.ProtocoloService;
import io.github.luanfrombrazil.overtimer.service.agente.Agentes;
import io.github.luanfrombrazil.overtimer.service.agente.AreaDeTrabalho;
import io.github.luanfrombrazil.overtimer.service.agente.Execucao;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProjetos;
import io.github.luanfrombrazil.overtimer.service.persistence.RepositorioProtocolos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendadorProtocolosTest {

    /** Relógio parado que o teste avança. */
    static class Relogio extends Clock {
        private volatile Instant agora;

        Relogio(LocalDateTime inicio) {
            ir(inicio);
        }

        void ir(LocalDateTime quando) {
            agora = quando.atZone(ZoneId.systemDefault()).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private static final LocalDateTime DIA = LocalDateTime.of(2026, 11, 2, 0, 0);

    @TempDir
    Path dados;

    @TempDir
    Path pastaDoProjeto;

    private ProjetoService projetos;
    private ProtocoloService protocolos;
    private AgendadorTest.AgenteFalso agente;
    private Agendador agendador;
    private Relogio relogio;

    @BeforeEach
    void preparar() {
        projetos = new ProjetoService(new RepositorioProjetos(dados));
        protocolos = new ProtocoloService(new RepositorioProtocolos(dados));
        agente = new AgendadorTest.AgenteFalso();
        agendador = new Agendador(projetos, protocolos, new Agentes(List.of(agente)), new AreaDeTrabalho(dados));
        relogio = new Relogio(DIA.withHour(7).withMinute(59));
        agendador.usarRelogio(relogio);
    }

    @AfterEach
    void encerrar() {
        agendador.encerrar();
    }

    private Protocolo protocoloAs8() {
        return protocolos.salvar(Protocolo.novo("Faculdade", "FA", "Listar as entregas do portal",
                Agenda.nosHorarios(List.of(LocalTime.of(8, 0))), Provedor.CLAUDE_CODE, Esforco.LOW,
                Acessos.nenhum(), List.of()));
    }

    /** Liga o agendador e espera o primeiro ciclo, que começa a contar a agenda dos protocolos. */
    private void iniciar() throws InterruptedException {
        agendador.iniciar();
        esperarCiclo();
    }

    private void esperarCiclo() throws InterruptedException {
        Thread.sleep(200);
        esperar(() -> !agendador.executando());
    }

    private static void esperar(BooleanSupplier condicao) throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        while (!condicao.getAsBoolean() && System.currentTimeMillis() < limite) {
            Thread.sleep(20);
        }
    }

    private void rodarAs(LocalDateTime quando) throws InterruptedException {
        relogio.ir(quando);
        agendador.agora();
        esperarCiclo();
    }

    @Test
    void rodaNoHorarioNaPastaDeTrabalho() throws InterruptedException {
        Protocolo protocolo = protocoloAs8();
        iniciar();
        assertEquals(0, agente.execucoes.size(), "antes das 8h não roda");

        rodarAs(DIA.withHour(8).withSecond(30));
        assertEquals(1, agente.execucoes.size());
        Execucao execucao = agente.execucoes.getFirst();
        assertEquals(dados.resolve("protocolos-rotina").resolve(protocolo.id()).resolve("trabalho"), execucao.pasta());
        assertTrue(execucao.prompt().contains("Listar as entregas do portal"));
        assertNotNull(execucao.acessos(), "roda com as ferramentas do protocolo");
        assertTrue(execucao.sessaoNova());
        assertTrue(agendador.log(protocolo.id()).stream().anyMatch(l -> l.contains("Concluído")));

        rodarAs(DIA.withHour(9));
        assertEquals(1, agente.execucoes.size(), "o horário das 8h já rodou hoje");

        rodarAs(DIA.plusDays(1).withHour(8).withMinute(1));
        assertEquals(2, agente.execucoes.size(), "no dia seguinte roda de novo");
    }

    @Test
    void desligadoNaoRoda() throws InterruptedException {
        Protocolo protocolo = protocolos.salvar(protocoloAs8().comAtivo(false));
        iniciar();

        rodarAs(DIA.withHour(8).withMinute(30));
        assertEquals(0, agente.execucoes.size());

        // Ligado depois do horário, não roda o que passou enquanto estava desligado.
        protocolos.definirAtivo(protocolo.id(), true);
        rodarAs(DIA.withHour(8).withMinute(31));
        assertEquals(0, agente.execucoes.size());

        rodarAs(DIA.plusDays(1).withHour(8).withSecond(5));
        assertEquals(1, agente.execucoes.size());
    }

    @Test
    void rodarAgoraRodaLogoForaDaAgendaEUmaVezSo() throws InterruptedException {
        Protocolo protocolo = protocoloAs8();
        iniciar();
        assertEquals(0, agente.execucoes.size());

        agendador.rodarAgora(protocolo.id());
        esperar(() -> agente.execucoes.size() >= 1);
        esperarCiclo();
        assertEquals(1, agente.execucoes.size(), "às 7h59, fora da agenda");

        agendador.agora();
        esperarCiclo();
        assertEquals(1, agente.execucoes.size(), "o pedido vale uma execução");

        rodarAs(DIA.withHour(8).withSecond(30));
        assertEquals(2, agente.execucoes.size(), "o horário da agenda continua valendo");
    }

    @Test
    void rodarAgoraRodaMesmoDesligado() throws InterruptedException {
        Protocolo protocolo = protocolos.salvar(protocoloAs8().comAtivo(false));
        iniciar();

        agendador.rodarAgora(protocolo.id());
        esperar(() -> agente.execucoes.size() >= 1);
        esperarCiclo();
        assertEquals(1, agente.execucoes.size());

        rodarAs(DIA.withHour(8).withSecond(30));
        assertEquals(1, agente.execucoes.size(), "desligado, a agenda não roda");
    }

    @Test
    void rodarAgoraComOAgendadorPausadoEsperaEleLigar() throws InterruptedException {
        Protocolo protocolo = protocoloAs8();
        agendador.rodarAgora(protocolo.id());
        Thread.sleep(200);
        assertEquals(0, agente.execucoes.size());

        iniciar();
        esperar(() -> agente.execucoes.size() >= 1);
        assertEquals(1, agente.execucoes.size());
    }

    @Test
    void intervaloContaDaUltimaExecucao() throws InterruptedException {
        protocolos.salvar(Protocolo.novo("E-mail", "EM", "Ler a caixa", Agenda.aCada(30), Provedor.CLAUDE_CODE,
                Esforco.LOW, Acessos.nenhum(), List.of()));
        iniciar();
        assertEquals(0, agente.execucoes.size(), "o intervalo conta de quando o agendador viu o protocolo");

        rodarAs(DIA.withHour(8).withMinute(29));
        assertEquals(1, agente.execucoes.size());
        rodarAs(DIA.withHour(8).withMinute(50));
        assertEquals(1, agente.execucoes.size());
        rodarAs(DIA.withHour(9));
        assertEquals(2, agente.execucoes.size());
    }

    @Test
    void foraDoHorarioDeFuncionamentoEsperaAbrir() throws InterruptedException {
        protocoloAs8();
        agendador.usarHorario(HorarioFuncionamento.vazio());
        iniciar();

        rodarAs(DIA.withHour(8).withSecond(30));
        assertEquals(0, agente.execucoes.size());

        agendador.usarHorario(HorarioFuncionamento.sempre());
        esperarCiclo();
        assertEquals(1, agente.execucoes.size(), "o horário que passou roda uma vez quando abre");
    }

    @Test
    void respeitaOLimiteDeUso() throws InterruptedException {
        AtomicInteger uso = new AtomicInteger(95);
        agendador.usarConsumo(p -> uso.get());
        agendador.aplicar(Configuracoes.padrao()); // automação até 90%
        agendador.usarRelogio(relogio);
        protocoloAs8();
        iniciar();

        rodarAs(DIA.withHour(8).withSecond(30));
        assertEquals(0, agente.execucoes.size());

        uso.set(40);
        rodarAs(DIA.withHour(8).withMinute(5));
        assertEquals(1, agente.execucoes.size());
    }

    @Test
    void passaNaFrenteDaTaskUmDeCadaVezETiraAFilaTratada() throws InterruptedException {
        Protocolo protocolo = protocolos.salvar(protocoloAs8().comFilas(
                List.of(ItemFila.novo("Marcar a prova", "sexta", null)), List.of()));
        Projeto projeto = projetos.salvar(Projeto.novo("P", "P", "", pastaDoProjeto.toString(), null,
                Provedor.AUTOMATICO, Esforco.LOW, Permissoes.padrao()));
        // Criada antes de ligar o agendador, para não ficar retida até o próximo ciclo do intervalo.
        projetos.salvarTask(projeto, Task.nova("Task da fila", "Faça a task", Provedor.AUTOMATICO, Esforco.LOW));
        // Fora do horário de funcionamento no começo, para a Task não rodar antes de o protocolo chegar a vez.
        agendador.usarHorario(HorarioFuncionamento.vazio());
        iniciar();

        relogio.ir(DIA.withHour(8).withSecond(30));
        agendador.usarHorario(HorarioFuncionamento.sempre());
        esperar(() -> agente.execucoes.size() >= 2);
        esperarCiclo();

        assertEquals(2, agente.execucoes.size());
        assertNotNull(agente.execucoes.get(0).acessos(), "o protocolo vem primeiro");
        assertTrue(agente.execucoes.get(0).prompt().contains("Marcar a prova"));
        assertNull(agente.execucoes.get(1).acessos(), "depois, a Task");
        assertTrue(protocolos.buscar(protocolo.id()).orElseThrow().filaDeEntrada().isEmpty());
    }
}
