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

O formato dos demais dados (protocolos, consumo) ainda está em [ideias em aberto](../ideias-em-aberto.md).

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
  }
}
```

- O horário de funcionamento é gravado como faixas de 30 em 30 minutos por dia.
- O limite de uso é a porcentagem da janela de uso de cada provedor até onde a automação pode começar Tasks, mais a faixa do Oráculo; o resto é sua. A seção `reservas` de versões anteriores é ignorada.
