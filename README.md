# Overtime Dispatcher

Aplicação desktop que mantém uma **fila de tarefas** para agentes de IA — Claude Code, Codex e Gemini — e as executa automaticamente quando o agente está ocioso, aproveitando os créditos das suas assinaturas sem gastar a parte reservada para o seu uso manual.

Serve para qualquer tipo de projeto: desenvolvimento de software, pesquisa, escrita, organização de documentos, automações do dia a dia.

> Status: **fase de documentação e design**. Ainda não há código; este repositório descreve módulos, funcionalidades e decisões.

## Como funciona

Você organiza o trabalho em **Projetos** e, dentro deles, **Tasks**, cada uma quebrada em **Subtarefas** pequenas. Nos horários que você liberar, a app entrega a próxima subtarefa a um agente livre e registra o resultado.

Além dos projetos, você cria **Protocolos**: rotinas que o agente segue de tempos em tempos, como ver as tarefas no portal da faculdade, ler o e-mail e montar um resumo, ou organizar a agenda dele com base no que encontrou.

Para o que é trabalho seu, existem as **Trilhas**: tarefas como estudos e trabalhos da faculdade, com o tempo dividido em blocos que vão para a agenda e guardadas no seu vault do Obsidian.

Acima de tudo isso está o **Oráculo**, que observa projetos, protocolos e trilhas e sugere o que fazer a seguir.

Quando algo exige uma decisão sua, a app avisa (na tela, no desktop e pelo WhatsApp) e espera. Com a janela fechada, ela continua trabalhando na bandeja do Windows.

## Telas

Protótipos no Figma ([OvertimeDispatcher](https://www.figma.com/design/0BqwZD0E88qRrWcxHGRb49/OvertimeDispatcher)).

### Home

O ponto de partida do dia: quanto os agentes produziram, o que aconteceu na última sessão de overtime, o que está esperando uma decisão sua e os blocos de estudo e trabalho marcados para hoje.

![Home](docs/imagens/home.png)

### Projeto

Onde você organiza o trabalho dos agentes: quebra cada Task em subtarefas, define o que depende do quê, acompanha o que o agente está fazendo e revisa o que ficou pronto.

![Projeto](docs/imagens/projeto.png)

Ao criar um projeto, você escolhe a pasta do computador onde ele vive, se ela usa git, os padrões das tasks e o que o agente pode fazer.

![Novo projeto](docs/imagens/novo-projeto.png)

### Protocolo

Onde você define uma rotina para o agente seguir: o passo a passo, quando roda, o que ele pode acessar e de quem aceita tarefas. Mostra o que chegou de outros protocolos, as propostas esperando você e o que foi feito na última execução.

![Protocolo](docs/imagens/protocolo.png)

Ao criar um protocolo, você escreve o passo a passo, escolhe os horários, os acessos (sites, e-mail, calendário) e quanto esforço o agente deve usar.

![Novo protocolo](docs/imagens/novo-protocolo.png)

### Trilha

Onde ficam as suas tarefas: estudos, trabalhos e projetos pessoais, cada um com o tempo dividido em blocos. Mostra o que já foi feito, o que está na agenda e o próximo bloco. Tudo fica num arquivo do seu vault do Obsidian.

![Trilha](docs/imagens/trilha.png)

### Configurações

Define quando os agentes podem trabalhar sozinhos em cada dia da semana e como os créditos de cada assinatura se dividem entre a automação, o Oráculo e você.

![Configurações](docs/imagens/configuracoes.png)

## Oráculo

O Oráculo é quem pensa no que vem depois. Ele lê os projetos, os protocolos e as trilhas e entende em que ponto cada um está. A partir disso:

- **sugere tasks** quando um projeto contínuo fica sem trabalho, sempre com uma frase dizendo **por quê**;
- **faz diagnósticos**, como um projeto parado, testes falhando ou tasks rejeitadas pelo mesmo motivo;
- **monta e acompanha roadmaps**, principalmente de estudos, marcando o progresso a cada etapa concluída;
- **sugere estudos** (vídeos, leituras, exercícios) a partir das matérias e dos trabalhos da faculdade;
- **propõe novos protocolos** quando percebe algo que vale acompanhar sempre.

Ele não executa nada sozinho: tudo o que sugere chega para você **aceitar ou recusar**, e a recusa ensina o que não sugerir de novo. Tudo o que vem do Oráculo aparece em **roxo**, e ele tem uma fatia própria dos créditos, definida nas configurações.

![Oráculo](docs/imagens/oraculo.png)

## Princípios

- **Você tem prioridade** — a automação nunca consome os créditos reservados para o seu uso manual.
- **Você decide o que entra** — cada projeto aponta para uma pasta no seu computador; se ela usa git, o agente trabalha numa cópia separada e nada chega à versão principal sem a sua revisão.
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
| Oráculo (sugestões e roadmap) — em definição | [docs/modulos/oraculo.md](docs/modulos/oraculo.md) |
| Trilhas (suas tarefas, no vault do Obsidian) | [docs/modulos/trilhas.md](docs/modulos/trilhas.md) |

## Glossário

| Termo | Significado |
|---|---|
| **Oráculo** | Observa os projetos e protocolos e sugere o que fazer: novas tasks, diagnósticos, roadmaps e estudos. |
| **Trilha** | Conjunto de tarefas para você (estudo, trabalho, projeto pessoal), com tempo planejado em blocos que vão para a agenda; fica no seu vault do Obsidian. |
| **Protocolo** | Rotina contínua: um passo a passo que o agente segue de tempos em tempos (ex.: ver tarefas no portal da faculdade, triar o e-mail, preencher o calendário). |
| **Projeto** | Agrupa Tasks com o mesmo contexto; pode ter um repositório git. É o que aparece na barra da esquerda. |
| **Task** | Unidade de trabalho com objetivo claro (equivale a uma história de usuário). |
| **Subtarefa** | Passo pequeno e verificável de uma Task; é o que o agente executa por vez. |
| **Agente / Provedor** | Ferramenta de IA que executa subtarefas (Claude Code, Codex, Gemini), cada uma com seus créditos. |
| **Pasta do projeto** | Pasta no seu computador para onde o projeto aponta e onde o agente trabalha; pode usar git ou não. |
| **Execução** | Uma chamada a um agente para uma subtarefa, com log e consumo. |
| **Ocioso** | Estado em que nenhuma execução da app está em andamento naquele provedor. |
| **Reserva** | Parte do limite de uso de cada provedor que a automação não pode consumir. |
| **Verificação** | Etapa em que você aprova ou rejeita o resultado de uma subtarefa. |
| **Canal** | Meio de comunicação entre a app e você (tela, WhatsApp). |
