# Módulo `scheduler` (agendador)

De tempos em tempos, verifica se há um agente ocioso e, se houver, entrega a próxima tarefa a ele.

- **Ocioso:** o agente não tem nenhuma execução da app em andamento.
- Só roda dentro do **horário de funcionamento** e sem passar da **reserva** (ver [consumo](consumo.md)).
- A unidade entregue é a **próxima subtarefa** da Task ou, se a Task não tiver subtarefas, a **Task inteira**. Uma Task sem subtarefas nunca fica parada na fila por isso.
- Respeita as **dependências** entre subtarefas.
- Dentro de um projeto, as Tasks rodam **na ordem em que aparecem na tela**, de cima para baixo. Para mudar a ordem, arraste a Task pela alça de três barras no fim do cartão; as subtarefas também têm a sua alça.
- Quando uma Task precisa de você (verificação, orientação, merge), o agente **segue para outra Task** em aberto.
- Projetos contínuos com poucas Tasks acionam o [Oráculo](oraculo.md).

## Como está implementado
- O botão no topo da barra lateral inicia e pausa. Rodando, faz um ciclo a cada 5 minutos e também logo depois de uma mudança sua (subtarefa nova, verificação aprovada, "Refazer").
- Cada ciclo executa, **uma de cada vez**, a próxima subtarefa na fila cujas dependências estão concluídas (ordem: projetos, Tasks, subtarefas), até a fila esvaziar.
- Pausar não interrompe a execução em andamento; fechar a app interrompe, e a subtarefa volta à fila na próxima abertura.
- Terminou bem: concluída, ou **aguardando verificação** se a subtarefa exige verificação manual. O agente responde com `FALHOU:` quando não consegue concluir, e a subtarefa fica vermelha; "Tentar de novo" a devolve à fila.
- Limite de 60 minutos por execução.
- **Direcionamento:** quando a resposta do agente termina com o bloco `DIRECIONAMENTO:`, o item fica amarelo com as perguntas e a Task inteira sai da fila (as subtarefas compartilham a sessão e seguem em ordem). Ao responder, ele volta à fila e a próxima execução retoma a sessão só com as respostas; se a sessão se perdeu, vai o pedido completo com uma seção "Direcionamento já dado". As respostas são descartadas quando o agente termina de rodar, e ficam guardadas se a execução nem terminar (tempo esgotado, CLI fora do ar).
- **Ainda não:** horário de funcionamento, reserva, várias execuções em paralelo e notificações.
