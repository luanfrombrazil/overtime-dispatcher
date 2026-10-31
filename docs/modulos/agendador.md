# Módulo `scheduler` (agendador)

De tempos em tempos, verifica se há um agente ocioso e, se houver, entrega a próxima tarefa a ele.

- **Ocioso:** o agente não tem nenhuma execução da app em andamento.
- Só roda dentro do **horário de funcionamento** e sem passar da **reserva** (ver [consumo](consumo.md)).
- A unidade entregue é a **próxima subtarefa** da Task ou, se a Task não tiver subtarefas, a **Task inteira**. Uma Task sem subtarefas nunca fica parada na fila por isso.
- Respeita as **dependências** entre subtarefas.
- Quando uma Task precisa de você (verificação, orientação, merge), o agente **segue para outra Task** em aberto.
- Projetos contínuos com poucas Tasks acionam o [Oráculo](oraculo.md).
