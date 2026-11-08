# Domínio

```
Projeto ──► Task ──► Subtarefa
Protocolo
Trilha ──► Tarefa sua ──► Bloco
Oráculo (observa tudo e sugere)
```

## Projeto
- Agrupa Tasks com o mesmo contexto.
- Aponta para uma **pasta do sistema**; pode usar **git**.
- **Finito** (as Tasks se esgotam) ou **contínuo** (nunca termina; quando fica com poucas Tasks, o Oráculo sugere novas).

## Task
- Trabalho para um agente, com **instrução** e **critério de aceite** próprios.
- **Executável por conta própria:** sem subtarefas, a Task inteira é entregue ao agente como uma unidade.
- Pode, opcionalmente, ser quebrada em **subtarefas pequenas**; nesse caso, o agente executa as subtarefas em ordem.
- Tem **esforço** e **provedor · modelo**.
- Pode vir de você, de um protocolo (injetada) ou do Oráculo (roxa, só roda depois de aceita).
- Com git: trabalha numa branch `overtime/…`, e o merge na main só acontece depois da sua verificação.

## Subtarefa
- Passo pequeno e **opcional** de uma Task, com instrução e critério de aceite.
- Pode **depender** de outras subtarefas.
- Pode exigir **verificação manual**.
- Diz ao agente se deve fazer **commit** ao terminar (marcado por padrão) e **push** depois do commit (desmarcado por padrão; só vale com commit).
- Tem o próprio **esforço** (barra de Baixo a Máximo; Médio por padrão) e **modelo** (Padrão, Opus, Sonnet ou Haiku; Padrão deixa a escolha ao agente). Subtarefas antigas, sem esforço, usam o da Task.
- O agente pode **pedir direcionamento**: para com perguntas, cada uma com opções sugeridas. A subtarefa fica amarela ("Direcionamento") e segura a Task; ao expandir a subtarefa no cartão da Task, aparecem as perguntas, com um botão de rádio por opção e um "Outro" com campo de texto. Enquanto espera, o agendador segue para outra Task. A Task sem subtarefas também pode perguntar; as perguntas ficam no próprio cartão.
- Direcionamento e **verificação manual** são esperas diferentes, ambas amarelas: a verificação aprova ou manda refazer; o direcionamento responde e retoma.

## Protocolo
Rotina contínua em que o agente segue um passo a passo seu. Ver [protocolos](modulos/protocolos.md).

## Trilha
Conjunto de tarefas para você, com o tempo planejado em blocos. Ver [trilhas](modulos/trilhas.md).

## Oráculo
Observa projetos, protocolos e trilhas e sugere o que fazer. Ver [oráculo](modulos/oraculo.md).

## Estado na tela
Cada item mostra o estado por uma borda colorida: verde (concluído), azul (em andamento), amarelo (precisa de você), vermelho (falhou), cinza (na fila). Roxo é do Oráculo.
