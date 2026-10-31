# Arquitetura

Aplicação desktop em Java, simples e modular, com classes bem definidas, seguindo **MVC** (Model, View, Controller).

## MVC

| Camada | O que contém |
|---|---|
| **Model** | Projetos, Tasks, subtarefas, Protocolos, Trilhas, as regras entre eles e a persistência |
| **View** | Telas em Swing |
| **Controller** | Liga as ações da tela ao Model e atualiza a View quando o Model muda |

## Módulos

| Módulo | Camada | Responsabilidade |
|---|---|---|
| `domain` | Model | Entidades e regras |
| `persistence` | Model | Guardar e carregar os dados |
| `agente` | Model | Executar os agentes (Claude Code, Codex, Gemini) |
| `consumo` | Model | Reserva de uso e horário de funcionamento |
| `scheduler` | Model | Pegar a próxima tarefa quando um agente fica ocioso |
| `comunicacao` | Model | Notificações na app, no desktop e no WhatsApp |
| `rpc` | Model | Funções da app que os agentes podem chamar (RPC e MCP) |
| `controller` | Controller | Controladores de cada tela |
| `ui` | View | Telas em Swing |

Detalhes de cada um em [`modulos/`](modulos/).

## Tecnologia

| Item | Escolha |
|---|---|
| Linguagem | Java 25 |
| Build | Maven |
| JSON | Jackson |
| Interface | Swing + FlatLaf (tema escuro) |
| Pacote raiz | `io.github.luanfrombrazil.overtimedispatcher` |

## Distribuição

`.exe` com Java embutido e reduzido (`jpackage` + `jlink`), com ícone próprio na janela, na bandeja e no executável. Durante o desenvolvimento, `.jar`.

## Execução em segundo plano

Fechar a janela não encerra a app: ela continua na bandeja do Windows, executando e notificando.
