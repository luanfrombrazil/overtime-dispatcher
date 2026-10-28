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
- a Task não está `PAUSADA`, `RASCUNHO` nem `AGUARDANDO_VOCE`;
- nenhuma outra Task do **mesmo repositório** está em execução;
- o provedor da Task (fixado, preferido ou escolhido no modo automático) é o provedor em questão.

Entre as elegíveis: maior **prioridade** da Task primeiro; em empate, a Task mais antiga.

## Ao terminar uma execução

| Resultado | Ação |
|---|---|
| Sucesso, sem verificação | Subtarefa → `CONCLUIDA` |
| Sucesso, requer verificação | Subtarefa → `AGUARDANDO_VERIFICACAO`; emite `VerificacaoSolicitada` |
| `LIMITE_DE_USO` | Subtarefa volta para `A_FAZER` **sem** contar tentativa; provedor bloqueado (ver [consumo](consumo.md)) |
| `TIMEOUT` / `FALHA_DO_AGENTE` | Conta tentativa; volta para `A_FAZER` ou vai para `FALHOU` |
| `NAO_AUTENTICADO` | Provedor desabilitado até você resolver; aviso pelos canais |
| `CANCELADO` | Volta para `A_FAZER` sem contar tentativa |

Depois de tratar o resultado, dispara um novo ciclo imediatamente.

## Controles

- **Iniciar / Pausar** o agendador inteiro.
- **Pausar um provedor** específico.
- **Executar agora**: roda uma subtarefa escolhida por você, ignorando o intervalo (mas respeitando a regra do mesmo repositório). Execuções manuais são marcadas com `origem = MANUAL`.
- **Cancelar** a execução em andamento.

## Recuperação

Se a app fechar no meio de uma execução, na próxima abertura as subtarefas `EM_ANDAMENTO` voltam para `A_FAZER` contando uma tentativa.
