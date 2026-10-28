# Decisões

Registro curto de decisões. Ao fechar uma decisão em aberto, mova-a para **Tomadas** com a data e o motivo.

## Tomadas

| # | Decisão | Motivo |
|---|---|---|
| D-01 | Aplicação desktop em Java com Swing | Simples, sem servidor, roda local onde estão os CLIs e repositórios |
| D-02 | A app controla as execuções; "ocioso" = nenhuma execução da app em andamento naquele provedor | Confiável e simples; detectar uso externo fica como extra |
| D-03 | Tasks com subtarefas pequenas, executadas em ordem | Abordagem ágil; cada execução tem escopo e critério de aceite claros |
| D-04 | Task pode ou não ter Repositório; sem repositório usa `workspace/<id>/` | Permite tarefas que não são de código |
| D-05 | Prompt enviado por stdin | Evita problemas de aspas e caracteres especiais no Windows |
| D-06 | Persistência atrás de interface; começa em JSON, um arquivo por Task | Fácil de inspecionar; troca para SQLite sem afetar o resto |
| D-07 | UI só fala com o `service`; eventos para atualização | Modularidade e testes |
| D-08 | Orçamento com contabilidade própria, com fonte oficial opcional | Não há garantia de API de uso em cada provedor |
| D-09 | Comunicação por interface `CanalComunicacao`; WhatsApp é um adaptador para o projeto existente | Desacopla a app do WhatsApp |
| D-10 | Múltiplos provedores (Claude Code, Codex, Gemini) atrás da interface `Agente` | Aproveitar créditos de várias assinaturas |
| D-11 | Uma Task fica num único provedor do início ao fim | Sessões não são transferíveis entre agentes |
| D-12 | Interfaces modeladas no Figma antes da implementação da UI | Definir o visual antes de codar os painéis |

## Em aberto

| # | Pergunta | Opções | Necessária para |
|---|---|---|---|
| A-01 | Subtarefas compartilham a sessão do agente? | Mesma sessão (proposta) / sessão nova por subtarefa | Fase 2 |
| A-02 | Subtarefas só sequenciais ou algumas independentes? | Sequenciais (proposta) / com dependências | Fase 1 |
| A-03 | Biblioteca de JSON | Jackson / Gson / sem dependências | Fase 1 |
| A-04 | Build e versão do Java | Maven ou Gradle; Java 17 ou 21 | Fase 1 |
| A-05 | Nome raiz dos pacotes | ex.: `autoclaude`, `dev.luan.autoclaude` | Fase 1 |
| A-06 | Como definir a reserva | Porcentagem / horários / ambos | Fase 3 |
| A-07 | Integração com o projeto de WhatsApp | HTTP / biblioteca / processo separado — depende do projeto | Fase 5 |
| A-08 | Antigravity tem execução sem interface? Senão, usar Gemini CLI | Verificar | Fase 4 |
| A-09 | Comandos exatos de Codex e Gemini (JSON, retomada de sessão, permissões) | Verificar nas versões instaladas | Fase 4 |
| A-10 | O nome "auto-claude" continua, mesmo com outros provedores? | Manter / renomear | Antes de publicar |
| A-11 | Look & feel do Swing | Padrão / FlatLaf (moderno, combina melhor com um design do Figma) | Fase 1 |
