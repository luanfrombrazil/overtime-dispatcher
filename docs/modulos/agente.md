# Módulo `agente`

Executa uma subtarefa num agente de código (Claude Code, Codex, Gemini...) e devolve o resultado num formato comum. É o **único** lugar da aplicação que sabe que existem provedores diferentes.

## Interface

```java
public interface Agente {
    ProvedorId id();
    boolean disponivel();                       // CLI instalado e autenticado?
    Capacidades capacidades();                  // o que este agente suporta
    ResultadoExecucao executar(PedidoExecucao pedido, Consumer<String> aoReceberLinha);
    void cancelar(String execucaoId);
}
```

| Tipo | Conteúdo |
|---|---|
| `PedidoExecucao` | prompt, pasta de trabalho, `sessionId` (opcional), `Permissoes`, timeout |
| `ResultadoExecucao` | sucesso/erro, texto final, novo `sessionId`, `Consumo`, código de saída, `TipoErro` |
| `TipoErro` | `LIMITE_DE_USO`, `TIMEOUT`, `NAO_AUTENTICADO`, `FALHA_DO_AGENTE`, `CANCELADO` |
| `Capacidades` | retoma sessão? informa consumo? saída em JSON? |

O `TipoErro` é o que permite ao agendador tratar todos os provedores igual: por exemplo, `LIMITE_DE_USO` pausa **aquele** provedor, seja qual for.

## Peças comuns

| Classe | Papel |
|---|---|
| `PromptBuilder` | Monta o prompt a partir de Task + Subtarefa + feedback. Igual para todos os provedores. |
| `ProcessoAgente` | Base reutilizável: `ProcessBuilder`, envio do prompt por stdin, leitura de stdout/stderr em thread própria, timeout e cancelamento. |
| `RegistroDeAgentes` | Lista de agentes habilitados na configuração; o agendador pede um por `ProvedorId`. |

Cada adaptador só precisa dizer **qual comando montar** e **como interpretar a saída**.

## Prompt (modelo)

```
Você está trabalhando na Task "<titulo>".
Contexto: <descricao>

Execute APENAS a subtarefa abaixo. Não avance para outras.

Subtarefa <id>: <titulo>
Instrução: <instrucao>
Critério de aceite: <criterioDeAceite>

[se houver feedback]
Na tentativa anterior você recebeu este retorno do revisor:
- <feedback>

Ao terminar, responda com um resumo curto do que fez e de como o critério de aceite foi atendido.
```

## Adaptadores

> Os comandos abaixo são o ponto de partida e **precisam ser confirmados** na versão instalada de cada CLI antes da implementação — opções de linha de comando mudam com frequência.

### Claude Code (`CLAUDE_CODE`)

- Modo não interativo: `claude -p` com o prompt pelo stdin.
- Saída estruturada: `--output-format json` (traz resultado, `session_id` e consumo).
- Continuar sessão: `-r <sessionId>`.
- Permissões: `--permission-mode` e `--allowedTools`.

### Codex (`CODEX`)

- Modo não interativo: `codex exec`.
- Verificar: saída em JSON, como retomar sessão e como restringir permissões (sandbox/aprovação).

### Gemini (`GEMINI`)

- Gemini CLI tem modo não interativo (`gemini -p`).
- **Antigravity** é um ambiente/IDE com agentes; é preciso verificar se ele oferece execução sem interface (linha de comando ou API). Se não oferecer, o adaptador usa o Gemini CLI, que consome da mesma conta.
- Verificar: saída em JSON, retomada de sessão e permissões.

### Novo provedor

1. Criar um `ProvedorId`.
2. Implementar `Agente` estendendo `ProcessoAgente`.
3. Implementar a `FonteDeUsage` dele (ver [consumo](consumo.md)).
4. Registrar na configuração.

Nada mais na aplicação muda.

## Pasta de trabalho

- Task com repositório → `repositorio.caminho`.
- Task sem repositório → `workspace/<taskId>/`, criada na primeira execução.

## Logs

Cada execução grava a saída completa em `logs/<taskId>/<execucaoId>.log`, além de mandar linha a linha para a tela pelo `aoReceberLinha`.

## Segurança

- Nunca liberar tudo por padrão: as permissões vêm do repositório/Task e são explícitas.
- O prompt vai por stdin, nunca concatenado num comando de shell.
- Timeout sempre definido; processo morto ao estourar.
