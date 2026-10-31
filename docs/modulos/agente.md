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
| Permissões | `--permission-mode`, `--allowedTools` |
| Modelo e esforço | `--model`, `--effort` (low, medium, high, xhigh, max) |
| Funções da app (MCP) | `--mcp-config` |

Na app: `claude -p --output-format stream-json --verbose --permission-mode dontAsk --effort <esforço>`, com o prompt pela entrada padrão. As permissões do projeto viram `--allowedTools` (leitura sempre; `Edit`/`Write` se pode editar; `Bash`/`PowerShell` se pode rodar testes, senão só `git` em projeto com git; `WebFetch`/`WebSearch` se pode acessar a internet). "Instalar dependências" é pedido no prompt, porque não dá para separar de outros comandos.

Codex e Gemini ainda não estão integrados: as Tasks com esses provedores falham com um aviso. "Automático" usa o Claude Code.

## Onde o agente trabalha
- Projeto com git: num worktree em `<pasta da app>/worktrees/<id da task>`, na branch `overtime/<título>-<id>` criada a partir da branch padrão. A pasta do projeto e a branch principal não são tocadas; o agente faz commit na branch da Task, sem merge nem push.
- Projeto sem git: direto na pasta do projeto.
