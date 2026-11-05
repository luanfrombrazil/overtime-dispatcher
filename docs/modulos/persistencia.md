# Módulo `persistence`

Guarda os dados da app em arquivos **JSON** (com Jackson), numa **pasta central** da app.

- Uma pasta por Task, com um JSON **enxuto**: só o que o agendador e o prompt precisam.
- Os arquivos dos projetos ficam na **pasta do sistema** de cada projeto; a app não os move.
- As tarefas das Trilhas ficam no **vault do Obsidian**.
- Se um agente editar o arquivo de uma Task, ele avisa a app pelo [RPC](rpc.md) para a fila ser atualizada.

## Pasta central

`%APPDATA%\Overtimer` no Windows (`~/.overtimer` se a variável não existir). Na primeira execução com o nome novo, os dados da pasta do nome antigo (`OvertimeDispatcher` / `.overtime-dispatcher`) são movidos para cá e os worktrees das Tasks são reparados. A propriedade `-Dovertimer.dados=<pasta>` troca a pasta, para testes ou para separar os dados de desenvolvimento.

## Projetos

```
projetos/<id do projeto>/projeto.json                 dados do projeto e a ordem das Tasks
projetos/<id do projeto>/tasks/<id da task>/task.json  a Task com as suas subtarefas
projetos/<id do projeto>/tasks/<id da task>/log.txt    o que o agente fez em cada execução
worktrees/<id da task>/                                 worktree git da Task (projetos com git)
```

- Os ids são UUIDs e não aparecem na tela.
- `projeto.json` guarda nome, sigla, descrição, pasta, branch (ausente quando o projeto não usa git), provedor e esforço padrão, permissões e a lista ordenada dos ids das Tasks.
- `task.json` guarda título, descrição, provedor, esforço, origem, a sessão do agente, o estado e a resposta da Task executada inteira (`execucao` e `resultado`, usados só sem subtarefas) e as subtarefas (instrução, critério de aceite, verificação manual, commit e push, esforço e modelo, estado, resposta da última execução e os ids das subtarefas das quais depende).
- O estado do projeto não é salvo, nem o da Task que tem subtarefas: vem das subtarefas.
- Cada arquivo é escrito num `.tmp` e depois trocado, para não ficar pela metade.
- Excluir um projeto apaga só a pasta dele na pasta central; a pasta do projeto no sistema não é tocada.

## Protocolos

```
protocolos-rotina/<id do protocolo>/protocolo.json    o protocolo inteiro
protocolos-rotina/<id do protocolo>/historico.json    as últimas 20 execuções
protocolos-rotina/<id do protocolo>/trabalho/         pasta onde o agente do protocolo roda
```

- `historico.json` guarda `{ "execucoes": [...] }`, da mais recente para a mais antiga; cada execução tem `inicio` e `fim` (ISO, ex.: `2026-11-05T08:00:12`), `sucesso`, `resultado` (resposta do agente ou motivo da falha) e `log` (até 500 linhas). Ao abrir a app, a agenda conta a partir do início da última execução: o horário que passou com a app fechada roda uma vez.

- A pasta `trabalho` é criada na primeira execução e mantida entre uma execução e outra; o agente pode deixar nela o que precisar consultar na próxima.

- `protocolo.json` guarda nome, sigla, passo a passo, a agenda, provedor e esforço, os acessos, os ids de quem entra direto (`aceitaDiretoDe`), se está ligado (`ativo`), a fila de entrada e as propostas.
- A agenda é `{ "modo": "HORARIOS", "horarios": [480, 1110], "intervaloMinutos": 0 }` (minutos depois da meia-noite, todo dia) ou `{ "modo": "INTERVALO", "horarios": [], "intervaloMinutos": 45 }`.
- Os acessos são `{ "sites": ["https://..."], "email": true, "calendario": false, "conectorEmail": "claude_ai_Gmail", "conectorCalendario": null }`: o conector é o nome do servidor MCP no CLI.
- Cada item da fila ou das propostas guarda id, título, descrição e o id do protocolo ou projeto de origem.
- O estado do protocolo não é salvo: vem das propostas.
- Como nos projetos, o arquivo é escrito num `.tmp` e depois trocado, e excluir apaga a pasta do protocolo.

