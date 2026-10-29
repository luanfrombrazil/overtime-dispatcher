# Overtime Dispatcher

Aplicação desktop que mantém uma **fila de tarefas** para agentes de IA — Claude Code, Codex e Gemini — e as executa automaticamente quando o agente está ocioso, aproveitando os créditos das suas assinaturas sem gastar a parte reservada para o seu uso manual.

Serve para qualquer tipo de projeto: desenvolvimento de software, pesquisa, escrita, organização de documentos, automações do dia a dia.

> Status: **fase de documentação e design**. Ainda não há código; este repositório descreve módulos, funcionalidades e decisões.

## Como funciona

Você organiza o trabalho em **Projetos** e, dentro deles, **Tasks**, cada uma quebrada em **Subtarefas** pequenas. Nos horários que você liberar, a app entrega a próxima subtarefa a um agente livre e registra o resultado.

Além dos projetos, você cria **Protocolos**: rotinas que o agente segue de tempos em tempos, como ver as tarefas no portal da faculdade, ler o e-mail e montar um resumo, ou organizar a agenda dele com base no que encontrou. Quando uma subtarefa exige conferência humana, a app avisa você (na tela, no desktop e pelo WhatsApp) e espera sua aprovação. Com a janela fechada, ela continua trabalhando na bandeja do Windows.

## Telas

Protótipos no Figma ([OvertimeDispatcher](https://www.figma.com/design/0BqwZD0E88qRrWcxHGRb49/OvertimeDispatcher)).

### Home

Mostra, num relance, quanto os agentes produziram, o que aconteceu na última sessão de overtime e o que está esperando uma decisão sua.

![Home](docs/imagens/home.png)

### Projeto

Onde você organiza o trabalho: cria projetos, quebra cada Task em subtarefas, acompanha o andamento e o que o agente está fazendo, e revisa o que ficou pronto.

![Projeto](docs/imagens/projeto.png)

### Protocolo

Onde você define uma rotina para o agente seguir: o passo a passo, quando roda, o que ele pode acessar e de quem aceita tarefas. Mostra o que chegou de outros protocolos, as propostas esperando você e o que foi feito na última execução.

![Protocolo](docs/imagens/protocolo.png)

### Configurações

Define quando os agentes podem trabalhar sozinhos em cada dia da semana e quanto dos seus créditos fica reservado para você usar manualmente.

![Configurações](docs/imagens/configuracoes.png)

## Princípios

- **Você tem prioridade** — a automação nunca consome os créditos reservados para o seu uso manual.
- **Você decide o que entra** — em projetos com repositório, o agente trabalha numa cópia separada; nada chega à versão principal sem a sua revisão.
- **Pergunta em vez de chutar** — quando falta informação, o agente pede orientação e segue para outra tarefa enquanto espera.
- **Trabalha em segundo plano** — roda com a janela fechada e avisa só quando precisa de você.
- **Vários agentes, uma fila** — Claude Code, Codex e Gemini usam os créditos de cada assinatura, sem mudar o jeito de trabalhar.
- **Nada acontece sem rastro** — toda execução fica registrada, com o que foi feito e quanto custou.

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
| Protocolos (rotinas contínuas) | [docs/modulos/protocolos.md](docs/modulos/protocolos.md) |

## Glossário

| Termo | Significado |
|---|---|
| **Protocolo** | Rotina contínua: um passo a passo que o agente segue de tempos em tempos (ex.: ver tarefas no portal da faculdade, triar o e-mail, preencher o calendário). |
| **Projeto** | Agrupa Tasks com o mesmo contexto; pode ter um repositório git. É o que aparece na barra da esquerda. |
| **Task** | Unidade de trabalho com objetivo claro (equivale a uma história de usuário). |
| **Subtarefa** | Passo pequeno e verificável de uma Task; é o que o agente executa por vez. |
| **Agente / Provedor** | Ferramenta de IA que executa subtarefas (Claude Code, Codex, Gemini), cada uma com seus créditos. |
| **Repositório** | Pasta versionada com git que um projeto pode ter (opcional). |
| **Execução** | Uma chamada a um agente para uma subtarefa, com log e consumo. |
| **Ocioso** | Estado em que nenhuma execução da app está em andamento naquele provedor. |
| **Reserva** | Parte do limite de uso de cada provedor que a automação não pode consumir. |
| **Verificação** | Etapa em que você aprova ou rejeita o resultado de uma subtarefa. |
| **Canal** | Meio de comunicação entre a app e você (tela, WhatsApp). |
