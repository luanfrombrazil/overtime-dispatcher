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

## Base técnica

| Item | Escolha |
|---|---|
| Linguagem | **Java 25 LTS** (records, sealed interfaces, pattern matching, virtual threads, FFM para APIs do Windows) — D-36 |
| Build | **Maven** — D-36 |
| JSON | **Jackson** — D-35 |
| Interface | Swing + **FlatLaf** (`FlatDarkLaf`) — D-13, D-25 |
| Pacote raiz | `io.github.luanfrombrazil.overtimedispatcher` — D-37 |
| Notificações com botões | biblioteca nativa de toast do Windows, a escolher (A-15) — D-41 |
| Distribuição | `.exe` com Java embutido e reduzido (`jpackage` + `jlink`) — D-43 |

## Build e distribuição

| Uso | Comando | Resultado |
|---|---|---|
| Desenvolvimento | `mvn package` | `overtime-dispatcher.jar` (precisa de Java 25 instalado) |
| Uso final | `mvn package -Pdistribuicao` | `OvertimeDispatcher.exe` numa pasta com um Java reduzido só aos módulos usados (sem precisar de Java instalado) |
| Instalador (opcional) | mesmo perfil, com o WiX Toolset instalado | instalador que cria atalho no menu Iniciar, aparece em "Adicionar ou remover programas" e registra a identidade da app para as notificações com botões |

**Ícone próprio** (nenhum símbolo do Java):

| Onde | Como |
|---|---|
| `.exe` e atalhos | `.ico` passado ao `jpackage --icon` |
| Janela e barra de tarefas | `setIconImages` com PNGs de 16, 32, 48 e 256 px |
| Bandeja | `TrayIcon` com PNG de 16/32 px, uma variação por estado (normal, executando, precisa de você) |
| Notificações | ícone do atalho registrado pelo instalador |

## Pacotes

Todos abaixo de `io.github.luanfrombrazil.overtimedispatcher`.

| Pacote | Responsabilidade | Depende de |
|---|---|---|
| `domain` | Entidades, enums e regras puras (transições de status). | nada |
| `persistence` | Salvar e carregar Tasks, Repositórios, Execuções e config. | `domain` |
| `agente` | Interface comum para agentes de IA (Claude Code, Codex, Gemini...): montar o prompt, rodar o processo, interpretar a saída. Um adaptador por provedor. | `domain` |
| `consumo` | Contabilizar uso **por provedor** e decidir se a automação pode rodar. | `domain` |
| `scheduler` | Ciclo periódico: ocioso? orçamento? próxima subtarefa? qual provedor? | `domain`, `agente`, `consumo` (via interfaces) |
| `comunicacao` | Enviar avisos e receber comandos (Swing, WhatsApp). | `domain` |
| `service` | Casos de uso da aplicação; único ponto de entrada da UI, dos canais e do RPC. | todos acima |
| `ui` | Telas Swing. | `service`, `domain` (só leitura) |
| `rpc` | Servidor JSON-RPC local para agentes e programas externos chamarem funções da app ([detalhes](modulos/rpc.md)). | `service`, `domain` |


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
| **RPC** | Pool pequeno do servidor HTTP local; cada chamada vira uma chamada ao `service`. |

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
5. Iniciar o servidor RPC em `127.0.0.1`.
6. Iniciar o agendador **pausado** — você liga quando quiser.
7. Ao iniciar, recuperar execuções interrompidas: subtarefas que ficaram `EM_ANDAMENTO` voltam para `A_FAZER` (contando tentativa).

## Ciclo de vida: bandeja do Windows

A app **continua rodando com a janela fechada** (decisão D-31):

- Fechar a janela (X) esconde a janela e mantém a app na **bandeja do sistema** (`java.awt.SystemTray`). Agendador, agentes, RPC e canais seguem funcionando.
- O ícone da bandeja muda conforme o estado geral: normal, executando, precisa de você.
- Menu do ícone: **Abrir**, **Pausar/Retomar agendador**, **Sair**. Só "Sair" encerra a app (e pergunta se há execução em andamento).
- Duplo clique no ícone, ou clique numa notificação, reabre a janela.
- **Resumo da ausência:** a app guarda o momento em que a janela foi escondida. Ao reabrir, se algo aconteceu nesse intervalo, mostra o `ResumoAusenciaDialog` com o que foi concluído, o que precisa de você e o que falhou (decisão D-32).
- Opção nas configurações: iniciar com o Windows, já na bandeja.

## Fora do escopo (por enquanto)

- Rodar em servidor ou sem interface gráfica.
- Múltiplos usuários.
- Push automático para o remoto (o merge é local e o push fica com você).
