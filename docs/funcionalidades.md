# Funcionalidades

Lista de funcionalidades por área. A coluna **Fase** remete ao [roadmap](roadmap.md).

## Projetos

| ID | Funcionalidade | Fase |
|---|---|---|
| F-10 | Criar, editar e remover projeto (nome, ícone de iniciais, descrição) | 1 |
| F-11 | Vincular repositório git ao projeto (pasta, branch padrão) ou deixar sem | 1 |
| F-12 | Validar se a pasta existe e é um repositório git | 1 |
| F-14 | Padrões das Tasks (provedor, esforço) e permissões do agente por projeto | 1 |
| F-15 | Barra de projetos com estado agregado (borda colorida) e motivo curto | 1 |
| F-16 | Buscar projeto | 1 |

## Tasks e subtarefas

| ID | Funcionalidade | Fase |
|---|---|---|
| F-01 | Criar, editar e remover Task dentro de um projeto (título, descrição, prioridade) | 1 |
| F-03 | Adicionar, editar, remover e reordenar subtarefas (arrastar e soltar) | 1 |
| F-04 | Definir instrução, critério de aceite e "requer verificação" por subtarefa | 1 |
| F-05 | Tasks do projeto em cartões, ordenadas por prioridade, com estado por borda | 1 |
| F-17 | Dependências entre subtarefas ("depende de"), com bloqueio de ciclos | 1 |
| F-18 | Escolher "Provedor · modelo" por Task, com padrão no projeto | 2 |
| F-06 | Pausar e retomar uma Task | 3 |
| F-07 | Cancelar e reabrir subtarefa | 2 |
| F-08 | Sugerir subtarefas com um agente a partir da descrição da Task | 6 |
| F-09 | Duplicar Task (modelo para tarefas recorrentes) | 6 |
| F-13 | Definir o nível de esforço por Task | 1 |

## Execução

| ID | Funcionalidade | Fase |
|---|---|---|
| F-20 | Executar uma subtarefa manualmente (menu de contexto) | 2 |
| F-21 | Console do projeto: log ao vivo, histórico e pendências do projeto | 2 |
| F-22 | Cancelar execução em andamento | 2 |
| F-23 | Histórico de execuções por subtarefa, com log, consumo e resultado | 2 |
| F-24 | Sessão compartilhada entre subtarefas da mesma Task | 2 |
| F-25 | Novas tentativas automáticas até o limite | 3 |
| F-26 | Recuperar execuções interrompidas ao reabrir a app | 3 |
| F-27 | Branch `overtime/<id>` e worktree por Task | 2 |
| F-28 | Revisão do diff da Task e merge com squash (sem push) | 4 |
| F-29 | Pedido de direcionamento pelo agente e resposta sua | 4 |

## Provedores (multiagente)

| ID | Funcionalidade | Fase |
|---|---|---|
| F-30 | Adaptador Claude Code | 2 |
| F-31 | Adaptador Codex | 4 |
| F-32 | Adaptador Gemini (Gemini CLI / Antigravity) | 4 |
| F-33 | Escolher provedor por Task ou deixar no modo automático | 4 |
| F-34 | Verificar se cada CLI está instalado e autenticado | 2 |
| F-35 | Trocar o provedor de uma Task manualmente, com resumo do que já foi feito | 6 |

## Agendador e consumo

| ID | Funcionalidade | Fase |
|---|---|---|
| F-40 | Iniciar e pausar o agendador; definir intervalo | 3 |
| F-41 | Executar automaticamente quando o provedor está ocioso | 3 |
| F-42 | Contabilizar consumo por provedor, por janela e por período | 3 |
| F-43 | Tetos por provedor que preservam uma reserva para uso manual | 3 |
| F-44 | Horários permitidos para a automação | 3 |
| F-45 | Botão "Vou usar agora" (pausa temporária por provedor) | 3 |
| F-46 | Pausar provedor automaticamente ao receber erro de limite | 3 |
| F-47 | Painel de consumo por provedor | 3 |
| F-48 | Detectar uso manual do CLI fora da app | 6 |

