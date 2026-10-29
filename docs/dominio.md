# Domínio

## Entidades

```
Projeto 1 ────► * Task 1 ────► * Subtarefa 1 ────► * Execucao
   ├── 1 pasta do sistema (obrigatória)
   └── 0..1 Git (se a pasta usa git)

Protocolo 1 ────► * ResultadoProtocolo (uma por execução)
```

### Projeto

Agrupa Tasks com o mesmo contexto. É o que aparece na barra da esquerda da app (decisão D-26).

| Campo | Tipo | Observação |
|---|---|---|
| `id` | `String` | Interno (slug, ex.: `meu-app`); não aparece na interface |
| `nome` | `String` | Ex.: "Meu App" |
| `descricao` | `String` | Contexto geral; entra no prompt de todas as Tasks do projeto |
| `pasta` | `Path` | **Obrigatória**: a pasta do sistema onde o projeto vive (ex.: `C:\code\meu-app`, `D:\Faculdade\TCC`). É onde o agente trabalha (decisão D-50) |
| `git` | `Optional<Git>` | Preenchido quando a pasta usa git; liga as branches `overtime/…`, worktrees e merge com revisão |
| `permissoesPadrao` | `Permissoes` | Herdadas pelas Tasks |
| `provedorPadrao`, `esforcoPadrao` | | Valores iniciais das Tasks novas |
| `aceitaDiretoDe` | `List<String>` | Protocolos que podem criar Tasks aqui sem proposta; vazio = tudo chega como proposta |

### Protocolo

Rotina contínua que o agente segue de tempos em tempos (decisões D-44 a D-47; detalhes em [protocolos](modulos/protocolos.md)).

| Campo | Tipo | Observação |
|---|---|---|
| `id` | `String` | Interno (slug); não aparece na interface |
| `nome` | `String` | Ex.: "Tarefas da faculdade" |
| `passos` | `String` | O passo a passo, em texto livre |
| `quando` | `Gatilho` | Horários fixos ou intervalo |
| `acessos` | `List<Acesso>` | `SITE(url)`, `EMAIL(conector)`, `CALENDARIO(conector, agendaDoAgente)` |
| `provedor`, `modelo`, `esforco` | | Como nas Tasks |
| `avisar` | `PoliticaAviso` | `SO_NOVIDADES`, `SEMPRE`, `NUNCA` |
| `ativo` | `boolean` | Desativado não roda |
| `filaDeEntrada` | `List<Task>` | Tasks injetadas por outros agentes, processadas na próxima execução |
| `aceitaDiretoDe` | `List<String>` | Origens confiáveis; as demais chegam como proposta |
| `sessionId` | `String` | Opcional: manter memória entre execuções |

Cada execução gera um `ResultadoProtocolo`: o que foi encontrado, o que é novo desde a anterior, as ações feitas e o consumo.

### Git

Valor dentro de um Projeto, presente só quando a pasta do projeto é um repositório git ("Usar git" no cadastro).

| Campo | Tipo | Observação |
|---|---|---|
| `branchPadrao` | `String` | Ex.: `main`; base das branches `overtime/…` e destino dos merges |

**Projeto sem git:** o agente trabalha **direto na pasta do projeto**, sem cópia separada nem revisão antes de entrar. As permissões do projeto valem do mesmo jeito. Para tarefas que alteram arquivos importantes, use git.

### Task

Unidade de trabalho com objetivo claro — equivalente a uma história de usuário.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | `String` | Interno e sequencial (ex.: `T-12`); **não aparece na interface** (decisão D-28). A ordem dos IDs desempata a fila (mais antiga primeiro) |
| `projeto` | `Projeto` | A qual projeto pertence; o repositório vem dele |
| `titulo` | `String` | É o que identifica a Task para você |
| `descricao` | `String` | Contexto e objetivo; entra em todo prompt da Task |
| `prioridade` | `Prioridade` | `BAIXA`, `MEDIA`, `ALTA`, `URGENTE` |
| `esforco` | `Esforco` | Nível de raciocínio do agente para esta Task (ver abaixo) |
| `provedor` | `ProvedorId` ou `AUTOMATICO` | Começa como o provedor escolhido ou `AUTOMATICO`; na primeira execução é **substituído** pelo provedor usado e fica fixo (ver regra 6) |
| `modelo` | `String` | Opcional; modelo do provedor (ex.: `sonnet`). Vazio = padrão do CLI. Na tela aparece combinado: "Claude Code · Sonnet" (decisão D-40) |
| `sessionId` | `String` | Sessão do agente compartilhada pelas subtarefas; vazio até a primeira execução |
| `pausada` | `boolean` | Você pausou manualmente |
| `branch` | `String` | Só em projeto com git: `overtime/<id>`, criada na primeira execução |
| `commitBase` | `String` | Só em projeto com git: commit da branch padrão de onde a branch partiu; base do diff de revisão |
| `mergeada` | `boolean` | Só em projeto com git: você aprovou e o merge foi feito |
| `subtarefas` | `List<Subtarefa>` | A ordem da lista é a ordem de execução |
| `origem` | `Origem` | `MANUAL` (você), `INJETADA(origem, execução)` — de qual protocolo ou Task veio (decisão D-48) — ou `ORACULO` (decisão D-54) |
| `chave` | `String` | Opcional; identifica a task injetada para evitar duplicatas (ex.: `entrega:fisica:relatorio-3`) |
| `proposta` | `boolean` | Injetada por origem não confiável: espera você aceitar, editar ou descartar (decisão D-49) |

