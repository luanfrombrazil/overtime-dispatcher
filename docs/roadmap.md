# Roadmap

Cada fase entrega algo utilizável. Os IDs remetem a [funcionalidades](funcionalidades.md).

## Fase 0 — Documentação e design ⬅ atual

- Documentação de módulos e funcionalidades (este repositório).
- Modelagem das telas no Figma ([interfaces](interfaces.md)).
- Fechar as decisões em aberto da fase 1 ([decisões](decisoes.md)).

## Fase 1 — Núcleo e cadastro

**Entrega:** app abre, cadastra Projetos, Tasks e Subtarefas, e salva em disco.

- Projeto Java configurado, pacotes criados.
- `domain` completo, com as transições de status testadas.
- `persistence` em JSON (pasta por projeto e por Task).
- UI: `MainFrame` com barra superior, barra de projetos, `ProjetoDetailPanel` com cartões de Task; `ProjetoDialog`, `TaskDialog`, `SubtarefaDialog`.
- F-01, F-03 a F-05, F-10 a F-17, F-70.

## Fase 2 — Execução manual (Claude Code)

**Entrega:** você executa uma subtarefa pelo menu de contexto e acompanha o Claude no console do projeto.

- Interface `Agente`, `ProcessoAgente`, `PromptBuilder`.
- Adaptador Claude Code; branch `overtime/…` e worktree por Task.
- Console do projeto (log, histórico), cancelamento.
- RPC básico e `odctl`, para o agente poder atualizar a fila durante a execução.
- F-07, F-18, F-20 a F-24, F-27, F-30, F-34, F-80 a F-84, F-86 (MCP).

## Fase 3 — Automação, bandeja e Home

**Entrega:** o agendador roda sozinho, com a janela fechada na bandeja, respeitando tetos e a sua reserva; a Home mostra o que aconteceu.

- Agendador, `ContabilidadeLocal`, `PoliticaDeConsumo`.
- Barra superior completa (consumo, notificações), `ConsumoPanel`.
- Bandeja do Windows, notificações no desktop, "Enquanto você estava fora".
- Home: números, mapa de atividade, última sessão, precisa de você, últimas 14 noites.
- F-06, F-25, F-26, F-40 a F-47, F-65A, F-66 a F-69, F-71, F-73 a F-75, F-85, F-90 a F-95.

> A partir da fase 3, cada fase também gera o `.exe` com Java embutido (D-43), já com o ícone próprio, para uso no dia a dia.

## Fase 4 — Verificação manual e multiagente

**Entrega:** subtarefas que exigem conferência param e esperam você na tela; Codex e Gemini entram como provedores.

- `VerificacaoPanel` (verificação e direcionamento), `MergePanel` com diff e merge.
- Adaptadores Codex e Gemini; modo automático de escolha de provedor.
- F-28, F-29, F-31 a F-33, F-50 a F-53, F-60.
- **Protocolos:** crawling somente leitura, e-mail somente leitura e a agenda própria do agente (F-100 a F-107).

> Verificação e multiagente podem virar fases separadas se uma delas ficar grande.

## Fase 5 — WhatsApp

**Entrega:** avisos e aprovações pelo celular.

- `CanalWhatsApp` integrado ao projeto existente.
- `RoteadorDeComandos`.
- F-61 a F-65, F-72.

## Fase 6 — Extras

- F-08 sugerir subtarefas com um agente.
- F-09 duplicar Task.
- F-35 trocar provedor com resumo.
- F-48 detectar uso manual.
- Métricas de consumo ao longo do tempo; troca para SQLite se necessário.