O formato dos demais dados (consumo) ainda está em [ideias em aberto](../ideias-em-aberto.md).

## Configurações

Ficam em `configuracoes.json`, na pasta central, e são gravadas ao clicar em "Salvar" nas Configurações. Sem o arquivo, ou com ele inválido, valem os padrões. Uma seção ausente (ex.: arquivo de uma versão anterior) fica com o padrão dela, sem afetar as outras.

```json
{
  "horarioFuncionamento": {
    "seg": ["00:00-07:00", "22:00-24:00"],
    "sab": ["00:00-24:00"]
  },
  "limitesDeUso": {
    "CLAUDE_CODE": { "automacao": 90, "oraculo": 5 },
    "CODEX": { "automacao": 90, "oraculo": 5 },
    "GEMINI": { "automacao": 90, "oraculo": 5 }
  },
  "execucao": { "intervaloMinutos": 5, "tentativas": 3, "timeoutMinutos": 30, "maximoPorIntervalo": 0 },
  "notificacoes": {
    "silenciarAgora": false,
    "silenciarAutomaticamente": true,
    "silencioInicio": "23:00",
    "silencioFim": "07:00",
    "falhasFuramSilencio": true,
    "canais": { "desktop": true, "whatsapp": false, "som": false },
    "avisos": ["VERIFICACAO", "DIRECIONAMENTO", "MERGE", "FALHAS"]
  },
  "prompts": {
    "modelos": [
      { "nome": "Padrão · subtarefas", "texto": "Você é um agente ... {regras.resposta}", "usadoEm": "SUBTAREFAS" },
      { "nome": "Padrão · task inteira", "texto": "Você é um agente ... {regras.resposta}", "usadoEm": "TASK_INTEIRA" }
    ],
    "variaveis": [
      { "nome": "regras.git", "descricao": "", "regra": "- Faça commit na branch `{branch}`." },
      { "nome": "estilo", "descricao": "Estilo do código", "regra": "- Siga o estilo do projeto." }
    ]
  }
}
```

- O horário de funcionamento é gravado como faixas de 30 em 30 minutos por dia.
- O limite de uso é a porcentagem da janela de uso de cada provedor até onde a automação pode começar Tasks, mais a faixa do Oráculo; o resto é sua. A seção `reservas` de versões anteriores é ignorada.
- Os prompts são os textos que o agente recebe. O agendador usa o primeiro modelo de `usadoEm` igual a `SUBTAREFAS` ou a `TASK_INTEIRA` (sem nenhum, o padrão da app) e troca cada `{variavel}` pelo texto dela. Some a linha que só tinha variáveis vazias e o parágrafo cujas variáveis estão todas vazias (ex.: "Critério de aceite:" sem critério). `{...}` que não é variável fica como está.
- `variaveis` guarda só as regras editadas e as criadas. Sem entrada, `regras.permissoes`, `regras.git` e `regras.resposta` são geradas das permissões do projeto, do commit e do push da subtarefa e do critério de aceite; `regras.git` fica vazia em projeto sem git. Uma regra pode usar as outras variáveis; as criadas também podem usar as `regras.*`. A retomada depois de um direcionamento repete `regras.resposta`.

## Notificações

Ficam em `notificacoes.json`, na pasta central, na ordem em que a app as guarda. Sem o arquivo, ou com ele inválido, a app começa sem notificações.

```json
{
  "notificacoes": [
    {
      "id": "3f2a...",
      "tipo": "FALHAS",
      "titulo": "Task falhou",
      "texto": "Timeout depois de 3 tentativas.",
      "projetoId": "9c1e...",
      "taskId": "b7d0...",
      "instante": "2026-11-05T14:30:12",
      "lida": false
    }
  ]
}
```

- `tipo` é um dos avisos das configurações (`VERIFICACAO`, `DIRECIONAMENTO`, `MERGE`, `FALHAS`, `LIMITE_DE_USO`, `SESSAO_CONCLUIDA`).
- `projetoId` e `taskId` ficam ausentes quando o aviso não é de um projeto ou de uma Task (ex.: limite de uso).
- `instante` é a data e hora local, no formato ISO.
- Uma notificação com tipo desconhecido, sem `id` ou sem `instante` válido é ignorada, sem afetar as outras.