O **status da Task não é guardado**: é derivado das subtarefas e de `pausada` (ver abaixo). As permissões vêm do projeto.

### Subtarefa

Passo pequeno e verificável. É o que o Claude executa de cada vez.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | `String` | ID curto (ex.: `T-12.3`) — usado nos comandos do WhatsApp |
| `titulo` | `String` | Aparece na tela e nas notificações |
| `instrucao` | `String` | O que o agente deve fazer |
| `criterioDeAceite` | `String` | Como saber que terminou |
| `requerVerificacao` | `boolean` | Se precisa da sua aprovação |
| `dependeDe` | `List<String>` | Subtarefas da mesma Task que precisam estar concluídas antes desta (decisão D-34). Vazio = pode rodar a qualquer momento |
| `status` | `StatusSubtarefa` | |
| `tentativas` | `int` | O máximo vem da configuração |
| `feedback` | `List<String>` | Comentários de rejeição e respostas de direcionamento; entram no próximo prompt |
| `pergunta` | `String` | Só em `AGUARDANDO_DIRECIONAMENTO`: o que o agente perguntou |

O histórico de execuções não fica na subtarefa: cada `Execucao` é um arquivo próprio na pasta da Task (ver [persistência](modulos/persistencia.md)).

### Execucao

Registro de uma chamada a um agente.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | `String` | |
| `subtarefaId` | `String` | A qual subtarefa pertence |
| `provedor` | `ProvedorId` | Qual agente executou |
| `esforco` | `Esforco` | Nível usado nesta execução |
| `inicio`, `fim` | `Instant` | |
| `codigoSaida` | `int` | |
| `resultado` | `String` | Texto final do Claude |
| `caminhoLog` | `Path` | Saída completa |
| `consumo` | `Consumo` | Tokens de entrada/saída e custo estimado |
| `origem` | `OrigemExecucao` | `AUTOMATICA` ou `MANUAL` |

### Permissoes

O que o agente pode fazer sem supervisão, num formato **neutro** (ex.: "pode editar arquivos", "pode rodar `npm test`", "pode acessar a rede"). Cada adaptador traduz isso para as opções do seu CLI. Ver [módulo agente](modulos/agente.md).

### Esforco

Quanto o agente deve "pensar" antes de agir. Níveis mais altos tendem a dar respostas melhores em tarefas difíceis, mas demoram mais e **consomem mais créditos**.

| Valor | Uso típico |
|---|---|
| `PADRAO` | Não envia nada; o agente usa o próprio padrão |
| `BAIXO` | Tarefas mecânicas: renomear, formatar, ajustes pequenos |
| `MEDIO` | A maioria das subtarefas |
| `ALTO` | Bugs difíceis, decisões de design, refatorações amplas |
| `MUITO_ALTO` | Problemas complexos, quando `ALTO` não bastou |
| `MAXIMO` | Casos excepcionais; usar com cuidado por causa do consumo |

O valor é **neutro**: cada adaptador traduz para a opção do seu agente (ver [módulo agente](modulos/agente.md)). Se um agente não tiver aquele nível, o adaptador usa o mais próximo.

Uma Task nova recebe o esforço padrão da configuração, e você pode mudar por Task.

### ProvedorId

Identificador de um agente configurado: `CLAUDE_CODE`, `CODEX`, `GEMINI` (e outros no futuro). É só um dado; o comportamento fica no módulo `agente`.

## Status da Subtarefa

