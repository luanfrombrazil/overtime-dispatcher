# Arquitetura

Aplicação desktop em Java, simples e modular, com classes bem definidas, seguindo **MVC** (Model, View, Controller).

## Camadas

| Camada | O que contém |
|---|---|
| **Model** | Os dados: as entidades (Projeto, Task, Subtarefa, Protocolo, Trilha, Oráculo) e as suas regras |
| **Service** | A lógica que opera sobre o Model: agentes, agendamento, consumo, notificações, RPC e persistência |
| **Controller** | Liga as ações da tela ao Model e aos serviços, e atualiza a View quando algo muda |
| **View** | Telas em Swing |

## Estrutura do código

```
src/main/java/io/github/luanfrombrazil/overtimedispatcher/
├─ OvertimeDispatcher.java   ponto de entrada
├─ model/                    entidades
├─ service/
│  ├─ agente/                executar os agentes (Claude Code, Codex, Gemini)
│  ├─ scheduler/             entregar a próxima tarefa a um agente ocioso
│  ├─ consumo/               horário de funcionamento e reserva
│  ├─ comunicacao/           notificações (app, desktop, WhatsApp)
│  ├─ rpc/                   funções expostas aos agentes (RPC e MCP)
│  └─ persistence/           ler e salvar os dados em JSON
├─ controller/               controladores das telas
└─ view/                     telas em Swing
```

Detalhes de cada serviço em [`modulos/`](modulos/).

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
