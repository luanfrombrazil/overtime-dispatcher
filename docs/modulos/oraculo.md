# Oráculo

> **Em definição.** Este documento registra o que já foi decidido; os pontos em aberto estão no fim.

O Oráculo **observa e propõe**. Ele varre os projetos e protocolos, entende o estado em que estão e sugere o que fazer a seguir. Ele **não executa trabalho**: o que ele sugere entra pelos mecanismos que já existem (propostas e tasks, decisões D-48 e D-49).

Fica **separado** de Projetos e Protocolos: é o primeiro item da barra lateral, com o símbolo de um **olho roxo** e um fundo levemente roxo.

## Projetos finitos e contínuos

| Tipo | Comportamento |
|---|---|
| **Finito** | As Tasks se esgotam e o projeto termina |
| **Contínuo** | Nunca termina (ex.: um produto em evolução, estudos de uma matéria). Quando fica **sem tasks ou com poucas** (limite configurável, ex.: menos de 2 pendentes), o Oráculo é acionado para sugerir as próximas |

Projetos contínuos têm um campo de **objetivo** (onde se quer chegar), que o Oráculo usa como norte.

## O que ele lê

Tudo o que ajuda a entender o estado de um projeto:
- arquivos da pasta do projeto e, se houver git, o histórico de commits;
- Tasks concluídas, rejeitadas e com falha, e os comentários de rejeição;
- resultados dos protocolos (ex.: tarefas e matérias encontradas no portal da faculdade);
- o objetivo e a descrição do projeto;
- o roadmap do projeto (ver abaixo).

## O que ele entrega

| Entrega | Exemplo |
|---|---|
| **Sugestões de tasks** | "Adicionar testes para o fluxo de pagamento", com subtarefas propostas |
| **Diagnósticos** | "Projeto parado há 12 dias", "testes falhando desde ontem", "3 tasks rejeitadas pelo mesmo motivo" |
| **Roadmap** | Sugere um roadmap (principalmente para **estudos**) e marca o progresso nele conforme as tasks são concluídas |
| **Sugestões de estudo** | A partir dos trabalhos recentes e das matérias da faculdade: vídeos, leituras, exercícios e pequenos projetos práticos |

### Tasks do Oráculo

O Oráculo **inclui tasks** diretamente na lista do projeto (decisão D-54):
- aparecem **em roxo**: borda roxa, fundo levemente roxo e o olho do Oráculo ao lado;
- embaixo do título, uma **frase curta com o porquê** da sugestão (ex.: "O login mudou 3 vezes este mês e não tem testes"). É a exceção à regra de texto mínimo: o motivo é o que permite decidir rápido;
- cada uma tem **Aceitar** e **Recusar**;
- **Aceitar** transforma em Task normal (entra na fila com a cor de estado comum); **Recusar** pede um motivo opcional, que o Oráculo usa para não sugerir a mesma coisa de novo;
- enquanto não for aceita, a task do Oráculo **não roda**.

Nos projetos que você marcar para aceitar direto do Oráculo, as tasks entram já aceitas.

## Além de um projeto

- **Visão geral:** o que merece a sua atenção em todos os projetos e protocolos.
- **Estudos:** cruza o que os protocolos encontram (trabalhos, provas, matérias) com o que já foi estudado e sugere o que estudar.
- Outras funções inspiradas em ferramentas parecidas, a definir (ver em aberto).

## Onde aparece

| Lugar | O quê |
|---|---|
| **Botão do Oráculo** (topo da barra lateral) | Abre a tela do Oráculo; mostra quantas sugestões novas há (`3 sugestões`) |
| **Tela do Oráculo** | **Feed** com sugestões, diagnósticos e estudos de todos os projetos |
| **Seção dentro de cada projeto** | O que o Oráculo diz sobre aquele projeto, incluindo o roadmap |
| **Notificações** | Sugestões e diagnósticos importantes, com texto mínimo |

Cor própria: **roxo**. As sugestões do Oráculo aparecem com borda roxa, separada das cores de estado.

## Consumo

O Oráculo tem uma **reserva própria**, definida em **Configurações › Agendamento**: cada controle de reserva tem uma **segunda alça, roxa**, que delimita a parte do Oráculo entre a automação (azul) e a sua reserva (cinza). Ele nunca usa o que está reservado para você nem o orçamento das Tasks.

## Símbolo

`IconeOraculo` no Figma, em duas versões:
- **Detalhado:** olho amendoado com íris roxa, dentro de um anel com pétalas em mandala. Usado em telas grandes e no cabeçalho do Oráculo.
- **Simples:** anel, olho e íris. Usado na barra lateral e nas notificações, de 16 a 24 px.

## Em aberto

- Quais funções inspiradas em ferramentas parecidas entram (ex.: revisão semanal, detecção de tarefas repetidas que poderiam virar protocolo, sugestão de prioridades).
- Como o roadmap é exibido e editado.
- Se o Oráculo também roda em ciclo fixo (ex.: revisão semanal), além do gatilho de "poucas tasks".
- Valor padrão da reserva própria.
