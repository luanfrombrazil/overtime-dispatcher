# Módulo `persistence`

Guarda os dados da app em arquivos **JSON** (com Jackson), numa **pasta central** da app.

- Uma pasta por Task, com um JSON **enxuto**: só o que o agendador e o prompt precisam.
- Os arquivos dos projetos ficam na **pasta do sistema** de cada projeto; a app não os move.
- As tarefas das Trilhas ficam no **vault do Obsidian**.
- Se um agente editar o arquivo de uma Task, ele avisa a app pelo [RPC](rpc.md) para a fila ser atualizada.

O formato exato dos arquivos está em [ideias em aberto](../ideias-em-aberto.md).

## Configurações
Ficam em `configuracoes.json`, na pasta central (`%APPDATA%\OvertimeDispatcher` no Windows; `~/.overtime-dispatcher` nos demais). Sem o arquivo, ou com ele inválido, valem os padrões.

O horário de funcionamento é gravado como faixas de 30 em 30 minutos por dia:

```json
{
  "horarioFuncionamento": {
    "seg": ["00:00-07:00", "22:00-24:00"],
    "sab": ["00:00-24:00"]
  }
}
```
