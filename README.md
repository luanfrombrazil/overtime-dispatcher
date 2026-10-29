# Overtime Dispatcher

Aplicação desktop que mantém uma **fila de tarefas** para agentes de código — começando pelo Claude Code e depois Codex e Gemini — e as executa automaticamente quando o agente está ocioso, respeitando um orçamento de uso por provedor que sempre deixa uma reserva para uso manual.

> Status: **fase de documentação e design**. Ainda não há código; este repositório descreve módulos, funcionalidades e decisões.

## Ideia em uma frase

Você organiza o trabalho em **Projetos** e, dentro deles, **Tasks** (histórias, no espírito ágil), cada uma quebrada em **Subtarefas** pequenas (*bite-sized*). Um agendador pega a próxima subtarefa sempre que um agente está livre e há orçamento naquele provedor, roda o agente em modo não interativo na pasta certa e registra o resultado. Quando uma subtarefa exige conferência humana, a app avisa você (na tela, no desktop e pelo WhatsApp) e espera sua aprovação. Com a janela fechada, ela continua trabalhando na bandeja do Windows.

## Telas

Protótipos do Figma ([Hyper-AI](https://www.figma.com/design/0BqwZD0E88qRrWcxHGRb49/Hyper-AI)).

### Home

Estatísticas, mapa de atividade do último ano, resumo da última sessão de overtime e o que precisa de você.

![Home](docs/imagens/home.png)

### Projeto

Barra de projetos à esquerda com o estado na borda colorida; no centro, as Tasks do projeto com as subtarefas e o console do projeto.

![Projeto](docs/imagens/projeto.png)

### Configurações

Horário de funcionamento por dia (48 segmentos de 30 min), reserva de uso para você por provedor e parâmetros de execução.

![Configurações](docs/imagens/configuracoes.png)

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
| Comunicação (app, desktop, WhatsApp) | [docs/modulos/comunicacao.md](docs/modulos/comunicacao.md) |
| Serviço | [docs/modulos/servico.md](docs/modulos/servico.md) |
| Interface | [docs/modulos/ui.md](docs/modulos/ui.md) |
| RPC (chamadas externas e dos agentes) | [docs/modulos/rpc.md](docs/modulos/rpc.md) |

## Glossário

| Termo | Significado |
|---|---|
| **Projeto** | Agrupa Tasks com o mesmo contexto; pode ter um repositório git. É o que aparece na barra da esquerda. |
| **Task** | Unidade de trabalho com objetivo claro (equivale a uma história de usuário). |
| **Subtarefa** | Passo pequeno e verificável de uma Task; é o que o agente executa por vez. |
| **Agente / Provedor** | Ferramenta de IA que executa subtarefas (Claude Code, Codex, Gemini), cada uma com seus créditos. |
| **Repositório** | Pasta de código local à qual uma Task pode estar vinculada (opcional). |
| **Execução** | Uma chamada a um agente para uma subtarefa, com log e consumo. |
| **Ocioso** | Estado em que nenhuma execução da app está em andamento naquele provedor. |
| **Reserva** | Parte do limite de uso de cada provedor que a automação não pode consumir. |
| **Verificação** | Etapa em que você aprova ou rejeita o resultado de uma subtarefa. |
| **Canal** | Meio de comunicação entre a app e você (tela, WhatsApp). |
