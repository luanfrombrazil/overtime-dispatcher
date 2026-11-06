# Protocolos

Rotinas contínuas em que o agente segue um passo a passo definido por você. Diferente de um projeto, um protocolo não termina: roda de tempos em tempos, traz o que encontrou e age dentro dos limites permitidos.

Exemplos:
- **Tarefas da faculdade:** fazer crawling no portal e buscar as tarefas a fazer.
- **E-mail:** verificar o e-mail seguindo um protocolo.
- **Agenda:** preencher o calendário a partir do que os outros protocolos encontram.

## O protocolo

Criado e editado pelo mesmo pop-up (**+ Protocolo** na barra lateral, **Editar** na tela dele): nome, passo a passo, quando roda, acessos, provedor e esforço. Nome e passo a passo são obrigatórios. **Excluir**, na edição, apaga o protocolo, a fila e a pasta de trabalho.

O interruptor da tela liga e desliga o protocolo. Desligado, ele não roda pela agenda, só pelo **Rodar agora**; ao religar, não roda o que passou enquanto estava desligado.

Na barra lateral, o protocolo fica amarelo quando há propostas esperando resposta e mostra "Desligado" quando está desligado.

## Agenda

| Modo | Como roda |
|---|---|
| **Horários fixos** | Todo dia, em cada horário da lista (ex.: 08:00 e 18:00) |
| **Intervalo** | A cada N minutos, contados do início da execução anterior |

Sem nenhum horário, o protocolo só roda pelo **Rodar agora**.

A agenda conta a partir do início da última execução. Um horário que passou com o agendador ocupado, fora do horário de funcionamento, com o provedor no limite de uso ou com a app fechada roda **uma vez**, assim que der; um protocolo que nunca rodou começa a contar quando o agendador o vê ligado pela primeira vez.

## Acessos

| Acesso | Como | O que pode |
|---|---|---|
| **Sites** | Crawling | Só ler os endereços cadastrados; sem login, formulários nem publicação |
| **E-mail** | Conector | Ler e criar rascunhos; nunca enviar, responder, encaminhar, apagar, arquivar ou modificar |
| **Calendário** | Conector | Criar, editar e apagar eventos só na **agenda própria do agente** ("Overtimer") |

Cada acesso vale de dois jeitos:

- **Ferramentas do agente:** sites liberam o `WebFetch` apenas nos domínios cadastrados (`WebFetch(domain:...)`); e-mail e calendário liberam o servidor MCP escolhido no pop-up (`mcp__<servidor>`). Acesso marcado sem conector não libera nada. No e-mail, as ferramentas do conector que enviam, respondem, encaminham, apagam, arquivam ou modificam e-mails, marcadores, filtros e rascunhos existentes ficam bloqueadas pelo nome (as do Gmail do Claude e os nomes comuns de outros servidores): um site lido não consegue fazer o agente enviar nada. Um servidor que use outros nomes para essas ações fica só com a regra do prompt.
- **Regras no prompt:** o que a lista de ferramentas não consegue fechar dentro de cada site ou conector (só leitura nos sites, só a agenda do agente no calendário). Os acessos não marcados viram proibições explícitas.

## Pasta de trabalho

O agente roda em `protocolos-rotina/<id>/trabalho`, na pasta de dados da app, criada na primeira execução e mantida entre uma e outra: é ali que ele guarda o que precisa consultar na próxima vez. Ele só lê e edita dentro dela, sem shell, e as credenciais da pasta do usuário ficam bloqueadas como nas Tasks.

## Execução

Os protocolos entram na mesma fila do agendador, **um de cada vez com as Tasks**: quando a agenda de um chega, ele passa na frente da próxima Task. Valem as mesmas regras das Tasks:

- só começa dentro do horário de funcionamento;
- não começa com o provedor no limite de uso;
- conta no máximo de execuções por intervalo;
- com o agendador pausado, nada roda.

Além dos ciclos do intervalo, o agendador acorda sozinho no próximo horário de protocolo.

**Rodar agora** pede a execução no próximo ciclo e faz esse ciclo já, mesmo fora da agenda ou com o protocolo desligado. O pedido vale uma execução e segue as regras acima; com o agendador pausado, espera ele ser ligado.

Cada execução é uma sessão nova do agente. O prompt leva o passo a passo, as tasks da fila de entrada (com a origem de cada uma) e as regras dos acessos. O agente responde com um resumo do que encontrou e fez, ou começa com `FALHOU:` quando não conseguiu concluir.

Com sucesso, saem da fila de entrada os itens que foram no prompt; os que chegaram durante a execução ficam para a próxima. Com falha, a fila fica como estava. Uma execução que falha não é repetida a cada ciclo: o protocolo espera o próximo horário.

## Histórico

As 20 execuções mais recentes ficam em `protocolos-rotina/<id>/historico.json` (formato em [persistência](persistencia.md)), com início, fim, resultado e o log do agente. A tela do protocolo mostra o console com duas abas:

- **Última execução:** o resumo, o log e a resposta do agente; enquanto o protocolo roda, mostra a execução ao vivo.
- **Histórico:** uma linha por execução, da mais recente para a mais antiga, com horário, se deu certo e a primeira linha do resultado.

O console só aparece depois da primeira execução.

## Injeção de tasks

Um agente pode criar trabalho para outro. Exemplo: o protocolo da faculdade encontra uma entrega e injeta no protocolo **Agenda** uma task para colocar a data no calendário, com a descrição.

O destino pode ser outro protocolo ou um projeto. A task entra **direto** na fila de entrada, quando a origem está em "Aceita direto de", ou como **proposta**, que você aceita ou recusa. A injeção ainda não está implementada: a fila de entrada e as propostas já existem no protocolo e a fila já é levada ao prompt e consumida.
