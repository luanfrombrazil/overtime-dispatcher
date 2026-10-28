# Domínio

## Entidades

```
Repositorio 0..1 ◄──── Task 1 ────► * Subtarefa 1 ────► * Execucao
```

### Repositorio

Pasta local de código à qual uma Task pode ser vinculada.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | `String` | |
| `nome` | `String` | Nome amigável |
| `caminho` | `Path` | Pasta local; precisa existir |
| `branchPadrao` | `String` | Opcional (ex.: `main`) |
| `permissoesPadrao` | `Permissoes` | Ferramentas liberadas por padrão para Tasks deste repositório |

### Task

Unidade de trabalho com objetivo claro — equivalente a uma história de usuário.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | `String` | ID curto e legível (ex.: `T-12`) |
| `titulo` | `String` | |
| `descricao` | `String` | Contexto e objetivo; entra em todo prompt da Task |
| `repositorio` | `Optional<Repositorio>` | Sem repositório → usa `workspace/<id>/` |
| `prioridade` | `Prioridade` | `BAIXA`, `MEDIA`, `ALTA`, `URGENTE` |
| `status` | `StatusTask` | Derivado das subtarefas (ver abaixo) |
| `provedorPreferido` | `PreferenciaProvedor` | Um provedor específico (`CLAUDE_CODE`, `CODEX`, `GEMINI`...) ou `AUTOMATICO` (o agendador escolhe pelo orçamento disponível) |
| `provedorFixado` | `ProvedorId` | Definido na primeira execução; mantém as subtarefas seguintes no mesmo agente (ver regra 6) |
| `sessionId` | `String` | Sessão do agente compartilhada pelas subtarefas (se a decisão for compartilhar); só vale para o `provedorFixado` |
| `permissoes` | `Permissoes` | Sobrescreve as do repositório, se definido |
| `subtarefas` | `List<Subtarefa>` | Ordenadas |
| `criadaEm`, `atualizadaEm` | `Instant` | |

### Subtarefa

Passo pequeno e verificável. É o que o Claude executa de cada vez.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | `String` | ID curto (ex.: `T-12.3`) — usado nos comandos do WhatsApp |
| `ordem` | `int` | Posição na Task |
| `titulo` | `String` | |
| `instrucao` | `String` | O que o Claude deve fazer |
| `criterioDeAceite` | `String` | Como saber que terminou |
| `requerVerificacao` | `boolean` | Se precisa da sua aprovação |
| `status` | `StatusSubtarefa` | |
| `tentativas` | `int` | |
| `maxTentativas` | `int` | Padrão vem da configuração |
| `feedback` | `List<String>` | Comentários de rejeição; entram no próximo prompt |
| `execucoes` | `List<Execucao>` | Histórico |

### Execucao

Registro de uma chamada a um agente.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | `String` | |
| `provedor` | `ProvedorId` | Qual agente executou |
| `inicio`, `fim` | `Instant` | |
| `codigoSaida` | `int` | |
| `resultado` | `String` | Texto final do Claude |
| `caminhoLog` | `Path` | Saída completa |
| `consumo` | `Consumo` | Tokens de entrada/saída e custo estimado |
| `origem` | `OrigemExecucao` | `AUTOMATICA` ou `MANUAL` |

### Permissoes

O que o agente pode fazer sem supervisão, num formato **neutro** (ex.: "pode editar arquivos", "pode rodar `npm test`", "pode acessar a rede"). Cada adaptador traduz isso para as opções do seu CLI. Ver [módulo agente](modulos/agente.md).

### ProvedorId

Identificador de um agente configurado: `CLAUDE_CODE`, `CODEX`, `GEMINI` (e outros no futuro). É só um dado; o comportamento fica no módulo `agente`.

## Status da Subtarefa

```
            ┌──────────── rejeitada (feedback) ────────────┐
            ▼                                              │
        A_FAZER ──► EM_ANDAMENTO ──► sucesso ──┬─ não requer verificação ──► CONCLUIDA
            ▲            │                     │
            │            │ erro                └─ requer ──► AGUARDANDO_VERIFICACAO ──► aprovada ──► CONCLUIDA
            │            ▼
            └── tentativas < max ── FALHOU_TEMP
                         │
                tentativas = max
                         ▼
                      FALHOU  ──► (você pode reabrir → A_FAZER)

  Qualquer status não final ──► CANCELADA
```

| Status | Significado |
|---|---|
| `A_FAZER` | Pronta para ser pega |
| `EM_ANDAMENTO` | Execução em curso |
| `AGUARDANDO_VERIFICACAO` | Terminou, esperando sua aprovação |
| `CONCLUIDA` | Final |
| `FALHOU` | Esgotou tentativas; precisa de intervenção |
| `CANCELADA` | Final; ignorada pelo agendador |

> `FALHOU_TEMP` não é um status persistido: é só o caminho em que a falha conta tentativa e a subtarefa volta para `A_FAZER`.

As transições válidas ficam no próprio `domain` (ex.: `subtarefa.iniciar()`, `subtarefa.aprovar()`), lançando exceção se a transição não for permitida.

## Status da Task (derivado)

| Status | Regra |
|---|---|
| `RASCUNHO` | Ainda sem subtarefas, ou marcada por você como não pronta |
| `PRONTA` | Tem subtarefas `A_FAZER` e nenhuma bloqueando |
| `EM_ANDAMENTO` | Alguma subtarefa `EM_ANDAMENTO` |
| `AGUARDANDO_VOCE` | Próxima subtarefa está `AGUARDANDO_VERIFICACAO` ou `FALHOU` |
| `PAUSADA` | Você pausou manualmente |
| `CONCLUIDA` | Todas as subtarefas `CONCLUIDA` ou `CANCELADA` |

## Regras de negócio

1. Subtarefas rodam **em ordem**: a subtarefa N só começa quando a N-1 está `CONCLUIDA` ou `CANCELADA`.
2. Uma Task `AGUARDANDO_VOCE` não bloqueia as outras: o agendador passa para a próxima Task.
3. Duas Tasks no **mesmo repositório** não rodam ao mesmo tempo.
4. Rejeição devolve a subtarefa para `A_FAZER` sem contar tentativa e anexa o comentário ao `feedback`.
5. Subtarefa `EM_ANDAMENTO` não pode ser editada nem removida.
6. Uma Task fica no mesmo provedor do início ao fim (`provedorFixado`), porque a sessão de um agente não é aproveitável em outro. Trocar de provedor no meio é uma ação manual, e a próxima subtarefa começa uma sessão nova com um resumo do que já foi feito.
