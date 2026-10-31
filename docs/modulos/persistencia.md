# Módulo `persistence`

Guarda os dados da app em arquivos **JSON** (com Jackson), numa **pasta central** da app.

- Uma pasta por Task, com um JSON **enxuto**: só o que o agendador e o prompt precisam.
- Os arquivos dos projetos ficam na **pasta do sistema** de cada projeto; a app não os move.
- As tarefas das Trilhas ficam no **vault do Obsidian**.
- Se um agente editar o arquivo de uma Task, ele avisa a app pelo [RPC](rpc.md) para a fila ser atualizada.

## Pasta central

`%APPDATA%\OvertimeDispatcher` no Windows (`~/.overtime-dispatcher` se a variável não existir). A propriedade `-Dovertime.dados=<pasta>` troca a pasta, para testes ou para separar os dados de desenvolvimento.

## Projetos

```
projetos/<id do projeto>/projeto.json                 dados do projeto e a ordem das Tasks
projetos/<id do projeto>/tasks/<id da task>/task.json  a Task com as suas subtarefas
projetos/<id do projeto>/tasks/<id da task>/log.txt    o que o agente fez em cada execução
worktrees/<id da task>/                                 worktree git da Task (projetos com git)
```

- Os ids são UUIDs e não aparecem na tela.
- `projeto.json` guarda nome, sigla, descrição, pasta, branch (ausente quando o projeto não usa git), provedor e esforço padrão, permissões e a lista ordenada dos ids das Tasks.
- `task.json` guarda título, descrição, provedor, esforço, origem, a sessão do agente e as subtarefas (instrução, critério de aceite, verificação manual, estado, resposta da última execução e os ids das subtarefas das quais depende).
- O estado do projeto e da Task não é salvo: vem das subtarefas.
- Cada arquivo é escrito num `.tmp` e depois trocado, para não ficar pela metade.
- Excluir um projeto apaga só a pasta dele na pasta central; a pasta do projeto no sistema não é tocada.

O formato dos demais dados (protocolos, consumo) ainda está em [ideias em aberto](../ideias-em-aberto.md).

## Configurações

Ficam em `configuracoes.json`, na pasta central. Sem o arquivo, ou com ele inválido, valem os padrões.

O horário de funcionamento é gravado como faixas de 30 em 30 minutos por dia:

```json
{
  "horarioFuncionamento": {
    "seg": ["00:00-07:00", "22:00-24:00"],
    "sab": ["00:00-24:00"]
  }
}
```
