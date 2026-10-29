# Overtime Dispatcher

Aplicação desktop em Java (Swing) que mantém uma **fila de tarefas** para agentes de código — começando pelo Claude Code e depois Codex e Gemini — e as executa automaticamente quando o agente está ocioso, respeitando um orçamento de uso por provedor que sempre deixa uma reserva para uso manual.

> Status: **fase de documentação**. Ainda não há código; este repositório descreve módulos, funcionalidades e decisões. As interfaces são modeladas no Figma.

## Ideia em uma frase

Você cadastra **Tasks** (histórias, no espírito ágil), cada uma quebrada em **Subtarefas** pequenas (*bite-sized*). Um agendador pega a próxima subtarefa sempre que um agente está livre e há orçamento naquele provedor, roda o agente em modo não interativo na pasta certa e registra o resultado. Quando uma subtarefa exige conferência humana, a app avisa você (na tela e pelo WhatsApp) e espera sua aprovação.

## Princípios

- **Simples e modular** — pacotes com responsabilidade única e dependências numa só direção.
- **A UI não sabe como as coisas são feitas** — ela só conversa com a camada de serviço.
- **Interfaces nas bordas** — persistência, fonte de uso e canais de comunicação são trocáveis.
- **Você tem prioridade** — a automação nunca consome a reserva destinada ao uso manual.
- **Vários provedores, uma interface** — Claude Code, Codex e Gemini entram como adaptadores; o resto da app não sabe qual está rodando.
- **Nada importante acontece sem rastro** — toda execução gera log, consumo e status.

## Documentação

| Documento | Conteúdo |
|---|---|
| [Arquitetura](docs/arquitetura.md) | Módulos, dependências, threads e regras gerais |
| [Domínio](docs/dominio.md) | Entidades, status e máquina de estados |
| [Funcionalidades](docs/funcionalidades.md) | Lista de funcionalidades agrupadas por módulo |
| [Interfaces](docs/interfaces.md) | Telas e componentes a modelar no Figma |
| [Roadmap](docs/roadmap.md) | Fases de entrega |
| [Decisões](docs/decisoes.md) | Decisões tomadas e em aberto |

### Módulos

| Módulo | Documento |
|---|---|
| Persistência | [docs/modulos/persistencia.md](docs/modulos/persistencia.md) |
| Agentes (Claude Code, Codex, Gemini) | [docs/modulos/agente.md](docs/modulos/agente.md) |
| Consumo (orçamento de uso) | [docs/modulos/consumo.md](docs/modulos/consumo.md) |
| Agendador | [docs/modulos/agendador.md](docs/modulos/agendador.md) |
| Comunicação (Swing / WhatsApp) | [docs/modulos/comunicacao.md](docs/modulos/comunicacao.md) |
| Serviço | [docs/modulos/servico.md](docs/modulos/servico.md) |
| Interface (Swing) | [docs/modulos/ui.md](docs/modulos/ui.md) |

## Glossário

| Termo | Significado |
|---|---|
| **Task** | Unidade de trabalho com objetivo claro (equivale a uma história de usuário). |
| **Subtarefa** | Passo pequeno e verificável de uma Task; é o que o agente executa por vez. |
| **Agente / Provedor** | Ferramenta de IA que executa subtarefas (Claude Code, Codex, Gemini), cada uma com seus créditos. |
| **Repositório** | Pasta de código local à qual uma Task pode estar vinculada (opcional). |
| **Execução** | Uma chamada a um agente para uma subtarefa, com log e consumo. |
| **Ocioso** | Estado em que nenhuma execução da app está em andamento naquele provedor. |
| **Reserva** | Parte do limite de uso de cada provedor que a automação não pode consumir. |
| **Verificação** | Etapa em que você aprova ou rejeita o resultado de uma subtarefa. |
| **Canal** | Meio de comunicação entre a app e você (tela, WhatsApp). |
