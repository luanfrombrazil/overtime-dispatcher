# Módulo `scheduler` (agendador)

Roda um ciclo periódico que decide se há trabalho a fazer e o despacha para um agente.

## Ciclo

A cada `intervalo` (configurável, ex.: 5 min), ou imediatamente quando uma execução termina:

```
1. Agendador pausado?                         → sim: fim do ciclo
2. Para cada provedor com vaga livre (ocioso):
   a. Buscar a próxima subtarefa elegível para esse provedor
   b. Consultar PoliticaDeConsumo              → negado: registrar motivo, próximo provedor
   c. Marcar subtarefa EM_ANDAMENTO (via service)
   d. Disparar execução na thread de execução
3. Fim do ciclo
```

## Ocioso

Um provedor está **ocioso** quando a app não tem nenhuma execução em andamento nele (ou menos que o limite de execuções paralelas configurado para ele, inicialmente 1).

## Seleção da próxima subtarefa

Uma subtarefa é elegível quando:

- está `A_FAZER`;
- é a primeira não concluída da sua Task (ordem);
- a Task não está `PAUSADA`, `RASCUNHO`, `AGUARDANDO_VOCE` nem `PRONTA_PARA_MERGE` (ela espera por você, e o agente segue para outra Task);
- o `provedor` da Task é o provedor em questão, ou é `AUTOMATICO` e a política de consumo escolhe este provedor.

Entre as elegíveis: maior **prioridade** da Task primeiro; em empate, a Task com o menor ID (a mais antiga).

## Ao terminar uma execução

| Resultado | Ação |
|---|---|
| Sucesso, sem verificação | Subtarefa → `CONCLUIDA` |
| Sucesso, requer verificação | Subtarefa → `AGUARDANDO_VERIFICACAO`; emite `VerificacaoSolicitada` |
| `LIMITE_DE_USO` | Subtarefa volta para `A_FAZER` **sem** contar tentativa; provedor bloqueado (ver [consumo](consumo.md)) |
| `TIMEOUT` / `FALHA_DO_AGENTE` | Conta tentativa; volta para `A_FAZER` ou vai para `FALHOU` |
| `NAO_AUTENTICADO` | Provedor desabilitado até você resolver; aviso pelos canais |
| `CANCELADO` | Volta para `A_FAZER` sem contar tentativa |
| Agente pediu direcionamento | Subtarefa → `AGUARDANDO_DIRECIONAMENTO` **sem** contar tentativa; emite `DirecionamentoSolicitado` |
| Última subtarefa da Task terminou (projeto com git) | Task → `PRONTA_PARA_MERGE`; emite `MergeSolicitado` |

Depois de tratar o resultado, dispara um novo ciclo imediatamente, para que o agente liberado pegue outra Task em aberto.

## Branch e worktree

Antes da primeira execução de uma Task de projeto com git, o agendador (via `service`) cria a branch `overtime/<id>` a partir da branch padrão, grava `branch` e `commitBase` na Task e cria a worktree em `worktrees/<projeto>-<id>/`. Todas as execuções da Task rodam nessa worktree. Depois do merge aprovado, a worktree e a branch são removidas.

## Tudo esperando por você

Se todas as Tasks elegíveis estiverem paradas esperando você, o agente fica ocioso. Nesse caso a app envia um resumo pelos canais (ex.: "3 Tasks paradas esperando você: …"), no máximo uma vez a cada X horas (configurável).

## Controles

- **Iniciar / Pausar** o agendador inteiro.
- **Pausar um provedor** específico.
- **Executar agora**: roda uma subtarefa escolhida por você, ignorando o intervalo. Execuções manuais são marcadas com `origem = MANUAL`.
- **Cancelar** a execução em andamento.

## Recuperação

Se a app fechar no meio de uma execução, na próxima abertura as subtarefas `EM_ANDAMENTO` voltam para `A_FAZER` contando uma tentativa.