## Verificação

| ID | Funcionalidade | Fase |
|---|---|---|
| F-50 | Fila de verificações pendentes | 4 |
| F-51 | Aprovar subtarefa | 4 |
| F-52 | Rejeitar com comentário, que entra no próximo prompt | 4 |
| F-53 | Ver resumo, log e arquivos alterados (diff) antes de decidir | 4 |

## Comunicação

| ID | Funcionalidade | Fase |
|---|---|---|
| F-60 | Notificações dentro da app (sino com contador de não lidas) | 4 |
| F-66 | Rodar na bandeja do Windows com a janela fechada (menu Abrir / Pausar / Sair) | 3 |
| F-67 | Notificações no desktop com botões (Aprovar, Rejeitar, Responder, Merge); clique abre a pendência | 3 |
| F-65A | Modo silencioso: agora, faixa automática diária, falhas furam o silêncio | 3 |
| F-68 | Pop-up "Enquanto você estava fora" ao voltar da bandeja | 3 |
| F-69 | Iniciar com o Windows, já na bandeja | 3 |
| F-61 | Enviar avisos pelo WhatsApp | 5 |
| F-62 | Aprovar, rejeitar e responder pelo WhatsApp usando o número da pendência (`ok 1`, `não 1: motivo`, `2: resposta`) | 5 |
| F-63 | Comandos `status`, `pendentes`, `pausar`, `retomar`, `vou usar` | 5 |
| F-64 | Aceitar comandos só de números autorizados | 5 |
| F-65 | Escolher quais avisos vão para cada canal | 5 |

## Home

| ID | Funcionalidade | Fase |
|---|---|---|
| F-90 | Botão Home (casinha) na barra superior | 3 |
| F-91 | Números com mini-gráfico: concluídas, pendências, falhas, horas de overtime | 3 |
| F-92 | Mapa de atividade do último ano (estilo contribuições do GitHub) | 3 |
| F-93 | Resumo da última sessão de overtime (horário, consumo, Tasks) | 3 |
| F-94 | "Precisa de você" com as pendências de todos os projetos | 3 |
| F-95 | Gráfico das últimas 14 noites | 3 |

## Configuração

| ID | Funcionalidade | Fase |
|---|---|---|
| F-70 | Pasta de dados, intervalo, tentativas e timeout padrão | 1 |
| F-73 | Tela de configurações com navegação lateral: Geral, Agendamento, Provedores, Notificações, Projetos padrão, Avançado | 3 |
| F-74 | Horário de funcionamento por dia da semana: barras verticais de 48 segmentos (30 min), seleção por clique e arraste, mais atalhos | 3 |
| F-75 | Reserva para você por provedor (controle deslizante) | 3 |
| F-71 | Configuração de provedores (habilitar, caminho do CLI, tetos, prioridade) | 3 |
| F-72 | Configuração de canais (WhatsApp, números autorizados) | 5 |

## RPC

| ID | Funcionalidade | Fase |
|---|---|---|
| F-80 | Servidor JSON-RPC local com token e papéis `DONO` / `AGENTE` | 2 |
| F-81 | `tarefa.recarregar`: recarregar uma Task editada fora da app | 2 |
| F-82 | `subtarefa.adicionar` e `subtarefa.atualizar` pelo agente | 2 |
| F-83 | Comando `odctl` para chamar o RPC pelo terminal | 2 |
| F-84 | Variáveis de ambiente e instrução do `odctl` em cada execução | 2 |
| F-85 | Demais métodos: `tarefa.criar`, `execucao.anotar`, `execucao.solicitarVerificacao`, `agendador.status` | 3 |
| F-86 | Servidor MCP com os métodos do RPC, passado ao agente por `--mcp-config` | 2 |
