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
| `PedidoExecucao` | prompt, pasta de trabalho, `sessionId` (opcional), `Permissoes`, `Esforco`, timeout |
| `ResultadoExecucao` | sucesso/erro, texto final, novo `sessionId`, `Consumo`, código de saída, `TipoErro` |
| `TipoErro` | `LIMITE_DE_USO`, `TIMEOUT`, `NAO_AUTENTICADO`, `FALHA_DO_AGENTE`, `CANCELADO` |
| `Capacidades` | retoma sessão? informa consumo? saída em JSON? quais níveis de esforço suporta? |

O `TipoErro` é o que permite ao agendador tratar todos os provedores igual: por exemplo, `LIMITE_DE_USO` pausa **aquele** provedor, seja qual for.

## Peças comuns

| Classe | Papel |
|---|---|
| `PromptBuilder` | Monta o prompt a partir de Task + Subtarefa + feedback. Igual para todos os provedores. |
| `ProcessoAgente` | Base reutilizável: `ProcessBuilder`, envio do prompt por stdin, leitura de stdout/stderr em thread própria, timeout e cancelamento. |
| `RegistroDeAgentes` | Lista de agentes habilitados na configuração; o agendador pede um por `ProvedorId`. |

Cada adaptador só precisa dizer **qual comando montar** (incluindo como traduzir `Permissoes` e `Esforco`) e **como interpretar a saída**.

## Esforço

O domínio usa níveis neutros (`PADRAO`, `BAIXO`, `MEDIO`, `ALTO`, `MUITO_ALTO`, `MAXIMO`; ver [domínio](../dominio.md#esforco)). Cada adaptador mantém uma tabela de tradução para o seu agente:

| Neutro | Claude Code (confirmado, D-42) | Codex | Gemini |
|---|---|---|---|
| `PADRAO` | não envia | não envia | não envia |
| `BAIXO` | `--effort low` | a confirmar | a confirmar |
| `MEDIO` | `--effort medium` | a confirmar | a confirmar |
| `ALTO` | `--effort high` | a confirmar | a confirmar |
| `MUITO_ALTO` | `--effort xhigh` | a confirmar | a confirmar |
| `MAXIMO` | `--effort max` | a confirmar | a confirmar |

- Se o agente não tiver um nível equivalente, usa o mais próximo e registra um aviso no log.
- Se o agente não permitir escolher esforço, `Capacidades` informa isso e a UI mostra o campo desabilitado para Tasks fixadas nele.

## Modelo

O campo `modelo` da Task (decisão D-40) é repassado ao CLI quando preenchido; vazio usa o padrão do CLI. No Claude Code: `--model <alias ou nome completo>`. Cada adaptador informa em `Capacidades` a lista de modelos que a UI oferece.

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

Se não tiver informação suficiente para uma decisão importante, não suponha: peça
direcionamento com `odctl direcionamento pedir "sua pergunta, com as opções que você vê"`
e encerre.

Se precisar alterar a fila (adicionar subtarefas, recarregar esta Task depois de editar
o arquivo dela), use o comando `odctl`. Rode `odctl ajuda` para ver as opções.

Ao terminar, responda com um resumo curto do que fez e de como o critério de aceite foi atendido.
```

## Variáveis de ambiente

Todo processo de agente recebe `OD_RPC_URL`, `OD_RPC_TOKEN`, `OD_TASK_ID` e `OD_SUBTAREFA_ID`, para poder chamar a app pelo [RPC](rpc.md). O token vale só para aquela execução e tem o papel `AGENTE`.

## Adaptadores

### Claude Code (`CLAUDE_CODE`) — confirmado na versão 2.1.308 (D-42)

| Necessidade | Opção |
|---|---|
| Não interativo | `claude -p`, prompt pelo stdin |
| Log ao vivo + resultado | `--output-format stream-json` (o último evento traz resultado, `session_id` e consumo) |
| Sessão da Task | primeira subtarefa: `--session-id <uuid gerado pela app>`; seguintes: `-r <uuid>` |
| Permissões | `--permission-mode` (`acceptEdits`, `dontAsk`...) e `--allowedTools` / `--disallowedTools` |
| Modelo e esforço | `--model`, `--effort` |
| Ferramentas da fila (MCP) | `--mcp-config <arquivo gerado por execução>` (decisão D-39) |

Exemplo:

```
claude -p --output-format stream-json --session-id 6f1c… --model sonnet --effort medium
       --permission-mode acceptEdits --allowedTools "Bash(mvn test:*)" Edit
       --mcp-config C:\Users\…\.overtime-dispatcher\run\E-0031-mcp.json
```

> Codex e Gemini não estão instalados nesta máquina; as opções deles serão confirmadas na fase 4 (A-08, A-09).

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

- Task com repositório → a worktree da Task (`worktrees/<projeto>-<id>/`, branch `overtime/<id>`), nunca a pasta original do repositório.
- Task sem repositório → `workspace/<taskId>/`, criada na primeira execução.

## Logs

Cada execução grava a saída completa em `logs/<taskId>/<execucaoId>.log`, além de mandar linha a linha para a tela pelo `aoReceberLinha`.

## Segurança

- Nunca liberar tudo por padrão: as permissões vêm do repositório/Task e são explícitas.
- O prompt vai por stdin, nunca concatenado num comando de shell.
- Timeout sempre definido; processo morto ao estourar.
