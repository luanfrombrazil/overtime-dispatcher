package io.github.luanfrombrazil.overtimer.service.agente;

import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Provedor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Claude Code em modo não interativo ({@code claude -p}), com a saída em stream-json.
 * <p>
 * O prompt vai pela entrada padrão (sem problemas de aspas na linha de comando do Windows). As permissões do
 * projeto viram {@code --allowedTools} com {@code --permission-mode dontAsk}: o que não foi liberado é negado
 * sem perguntar.
 */
public class ClaudeCodeAgente implements Agente {

    private static final int LINHAS_DE_ERRO = 5;

    private final Optional<List<String>> comando;
    private volatile Process atual;

    public ClaudeCodeAgente() {
        this(Executavel.localizar("claude"));
    }

    /** @param comando começo da linha de comando do CLI, ou vazio se não estiver instalado */
    ClaudeCodeAgente(Optional<List<String>> comando) {
        this.comando = comando;
    }

    @Override
    public Provedor provedor() {
        return Provedor.CLAUDE_CODE;
    }

    @Override
    public boolean disponivel() {
        return comando.isPresent();
    }

    @Override
    public ResultadoExecucao executar(Execucao execucao, Consumer<String> log) throws InterruptedException {
        if (comando.isEmpty()) {
            return ResultadoExecucao.falha("O Claude Code não foi encontrado no PATH.");
        }
        ProcessBuilder construtor = new ProcessBuilder(linhaDeComando(comando.get(), execucao))
                .directory(execucao.pasta().toFile());
        // Sem isso, um claude chamado de dentro de outra sessão do Claude Code pode se recusar a rodar.
        construtor.environment().remove("CLAUDECODE");

        Process processo;
        try {
            processo = construtor.start();
        } catch (IOException e) {
            return ResultadoExecucao.falha("Não foi possível iniciar o Claude Code: " + e.getMessage());
        }
        atual = processo;
        LeitorClaudeCode leitor = new LeitorClaudeCode(log);
        Deque<String> erros = new ArrayDeque<>();
        Thread saida = Thread.ofVirtual().start(() -> ler(processo.getInputStream(), leitor::processar));
        Thread saidaDeErro = Thread.ofVirtual().start(() -> ler(processo.getErrorStream(), linha -> {
            synchronized (erros) {
                erros.addLast(linha);
                if (erros.size() > LINHAS_DE_ERRO) {
                    erros.removeFirst();
                }
            }
        }));
        try (OutputStream entrada = processo.getOutputStream()) {
            entrada.write(execucao.prompt().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.accept("Não foi possível enviar o pedido: " + e.getMessage());
        }

        try {
            if (!processo.waitFor(execucao.limite().toMillis(), TimeUnit.MILLISECONDS)) {
                encerrar(processo);
                return new ResultadoExecucao(false,
                        "Tempo esgotado após " + execucao.limite().toMinutes() + " min.", leitor.sessao());
            }
            saida.join();
            saidaDeErro.join();
        } catch (InterruptedException e) {
            encerrar(processo);
            throw e;
        } finally {
            atual = null;
        }

        int codigo = processo.exitValue();
        if (leitor.terminou() && !leitor.erro() && codigo == 0) {
            return new ResultadoExecucao(true, leitor.resposta(), leitor.sessao());
        }
        String motivo = leitor.terminou() && leitor.resposta() != null && !leitor.resposta().isBlank()
                ? leitor.resposta()
                : "O Claude Code terminou com código " + codigo + ".";
        synchronized (erros) {
            if (!erros.isEmpty()) {
                motivo += "\n" + String.join("\n", erros);
            }
        }
        return new ResultadoExecucao(false, motivo, leitor.sessao());
    }

    @Override
    public void cancelar() {
        Process processo = atual;
        if (processo != null) {
            encerrar(processo);
        }
    }

    static List<String> linhaDeComando(List<String> comando, Execucao execucao) {
        List<String> linha = new ArrayList<>(comando);
        linha.addAll(List.of("-p", "--output-format", "stream-json", "--verbose",
                "--permission-mode", "dontAsk",
                "--effort", execucao.esforco().name().toLowerCase(Locale.ROOT)));
        if (execucao.modelo().apelido() != null) {
            linha.addAll(List.of("--model", execucao.modelo().apelido()));
        }
        linha.addAll(execucao.sessaoNova()
                ? List.of("--session-id", execucao.sessao())
                : List.of("--resume", execucao.sessao()));
        linha.add("--allowedTools");
        linha.add(String.join(",", ferramentas(execucao.permissoes(), execucao.usaGit())));
        return linha;
    }

    /** Ferramentas liberadas para o agente a partir das permissões do projeto. */
    static List<String> ferramentas(Permissoes permissoes, boolean usaGit) {
        List<String> ferramentas = new ArrayList<>(List.of("Read", "Glob", "Grep", "TodoWrite"));
        if (permissoes.editarArquivos()) {
            ferramentas.addAll(List.of("Edit", "Write", "NotebookEdit"));
        }
        if (permissoes.rodarTestesEBuild()) {
            ferramentas.addAll(List.of("Bash", "PowerShell"));
        } else if (usaGit) {
            ferramentas.addAll(List.of("Bash(git *)", "PowerShell(git *)"));
        }
        if (permissoes.acessarInternet()) {
            ferramentas.addAll(List.of("WebFetch", "WebSearch"));
        }
        return ferramentas;
    }

    private static void ler(InputStream fluxo, Consumer<String> linhas) {
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(fluxo, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                linhas.accept(linha);
            }
        } catch (IOException e) {
            // O processo foi encerrado; não há mais o que ler.
        }
    }

    /** Encerra o processo e os filhos dele (o claude.exe do Windows abre outro processo por baixo). */
    private static void encerrar(Process processo) {
        processo.descendants().forEach(ProcessHandle::destroyForcibly);
        processo.destroyForcibly();
    }
}
