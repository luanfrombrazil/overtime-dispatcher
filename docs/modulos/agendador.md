# Módulo `scheduler` (agendador)

De tempos em tempos, verifica se há um agente ocioso e, se houver, entrega a próxima tarefa a ele.

- **Ocioso:** o agente não tem nenhuma execução da app em andamento.
- Só roda dentro do **horário de funcionamento** e sem passar da **reserva** (ver [consumo](consumo.md)).
- Respeita as **dependências** entre subtarefas.
- Quando uma Task precisa de você (verificação, orientação, merge), o agente **segue para outra Task** em aberto.
- Projetos contínuos com poucas Tasks acionam o [Oráculo](oraculo.md).