```
            ┌──────────── rejeitada (feedback) ────────────┐
            ▼                                              │
        A_FAZER ──► EM_ANDAMENTO ──► sucesso ──┬─ não requer verificação ──► CONCLUIDA
          ▲  ▲           │  │                  │
          │  │           │  │ erro             └─ requer ──► AGUARDANDO_VERIFICACAO ──► aprovada ──► CONCLUIDA
          │  │           │  ▼
          │  └─ você responde ── AGUARDANDO_DIRECIONAMENTO ◄── agente pede direcionamento
          │              │
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
| `AGUARDANDO_DIRECIONAMENTO` | O agente parou no meio e fez uma pergunta; espera sua resposta (não conta tentativa) |
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
| `AGUARDANDO_VOCE` | Próxima subtarefa está `AGUARDANDO_VERIFICACAO`, `AGUARDANDO_DIRECIONAMENTO` ou `FALHOU` |
| `PAUSADA` | Você pausou manualmente |
| `PRONTA_PARA_MERGE` | Task de projeto com git, todas as subtarefas finalizadas, esperando sua revisão do diff completo |
| `CONCLUIDA` | Todas as subtarefas `CONCLUIDA` ou `CANCELADA` e, se tiver repositório, `mergeada = true` |

## Estado visual

A interface não mostra os status com badges. Cada status vira um de cinco **estados visuais**, mostrados como uma borda colorida à esquerda do item, com uma mensagem curta do motivo (decisão D-27; detalhes em [interfaces](interfaces.md#estado-visual-borda-colorida)). Texto só em Atenção e Erro, o mais curto possível (ex.: `1 verificação`, `Falhou`):

| Estado visual | Status |
|---|---|
| `Concluido` (verde) | `CONCLUIDA`; Task mergeada |
| `Em andamento` (azul) | `EM_ANDAMENTO` |
| `Atencao` (amarelo) | `AGUARDANDO_VERIFICACAO`, `AGUARDANDO_DIRECIONAMENTO`, `PRONTA_PARA_MERGE`, `PAUSADA` |
| `Erro` (vermelho) | `FALHOU`; provedor não autenticado; conflito de merge sem solução |
| `Pendente` (cinza) | `A_FAZER`, `RASCUNHO`, `PRONTA` |

`CANCELADA` não aparece por padrão (filtro). O estado de uma Task e de um Projeto é o mais grave entre os seus itens: `Erro` > `Atencao` > `Em andamento` > `Pendente` > `Concluido`.

## Regras de negócio

1. **Dependências:** uma subtarefa só começa quando todas as de `dependeDe` estão `CONCLUIDA` ou `CANCELADA`. Dentro de uma Task roda **uma subtarefa por vez** (mesma sessão e worktree). Entre as elegíveis, vale a ordem da lista. Dependências circulares são recusadas ao salvar.
2. Uma Task que depende de você (`AGUARDANDO_VOCE`, `PRONTA_PARA_MERGE`, `PAUSADA`) **não bloqueia as outras**: o agente fica livre na hora e o agendador dispara um novo ciclo imediatamente, pegando a próxima Task em aberto. Quando você responde, a Task volta para a fila com a prioridade normal.
3. **Branch e worktree por Task:** toda Task de projeto com git trabalha na branch `overtime/<id>` (ex.: `overtime/T-12`), criada a partir da branch padrão, numa worktree própria em `worktrees/<projeto>-<id>/`. A sua cópia do repositório nunca é alterada pelo agente. Por isso, Tasks diferentes do mesmo repositório podem rodar em paralelo (limitado pela configuração de execuções simultâneas).
4. Rejeição devolve a subtarefa para `A_FAZER` sem contar tentativa e anexa o comentário ao `feedback`.
5. Subtarefa `EM_ANDAMENTO` não pode ser editada nem removida.
6. Uma Task fica no mesmo provedor do início ao fim (o `provedor` gravado na primeira execução), porque a sessão de um agente não é aproveitável em outro. Trocar de provedor no meio é uma ação manual, e a próxima subtarefa começa uma sessão nova com um resumo do que já foi feito.
7. **Merge só com você:** quando todas as subtarefas terminam, a Task vai para `PRONTA_PARA_MERGE`. Você revisa o diff completo (`commitBase..overtime/<id>`) e:
   - **aprova:** a app faz merge com squash na branch padrão, **sem push automático**, e apaga a worktree e a branch;
   - **rejeita com comentário:** o comentário vira uma nova subtarefa e a Task volta para a fila.

   Se a branch padrão mudou e houver conflito, a resolução vira uma subtarefa com verificação. O papel `AGENTE` nunca faz merge nem push.
8. **Direcionamento:** se o agente precisar de uma decisão sua no meio do trabalho, ele pede pelo RPC (`direcionamento.pedir`). A subtarefa vai para `AGUARDANDO_DIRECIONAMENTO` sem contar tentativa; sua resposta entra no prompt e a execução continua na mesma sessão.
