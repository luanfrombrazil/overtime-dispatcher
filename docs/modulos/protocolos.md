# Protocolos

Rotinas contínuas em que o agente segue um passo a passo definido por você. Diferente de um projeto, um protocolo não termina: roda de tempos em tempos, traz o que encontrou e age dentro dos limites permitidos.

Exemplos:
- **Tarefas da faculdade:** fazer crawling no portal e buscar as tarefas a fazer.
- **E-mail:** verificar o e-mail seguindo um protocolo.
- **Agenda:** preencher o calendário a partir do que os outros protocolos encontram.

## Acessos

| Acesso | Como | O que pode |
|---|---|---|
| **Sites** | Crawling | Só ler; roda sem aprovação |
| **E-mail** | Conector | Ler e criar rascunhos; nunca enviar, apagar ou modificar |
| **Calendário** | Conector | Agir sozinho na **agenda própria do agente** |

## Injeção de tasks

Um agente pode criar trabalho para outro. Exemplo: o protocolo da faculdade encontra uma entrega e injeta no protocolo **Agenda** uma task para colocar a data no calendário, com a descrição.

O destino pode ser outro protocolo ou um projeto. A task entra **direto** ou como **proposta**, que você aceita ou recusa.
