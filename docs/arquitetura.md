# Arquitetura

## Visão geral

```
                        ┌──────────────────────────────┐
                        │            ui (Swing)        │
                        └──────────────┬───────────────┘
                                       │ só chama o serviço
                        ┌──────────────▼───────────────┐
                        │            service           │  orquestra tudo, emite eventos
                        └──┬───────┬───────┬───────┬───┘
                           │       │       │       │
              ┌────────────▼┐ ┌────▼────┐ ┌▼──────┐ ┌▼────────────┐
              │ scheduler   │ │ agente  │ │consumo│ │ comunicacao │
              └──────┬──────┘ └────┬────┘ └───┬───┘ └──────┬──────┘
                     │        ┌────┴─────────────┐         │
                     │        │ Claude Code      │         │
                     │        │ Codex            │         │
                     │        │ Gemini / Antigr. │         │
                     │        └────┬─────────────┘         │
                     │             │          │            │
              ┌──────▼─────────────▼──────────▼────────────▼──────┐
              │                    domain                         │
              └──────────────────────────▲────────────────────────┘
                                         │
                              ┌──────────┴──────────┐
                              │     persistence     │
                              └─────────────────────┘
```

## Pacotes

| Pacote | Responsabilidade | Depende de |
|---|---|---|
| `domain` | Entidades, enums e regras puras (transições de status). | nada |
| `persistence` | Salvar e carregar Tasks, Repositórios, Execuções e config. | `domain` |
| `agente` | Interface comum para agentes de código (Claude Code, Codex, Gemini...): montar o prompt, rodar o processo, interpretar a saída. Um adaptador por provedor. | `domain` |
| `consumo` | Contabilizar uso **por provedor** e decidir se a automação pode rodar. | `domain` |
| `scheduler` | Ciclo periódico: ocioso? orçamento? próxima subtarefa? qual provedor? | `domain`, `agente`, `consumo` (via interfaces) |
| `comunicacao` | Enviar avisos e receber comandos (Swing, WhatsApp). | `domain` |
| `service` | Casos de uso da aplicação; único ponto de entrada da UI. | todos acima |
| `ui` | Telas Swing. | `service`, `domain` (só leitura) |

> O nome raiz dos pacotes (ex.: `autoclaude.*`) está em [decisões](decisoes.md).

## Regras

1. **Dependências numa direção só.** Nenhum módulo de baixo conhece a `ui` ou o `service`.
2. **`domain` é puro.** Sem I/O, sem Swing, sem threads. Fácil de testar.
3. **Bordas são interfaces.** `TaskStore`, `FonteDeUsage`, `CanalComunicacao` e `Agente` são interfaces; as implementações concretas são escolhidas na inicialização (`Main`).
4. **Nenhum código fora de `agente` sabe qual provedor está rodando.** O resto da app fala com `Agente`; o provedor é só um dado (`ProvedorId`).
5. **A UI nunca bloqueia.** Qualquer coisa demorada roda fora da EDT (Event Dispatch Thread).
6. **Estado muda num lugar só.** Toda alteração de status passa pelo `service`, que persiste e emite um evento.

## Threads

| Thread | O que roda |
|---|---|
| **EDT** (Swing) | Desenho da tela e reação a cliques. Nunca espera processo nem disco lento. |
| **Agendador** | `ScheduledExecutorService` com 1 thread; dispara o ciclo a cada N minutos. |
| **Execução** | Uma thread por execução de agente em andamento (limite configurável; inicialmente 1 por provedor). |
| **Leitura de saída** | Lê stdout/stderr do processo e repassa linha a linha ao log. |
| **Canais** | Cada canal externo (ex.: WhatsApp) recebe mensagens na sua própria thread. |

Eventos que vêm de outras threads chegam à UI por `SwingUtilities.invokeLater`.

## Eventos

O `service` publica eventos para quem se inscrever (UI, canais de comunicação):

| Evento | Quando |
|---|---|
| `TaskAlterada` | Task criada, editada, removida ou mudou de status |
| `SubtarefaAlterada` | Subtarefa mudou de status |
| `ExecucaoIniciada` / `ExecucaoFinalizada` | Início e fim de uma chamada a um agente |
| `LinhaDeLog` | Nova linha de saída de uma execução em andamento |
| `VerificacaoSolicitada` | Subtarefa entrou em `AGUARDANDO_VERIFICACAO` |
| `AgendadorPausado` / `AgendadorRetomado` | Mudança de estado do agendador, com o motivo |
| `OrcamentoAtualizado` | Consumo da janela atual de um provedor mudou |

## Inicialização (`Main`)

1. Carregar configuração.
2. Criar implementações concretas (store, agentes habilitados, fontes de uso, canais).
3. Criar o `TaskService` injetando essas dependências (injeção manual por construtor, sem framework).
4. Abrir a `MainFrame` na EDT.
5. Iniciar o agendador **pausado** — você liga quando quiser.
6. Ao iniciar, recuperar execuções interrompidas: subtarefas que ficaram `EM_ANDAMENTO` voltam para `A_FAZER` (contando tentativa).

## Fora do escopo (por enquanto)

- Rodar em servidor ou sem interface gráfica.
- Múltiplos usuários.
- Executar Tasks em paralelo no mesmo repositório.
