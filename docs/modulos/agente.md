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

## Onde o agente trabalha
- Projeto com git: numa branch `overtime/…` separada.
- Projeto sem git: direto na pasta do projeto.
