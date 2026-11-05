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
 * projeto viram {@code --allowedTools}. Por cima disso, {@code --disallowedTools} barra, mesmo com o shell liberado, o que nunca deve
 * acontecer sem supervisão: ler credenciais, trocar de branch ou reescrever o histórico do worktree, e push que a
 * subtarefa não pediu. Internet e instalação de dependências com o shell liberado continuam pedidas no prompt:
 * por prefixo de comando não dá para fechar.
 */
public class ClaudeCodeAgente implements Agente {

    private static final int LINHAS_DE_ERRO = 5;

    /** Subcomandos git que o agente usa para ver e registrar o trabalho na branch da Task. */
    private static final List<String> GIT_DO_DIA_A_DIA = List.of(
            "status", "diff", "log", "show", "add", "rm", "mv", "commit", "restore", "rev-parse", "ls-files",
            "blame", "grep");

    /** Credenciais e chaves na pasta do usuário. */
    private static final List<String> SEGREDOS = List.of(
            "~/.ssh/**", "~/.aws/**", "~/.azure/**", "~/.gnupg/**", "~/.kube/**", "~/.docker/config.json",
            "~/.config/gh/**", "~/.config/gcloud/**", "~/.git-credentials", "~/.npmrc", "~/.pypirc",
            "~/.netrc", "~/.m2/settings.xml", "~/.gradle/gradle.properties", "~/.claude/.credentials.json",
            "~/.codex/auth.json");

    /** Sempre fora: trocam de branch, reescrevem o histórico, apagam branches ou mexem na configuração. */
    private static final List<String> GIT_PROIBIDO = List.of(
            "git checkout", "git switch", "git merge", "git rebase", "git reset --hard", "git branch -D",
            "git branch -d", "git push --force", "git push -f", "git config", "git remote", "git worktree",
            "git filter-branch", "git update-ref");

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
                "--effort", execucao.esforco().name().toLowerCase(Locale.ROOT)));
        if (execucao.modelo().apelido() != null) {
            linha.addAll(List.of("--model", execucao.modelo().apelido()));
        }
        linha.addAll(execucao.sessaoNova()
                ? List.of("--session-id", execucao.sessao())
                : List.of("--resume", execucao.sessao()));
        linha.add("--allowedTools");
        linha.add(String.join(",", ferramentas(execucao.permissoes(), execucao.usaGit(), execucao.push())));
        linha.add("--disallowedTools");
        linha.add(String.join(",", bloqueadas(execucao.usaGit(), execucao.push())));
        return linha;
    }

    /**
     * Ferramentas liberadas para o agente a partir das permissões do projeto. A edição fica presa à pasta de
     * trabalho ({@code ./**} é relativo à pasta em que o Claude Code roda). Sem shell liberado, só os comandos
     * git do dia a dia da Task: um {@code git *} genérico deixaria passar {@code git config}/{@code -c}, que
     * executam qualquer programa.
     */
    static List<String> ferramentas(Permissoes permissoes, boolean usaGit, boolean push) {
        List<String> ferramentas = new ArrayList<>(List.of("Read", "Glob", "Grep", "TodoWrite"));
        if (permissoes.editarArquivos()) {
            ferramentas.addAll(List.of("Edit(./**)", "Write(./**)", "NotebookEdit(./**)"));
        }
        if (permissoes.rodarTestesEBuild()) {
            ferramentas.addAll(List.of("Bash", "PowerShell"));
        } else if (usaGit) {
            List<String> git = new ArrayList<>(GIT_DO_DIA_A_DIA);
            if (push) {
                git.add("push");
            }
            for (String subcomando : git) {
                ferramentas.add("Bash(git " + subcomando + ")");
                ferramentas.add("Bash(git " + subcomando + " *)");
                ferramentas.add("PowerShell(git " + subcomando + ")");
                ferramentas.add("PowerShell(git " + subcomando + " *)");
            }
        }
        if (permissoes.acessarInternet()) {
            ferramentas.addAll(List.of("WebFetch", "WebSearch"));
        }
        // No modo -p não há a quem perguntar: ferramenta de conector fora desta lista é negada.
        permissoes.conectores().forEach(c -> ferramentas.add("mcp__" + c));
        return ferramentas;
    }

    /**
     * Regras de bloqueio, que valem mesmo com o shell liberado: ler credenciais e, com git, o que tira o worktree da
     * branch da Task ou mexe no repositório (e o push que a subtarefa não pediu). A edição já fica presa à pasta de
     * trabalho por {@link #ferramentas}.
     */
    static List<String> bloqueadas(boolean usaGit, boolean push) {
        List<String> bloqueadas = new ArrayList<>();
        for (String segredo : SEGREDOS) {
            bloqueadas.add("Read(" + segredo + ")");
        }
        List<String> comandos = new ArrayList<>();
        if (usaGit) {
            comandos.addAll(GIT_PROIBIDO);
            if (!push) {
                comandos.add("git push");
            }
        }
        // Uma regra por comando, com o * colado: instalado pelo npm, o claude roda pelo cmd.exe, limitado a ~8 mil
        // caracteres.
        for (String comando : comandos) {
            bloqueadas.add("Bash(" + comando + "*)");
            bloqueadas.add("PowerShell(" + comando + "*)");
        }
        return bloqueadas;
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
