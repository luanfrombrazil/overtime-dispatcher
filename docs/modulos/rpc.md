# Módulo `rpc`

Permite que **programas externos**, principalmente os próprios agentes durante uma execução, chamem funções da app. Exemplos:

- o agente editou o arquivo de uma Task e pede para a app recarregá-la;
- o agente percebeu que a subtarefa é grande demais e adiciona novas subtarefas;
- o agente consulta qual é a sua subtarefa atual e o que já foi feito.

Sem RPC, a app não sabe que alguém mexeu nos arquivos, e uma alteração externa seria sobrescrita (ver [persistência](persistencia.md#regras-de-gravação)).

## Arquitetura

```
 agente (Claude, Codex, Gemini)
        │ chama
        ▼
 ┌──────────────────────┐       ┌──────────────────────┐
 │ odctl (linha de cmd) │  ou   │ servidor MCP (futuro) │
 └──────────┬───────────┘       └──────────┬───────────┘
            │      JSON-RPC 2.0 sobre HTTP local
            ▼
 ┌──────────────────────────────────────────┐
 │ rpc: ServidorRpc → Despachante → métodos │
 └────────────────────┬─────────────────────┘
                      ▼
                   service          (mesmas regras e validações da UI)
```

- **Transporte:** HTTP em `127.0.0.1` (nunca exposto na rede), porta configurável, com mensagens no formato **JSON-RPC 2.0**. O próprio JDK tem um servidor HTTP simples (`com.sun.net.httpserver`), então não precisa de framework.
- **O RPC é só mais uma porta de entrada do `service`**, como a UI e o WhatsApp. Nenhuma regra de negócio fica no módulo `rpc`.
- **`odctl`:** um comando pequeno de linha de comando (pode ser o próprio jar com um argumento) que transforma `odctl tarefa recarregar T-12` numa chamada RPC. É o jeito mais simples de qualquer agente usar, porque todos sabem rodar comandos no terminal.
- **MCP (desde a fase 2, decisão D-39):** o mesmo servidor local também fala MCP (transporte HTTP), sobre o mesmo despachante. A cada execução, a app gera um arquivo de configuração MCP com a URL e o token daquela execução e o passa ao agente (`--mcp-config` no Claude Code). Assim os métodos aparecem como **ferramentas nativas** (ex.: `pedir_direcionamento`, `adicionar_subtarefa`). O `odctl` continua existindo para uso no terminal e para agentes sem MCP.

## Métodos

| Método | Parâmetros | O que faz |
|---|---|---|
| `tarefa.recarregar` | `taskId` | Relê o `task.json` do disco, valida e atualiza a fila e a tela |
| `tarefa.buscar` | `taskId` | Devolve a Task com suas subtarefas |
| `tarefa.listar` | filtros opcionais | Lista Tasks (id, título, status, prioridade) |
| `tarefa.criar` | título, descrição, prioridade, esforço | Só `DONO`: cria uma Task num projeto |
| `task.injetar` | destino (protocolo ou projeto), título, descrição, dados, chave | Cria trabalho para outro destino: **task direta** se o destino confia na origem, senão **proposta** (ver [protocolos](protocolos.md#injeção-de-tasks)). Com a mesma chave, atualiza a existente. Exposto no MCP como `injetar_task` |
| `proposta.aceitar` / `proposta.descartar` | `propostaId`, edições | Só `DONO`: transforma a proposta em task ou a descarta |
| `subtarefa.adicionar` | `taskId`, título, instrução, critério, posição | Adiciona subtarefa |
| `subtarefa.atualizar` | `subtarefaId`, campos | Edita uma subtarefa que ainda não começou |
| `subtarefa.atual` | — | A subtarefa que está sendo executada por quem chama |
| `execucao.anotar` | texto | Acrescenta uma nota ao log da execução atual |
| `execucao.solicitarVerificacao` | motivo | Pede que a subtarefa atual passe por verificação, mesmo que não estivesse marcada |
| `direcionamento.pedir` | pergunta | O agente precisa de uma decisão sua: a subtarefa vai para `AGUARDANDO_DIRECIONAMENTO`, sem contar tentativa, e a pergunta chega a você pelos canais |
| `direcionamento.responder` | `subtarefaId`, resposta | Só `DONO`: a resposta entra no prompt e a subtarefa volta para `A_FAZER` |
| `task.aprovarMerge` / `task.rejeitarMerge` | `taskId`, comentário | Só `DONO`: merge com squash na branch padrão, ou nova subtarefa com o comentário |
| `agendador.status` | — | Estado do agendador, execuções em andamento, orçamento por provedor |

## Contexto da execução

Ao iniciar um agente, a app passa variáveis de ambiente para o processo:

| Variável | Conteúdo |
|---|---|
| `OD_RPC_URL` | Endereço do servidor (ex.: `http://127.0.0.1:47321/rpc`) |
| `OD_RPC_TOKEN` | Token **desta execução** |
| `OD_TASK_ID` / `OD_SUBTAREFA_ID` | O que está sendo executado |

O prompt de cada subtarefa inclui uma instrução curta de como usar o `odctl` (ver [agente](agente.md#prompt-modelo)).

## Segurança

O agente roda sem supervisão, então ele **não pode ter os mesmos poderes que você**.

1. **Só localhost.** O servidor nunca escuta em outra interface.
2. **Token obrigatório.** Toda chamada leva um token no cabeçalho `Authorization`.
3. **Dois papéis:**

   | Papel | Token | Pode |
   |---|---|---|
   | `AGENTE` | gerado por execução, expira quando ela termina | ler Tasks; mexer **só na própria Task** (recarregar, adicionar e editar subtarefas pendentes); injetar tasks em outros protocolos e projetos (direto ou como proposta, conforme o destino); anotar; pedir verificação; pedir direcionamento |
   | `DONO` | fixo, guardado em `config.json` | tudo o que a UI faz |

4. **O que um agente nunca pode fazer:** aprovar ou rejeitar verificações (nem as próprias), responder direcionamentos, fazer merge ou push, concluir subtarefas diretamente, mudar prioridade, provedor, esforço, orçamento ou permissões, e iniciar ou pausar o agendador.
5. **Validação igual à da UI.** Um `tarefa.recarregar` com um arquivo inválido é recusado, e a versão em memória é mantida.
6. **Registro.** Toda chamada fica no log da execução (método, parâmetros e resultado).

## Exemplo

Requisição:

```json
{ "jsonrpc": "2.0", "id": 1, "method": "subtarefa.adicionar",
  "params": { "taskId": "T-12", "titulo": "Atualizar a documentação do login",
              "instrucao": "Descreva a normalização do e-mail no README.",
              "criterioDeAceite": "O README menciona a regra.", "posicao": 3 } }
```

Resposta:

```json
{ "jsonrpc": "2.0", "id": 1, "result": { "subtarefaId": "T-12.3" } }
```

Pelo `odctl`:

```
odctl subtarefa adicionar T-12 --titulo "Atualizar a documentação do login" --instrucao "..." --criterio "..."
```

## Erros

| Código | Quando |
|---|---|
| `-32601` | Método não existe |
| `-32602` | Parâmetros inválidos |
| `1001` | Não autorizado (sem token ou token expirado) |
| `1002` | Proibido para o papel (ex.: agente tentando aprovar) |
| `1003` | Task ou subtarefa não encontrada |
| `1004` | Transição inválida (ex.: editar subtarefa em andamento) |
| `1005` | Arquivo inválido ao recarregar |
