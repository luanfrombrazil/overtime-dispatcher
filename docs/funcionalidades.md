# Funcionalidades

Lista de funcionalidades por área. A coluna **Fase** remete ao [roadmap](roadmap.md).

## Tasks e subtarefas

| ID | Funcionalidade | Fase |
|---|---|---|
| F-01 | Criar, editar e remover Task (título, descrição, prioridade) | 1 |
| F-02 | Vincular Task a um Repositório ou deixar sem vínculo | 1 |
| F-03 | Adicionar, editar, remover e reordenar subtarefas | 1 |
| F-04 | Definir instrução, critério de aceite e "requer verificação" por subtarefa | 1 |
| F-05 | Filtrar e ordenar Tasks por status, prioridade, repositório e provedor | 1 |
| F-06 | Pausar e retomar uma Task | 3 |
| F-07 | Cancelar e reabrir subtarefa | 2 |
| F-08 | Sugerir subtarefas com um agente a partir da descrição da Task | 6 |
| F-09 | Duplicar Task (modelo para tarefas recorrentes) | 6 |

## Repositórios

| ID | Funcionalidade | Fase |
|---|---|---|
| F-10 | Cadastrar, editar e remover Repositório (nome, pasta, branch padrão) | 1 |
| F-11 | Definir permissões padrão por repositório | 2 |
| F-12 | Validar se a pasta existe e se é um repositório git | 1 |

## Execução

| ID | Funcionalidade | Fase |
|---|---|---|
| F-20 | Executar uma subtarefa manualmente ("Executar agora") | 2 |
| F-21 | Ver a saída do agente ao vivo | 2 |
| F-22 | Cancelar execução em andamento | 2 |
| F-23 | Histórico de execuções por subtarefa, com log, consumo e resultado | 2 |
| F-24 | Sessão compartilhada entre subtarefas da mesma Task | 2 |
| F-25 | Novas tentativas automáticas até o limite | 3 |
| F-26 | Recuperar execuções interrompidas ao reabrir a app | 3 |

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
| F-60 | Notificações dentro da app | 4 |
| F-61 | Enviar avisos pelo WhatsApp | 5 |
| F-62 | Aprovar e rejeitar pelo WhatsApp | 5 |
| F-63 | Comandos `status`, `pendentes`, `pausar`, `retomar`, `vou usar` | 5 |
| F-64 | Aceitar comandos só de números autorizados | 5 |
| F-65 | Escolher quais avisos vão para cada canal | 5 |

## Configuração

| ID | Funcionalidade | Fase |
|---|---|---|
| F-70 | Pasta de dados, intervalo, tentativas e timeout padrão | 1 |
| F-71 | Configuração de provedores (habilitar, caminho do CLI, tetos, prioridade) | 3 |
| F-72 | Configuração de canais (WhatsApp, números autorizados) | 5 |
