# Protocolos

Rotinas contínuas em que o agente segue um passo a passo definido por você, de tempos em tempos (decisão D-44). Diferente de um Projeto, um protocolo não termina: ele roda, traz o que encontrou, age dentro dos limites permitidos e volta a rodar no próximo horário.

Exemplos:
- **Tarefas da faculdade:** entrar no portal, listar as atividades com prazo nos próximos 14 dias e avisar das novas.
- **Triagem do e-mail:** ler os e-mails não lidos, separar o que precisa de resposta e montar um resumo.
- **Agenda:** a partir das tarefas e e-mails, criar ou ajustar eventos na agenda do agente.

## O que um protocolo tem

| Campo | Exemplo |
|---|---|
| Nome e ícone | "Tarefas da faculdade" |
| Passo a passo | Texto livre com as regras que o agente segue |
| Quando roda | Horários fixos (ex.: 7h e 19h) ou intervalo (ex.: a cada 4h), dentro do horário de funcionamento |
| Acessos | Sites (crawling), e-mail, calendário |
| Provedor · modelo · esforço | Como nas Tasks |
| Avisar quando | Só quando houver algo novo / sempre / nunca (só registra) |
| Fila de entrada | Tasks injetadas por outros agentes, processadas na próxima execução |
| Aceitar tasks direto de | Origens confiáveis; as demais chegam como proposta |

## Acessos e o que o agente pode fazer

Os limites são **técnicos**, não só instruções no prompt: a app configura as ferramentas de cada execução de forma que a ação proibida não esteja disponível.

| Acesso | Como | Pode | Não pode | Aprovação |
|---|---|---|---|---|
| **Sites** (ex.: portal da faculdade) | Crawling, num navegador da própria app com perfil separado; você faz o login uma vez | Ler, navegar, extrair informações | Enviar formulários, entregar trabalhos, alterar dados | Não precisa: só leitura (D-45) |
| **E-mail** | Conector (ex.: Gmail) | Ler, buscar e **criar rascunhos** | **Apagar, mover, marcar, arquivar ou enviar** | Não precisa: rascunho só sai se você enviar (D-46) |
| **Calendário** | Conector (ex.: Google Calendar), restrito à **agenda própria do agente** | Criar, editar e apagar eventos **na agenda do agente** | Mexer nas suas agendas | Não precisa: age sozinho na própria agenda (D-47) |

Como funciona na prática:
- **Sites:** a sessão do navegador fica no perfil da app; a app **nunca guarda nem digita a sua senha**. Se a sessão expirar, o protocolo fica amarelo ("Login") até você entrar de novo.
- **E-mail:** o conector é autorizado com permissão de leitura e de rascunhos; as ferramentas de enviar, apagar, mover e marcar ficam bloqueadas na execução. Um rascunho criado pelo agente só sai da caixa se você mesmo o enviar.
- **Calendário:** você cria uma agenda para o agente (ex.: "Overtime") e a compartilha com você; o protocolo só recebe o ID dessa agenda. Você vê os eventos dela junto com os seus, mas o agente não toca nas suas.

## Execução

- Os protocolos usam o mesmo agendador, a mesma reserva de uso e o mesmo horário de funcionamento das Tasks.
- No horário de um protocolo, ele **passa na frente** das Tasks na fila (é sensível ao tempo), mas nunca interrompe uma execução em andamento.
- Cada execução gera um **resultado** com: o que foi encontrado, o que é **novo** desde a última vez, as ações feitas (ex.: eventos criados) e o consumo.
- O agente compara com a última execução para avisar só do que mudou.

## Injeção de tasks

Um agente pode **criar trabalho para outro** (decisão D-48). Exemplo: o protocolo "Tarefas da faculdade" encontra uma entrega nova e injeta uma task no protocolo "Agenda": *"Incluir no calendário: Entrega do relatório de Física, 12/11 23:59, descrição com o link da atividade"*. Na próxima execução, o protocolo "Agenda" processa essa task e cria o evento.

### Destinos

| Destino | O que acontece |
|---|---|
| **Outro protocolo** | A task entra na **fila de entrada** do protocolo de destino, que a processa na próxima execução (ou logo, se o destino estiver configurado para rodar ao receber) |
| **Um projeto** | Vira uma Task do projeto, com as subtarefas que o agente sugerir |

### Proposta ou task direta

Cada destino define **de quem aceita tasks direto**; o resto chega como **proposta** (decisão D-49):

| Tipo | Quando | Como aparece |
|---|---|---|
| **Task direta** | A origem está na lista de confiança do destino (ex.: "Agenda" aceita direto de "Tarefas da faculdade") | Entra na fila e roda sem passar por você |
| **Proposta** | Origem fora da lista, ou destino que exige aprovação (padrão para projetos) | Fica amarela com `1 proposta`; você aceita, edita ou descarta |

A configuração fica na tela de cada protocolo e projeto: **"Aceitar tasks direto de:"** com a lista das origens confiáveis.

### Proteções

- **Sem duplicatas:** cada task injetada leva uma **chave** (ex.: `entrega:fisica:relatorio-3`). Se já existir uma com a mesma chave, a app atualiza a existente em vez de criar outra. Assim, o crawling diário não recria a mesma entrega.
- **Sem laços:** um protocolo não injeta nele mesmo; uma cadeia de injeções tem no máximo 3 saltos; e há um limite de injeções por execução (ex.: 20). Passou disso, vira proposta.
- **Rastro:** toda task injetada guarda a **origem** (`Tarefas da faculdade › execução de 29/10 07:00`), visível na tela.
- **Mesmos limites de acesso:** a task injetada roda com os acessos e permissões **do destino**, nunca com os da origem.

### Como o agente injeta

Pelo RPC / MCP (ver [rpc](rpc.md)): ferramenta `injetar_task` com destino, título, descrição, dados (ex.: data, link) e chave.

## Estado visual

| Estado | Quando | Texto |
|---|---|---|
| Verde | Última execução ok, nada novo | nenhum |
| Azul | Executando | nenhum |
| Amarelo | Há novidades para ver, propostas esperando você, ou o login do site expirou | `3 novas`, `1 proposta`, `Login` |
| Vermelho | Falhou | `Falhou` |
| Cinza | Desativado | nenhum |

## Na interface

- A barra da esquerda tem duas seções: **Projetos** e **Protocolos**.
- A tela do protocolo mostra o passo a passo, quando roda, os acessos, a última execução (novidades em destaque) e o histórico.
- Botões: rodar agora, ativar ou desativar, editar.
- Na Home, as novidades dos protocolos entram em "Precisa de você", e as execuções entram no resumo da sessão.
