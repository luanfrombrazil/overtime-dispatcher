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
import java.util.regex.Pattern;

/**
 * Claude Code em modo não interativo ({@code claude -p}), com a saída em stream-json.
 * <p>
 * O prompt vai pela entrada padrão (sem problemas de aspas na linha de comando do Windows). As permissões do
 * projeto viram {@code --allowedTools} com {@code --permission-mode dontAsk}: o que não foi liberado é negado
 * sem perguntar. Por cima disso, {@code --disallowedTools} barra o que nunca deve acontecer sem supervisão (ler
 * credenciais, push não pedido, trocar de branch) e o que as permissões negam mas um shell liberado ainda
 * permitiria (baixar da internet, instalar dependências).
 */
public class ClaudeCodeAgente implements Agente {

    private static final int LINHAS_DE_ERRO = 5;

    private static final Pattern UUID =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

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

    private static final List<String> INTERNET = List.of(
            "curl", "wget", "Invoke-WebRequest", "Invoke-RestMethod", "iwr ", "irm ", "Start-BitsTransfer",
            "git clone", "git fetch", "git pull");

    private static final List<String> INSTALACAO = List.of(
            "npm install", "npm i ", "npm ci", "npm add", "npm update", "yarn add", "pnpm add", "pnpm install",
            "pip install", "pip3 install", "python -m pip install", "py -m pip install", "uv add",
            "uv pip install", "poetry add", "pipx install", "cargo add", "cargo install", "go get", "go install",
            "gem install", "bundle install", "composer require", "composer install", "dotnet add",
            "winget install", "choco install", "scoop install", "apt install", "apt-get install",
            "brew install", "Install-Module", "Install-Package");

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
        if (!sessaoValida(execucao.sessao())) {
            // O id vem do task.json e vai para a linha de comando (que pode passar pelo cmd.exe): só um UUID passa.
            return ResultadoExecucao.falha("Id de sessão inválido: " + execucao.sessao());
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
        linha.add(String.join(",", ferramentas(execucao.permissoes(), execucao.usaGit(), execucao.push())));
        linha.add("--disallowedTools");
        linha.add(String.join(",", bloqueadas(execucao.permissoes(), execucao.usaGit(), execucao.push())));
        return linha;
    }

    /** Se o id de sessão é um UUID, o único formato que a app cria e o Claude Code devolve. */
    static boolean sessaoValida(String sessao) {
        return sessao != null && UUID.matcher(sessao).matches();
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
        return ferramentas;
    }

    /**
     * Regras de bloqueio, que valem mesmo com o shell liberado. São por prefixo de comando: não fecham todas as
     * portas de um shell, mas cortam o caminho óbvio para o que as permissões negam.
     */
    static List<String> bloqueadas(Permissoes permissoes, boolean usaGit, boolean push) {
        List<String> bloqueadas = new ArrayList<>();
        for (String segredo : SEGREDOS) {
            bloqueadas.add("Read(" + segredo + ")");
            bloqueadas.add("Edit(" + segredo + ")");
        }
        List<String> comandos = new ArrayList<>();
        if (usaGit) {
            comandos.addAll(GIT_PROIBIDO);
            if (!push) {
                comandos.add("git push");
            }
        }
        if (!permissoes.acessarInternet()) {
            bloqueadas.addAll(List.of("WebFetch", "WebSearch"));
            comandos.addAll(INTERNET);
        }
        if (!permissoes.instalarDependencias()) {
            comandos.addAll(INSTALACAO);
        }
        // Uma regra por comando, com o * colado: a linha inteira passa pelo cmd.exe, limitado a ~8 mil caracteres.
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
