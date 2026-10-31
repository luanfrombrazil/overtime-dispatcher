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
- Trabalho para um agente, dividido em **subtarefas pequenas**.
- Tem **esforço** e **provedor · modelo**.
- Pode vir de você, de um protocolo (injetada) ou do Oráculo (roxa, só roda depois de aceita).
- Com git: trabalha numa branch `overtime/…`, e o merge na main só acontece depois da sua verificação.

## Subtarefa
- Passo pequeno de uma Task, com instrução e critério de aceite.
- Pode **depender** de outras subtarefas.
- Pode exigir **verificação manual**.
- O agente pode **pedir orientação** no meio do trabalho; enquanto espera, segue para outra Task.

## Protocolo
Rotina contínua em que o agente segue um passo a passo seu. Ver [protocolos](modulos/protocolos.md).

## Trilha
Conjunto de tarefas para você, com o tempo planejado em blocos. Ver [trilhas](modulos/trilhas.md).

## Oráculo
Observa projetos, protocolos e trilhas e sugere o que fazer. Ver [oráculo](modulos/oraculo.md).

## Estado na tela
Cada item mostra o estado por uma borda colorida: verde (concluído), azul (em andamento), amarelo (precisa de você), vermelho (falhou), cinza (na fila). Roxo é do Oráculo.
