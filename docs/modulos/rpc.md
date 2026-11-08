# Módulo `rpc`

Funções da app que os agentes podem chamar. Hoje são oferecidas por um **servidor MCP** dentro da própria app (D-11, D-21); outros transportes (RPC, `odctl`) ainda estão em [ideias em aberto](../ideias-em-aberto.md).

## Servidor MCP

- **Onde:** `http://127.0.0.1:<porta>/mcp`, só na própria máquina. Transporte Streamable HTTP, só com respostas JSON (sem fluxo de eventos do servidor; `GET` responde 405).
- **Porta e token externo:** em `servidor-mcp.json`, na pasta central (`{"porta": 47821, "token": "..."}`). O arquivo é criado na primeira vez, com a porta padrão `47821` e um token aleatório. Para trocar a porta ou o token, edite o arquivo e reabra a app.
- **Se não subir** (ex.: porta em uso), a app segue sem ele: as execuções rodam sem o `--mcp-config` e Configurações → Avançado mostra o motivo.
- **Acesso:** toda chamada precisa de `Authorization: Bearer <token>`. Pedidos com `Origin` de fora de `localhost` são recusados (403), para uma página aberta no navegador não chegar ao servidor.

### Dois tipos de token

| Token | Quem usa | Projeto padrão | Validade |
|---|---|---|---|
| Externo | Um agente de fora da app, ex.: uma sessão sua do Claude Code | Nenhum: o agente informa o projeto | Até você trocar o arquivo |
| Por execução | O agente que o agendador roda numa subtarefa ou Task | O da execução | Só enquanto ela roda |

Para registrar no Claude Code de fora da app: Configurações → Avançado → **Copiar comando do Claude Code**, que copia

```
claude mcp add --transport http overtimer http://127.0.0.1:47821/mcp --header "Authorization: Bearer <token>"
```

Nas execuções da app, o agendador gera um token, grava um arquivo temporário de `--mcp-config` com ele, passa o arquivo ao Claude Code e libera `mcp__overtimer` no `--allowedTools`. Ao terminar a execução, o token deixa de valer e o arquivo é apagado.

### Ferramentas

| Ferramenta | O que faz |
|---|---|
| `listar_projetos` | Id, nome, descrição e pasta de cada projeto. |
| `listar_tasks` | Tasks de um projeto, com estado, se ainda é sugestão, se está finalizada e as subtarefas (id, título, estado). |
| `sugerir_task` | Cria uma Task sugerida, opcionalmente com subtarefas. |
| `sugerir_subtarefas` | Acrescenta subtarefas sugeridas ao fim de uma Task existente. |

Todas aceitam `projeto` (id ou nome, sem diferenciar maiúsculas). Sem ele, vale o projeto da execução; com o token externo, ele é obrigatório. Sugestões podem ir para **qualquer projeto**.

Campos de uma subtarefa: `titulo`, `instrucao`, `criterioDeAceite`, `requerVerificacao` (padrão `false`), `esforco` (`low` a `max`; padrão `medium`) e `dependeDe` (posições, a partir de 0, de outras subtarefas da mesma chamada). Em `sugerir_subtarefas` há também `dependeDeExistentes`, com ids de subtarefas que já estão na Task. A Task tem `titulo`, `descricao` e `esforco` (padrão: o do projeto); o provedor é o padrão do projeto.

Erros que o agente pode corrigir (projeto não encontrado, dependência inválida, Task finalizada…) voltam como resultado com `isError`, com a mensagem; ferramenta ou método desconhecido é erro JSON-RPC.

## Sugestões

- **Origem e estado:** a Task sugerida tem origem `AGENTE` e execução `SUGERIDA`; cada subtarefa sugerida tem estado `SUGERIDA`. A aparência é a de uma Task normal, com "Sugerida" (ou "N subtarefas sugeridas") sob o título e os botões **Recusar** e **Aceitar**.
- **Fila:** uma Task com qualquer coisa sugerida fica toda fora da fila. Ela só conta como aceita quando **todas** as subtarefas forem aceitas; numa Task existente, uma subtarefa sugerida segura também as que já estavam aceitas.
- **Aceitar / recusar na Task:** aceita ou recusa tudo o que ainda é sugestão nela. Recusar uma Task sugerida a remove (pede confirmação se ela tem subtarefas); numa Task existente, saem só as subtarefas sugeridas.
- **Aceitar / recusar na subtarefa:** só aquela. Aceita a última sugestão de uma Task sugerida, a Task conta como aceita. Se você recusa a última sugestão e as outras subtarefas já foram aceitas, a Task também conta como aceita. Se não sobra nenhuma subtarefa, a Task continua sugerida, para ser aceita ou recusada inteira.
- **Editar:** a Task e as subtarefas sugeridas podem ser editadas como as outras. Salvar não aceita: aceitar entra na fila com o que foi editado.
- **Limites:** a Task finalizada, em andamento, rascunho ou do Oráculo não recebe subtarefas sugeridas.
