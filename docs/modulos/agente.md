# Módulo `agente`

Executa o trabalho num agente de IA e devolve o resultado. Todos os provedores ficam atrás de uma interface comum, então o resto da app não sabe qual está rodando.

## Provedores
| Provedor | Situação |
|---|---|
| **Claude Code** | Instalado; opções confirmadas (versão 2.1.308) |
| **Codex** | A verificar quando instalado |
| **Gemini** (Gemini CLI ou Antigravity) | A verificar quando instalado |

Cada Task escolhe **provedor · modelo** (ex.: "Claude Code · Sonnet") e o **esforço**.

O agente recebe uma Task inteira (quando ela não tem subtarefas) ou uma subtarefa por vez; nos dois casos, o prompt leva a instrução e o critério de aceite da Task.

## Claude Code

| Necessidade | Opção |
|---|---|
| Rodar sem interação | `-p` |
| Saída estruturada / ao vivo | `--output-format json` / `stream-json` |
| Sessão compartilhada entre subtarefas | `--session-id` na primeira, `-r` nas seguintes |
| Permissões | `--permission-mode`, `--allowedTools`, `--disallowedTools` |
| Modelo e esforço | `--model`, `--effort` (low, medium, high, xhigh, max) |
| Funções da app (MCP) | `--mcp-config` |

Na app: `claude -p --output-format stream-json --verbose --permission-mode dontAsk --effort <esforço>`, com o prompt pela entrada padrão. As permissões do projeto viram `--allowedTools` (leitura sempre; `Edit`/`Write` só dentro da pasta de trabalho (`./**`) se pode editar; `Bash`/`PowerShell` se pode rodar testes, senão só os subcomandos git do dia a dia (`status`, `diff`, `log`, `add`, `commit`…, e `push` quando a subtarefa pede) em projeto com git; `WebFetch`/`WebSearch` se pode acessar a internet).

`--disallowedTools` vale mesmo com o shell liberado:
- sempre: ler ou editar credenciais da pasta do usuário (`~/.ssh`, `~/.aws`, `~/.git-credentials`, `~/.claude/.credentials.json`…);
- com git: trocar de branch, merge, rebase, `reset --hard`, apagar branches, `push --force`, `git config`/`remote`/`worktree`, e `push` quando a subtarefa não pede;
- sem internet: `WebFetch`/`WebSearch` e `curl`, `wget`, `Invoke-WebRequest`, `git clone`/`fetch`/`pull`…;
- sem "instalar dependências": `npm install`, `pip install`, `cargo add`, `winget install`…

Os bloqueios de comando são por prefixo, então não fecham todas as portas de um shell liberado (um script pode baixar algo); o prompt continua pedindo o mesmo. Na instalação pelo npm a linha de comando passa pelo `cmd.exe`: por isso o id da sessão (lido do `task.json`) só passa se for um UUID, e os bloqueios usam uma regra por comando, para caber no limite de ~8 mil caracteres do `cmd.exe`.

Ids de projeto e de Task (que viram nomes de pasta) não podem sair da pasta da aplicação, e a branch padrão não pode começar com `-` (viraria opção do git). As chamadas ao git rodam com `GIT_TERMINAL_PROMPT=0`, para não travar esperando senha.

Codex e Gemini ainda não estão integrados: as Tasks com esses provedores falham com um aviso. "Automático" usa o Claude Code.

## Onde o agente trabalha
- Projeto com git: num worktree em `<pasta da app>/worktrees/<id da task>`, na branch `overtime/<título>-<id>` criada a partir da branch padrão. A pasta do projeto e a branch principal não são tocadas; o agente faz commit na branch da Task, sem merge nem push.
- Projeto sem git: direto na pasta do projeto.
