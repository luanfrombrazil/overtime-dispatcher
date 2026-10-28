# Roadmap

Cada fase entrega algo utilizável. Os IDs remetem a [funcionalidades](funcionalidades.md).

## Fase 0 — Documentação e design ⬅ atual

- Documentação de módulos e funcionalidades (este repositório).
- Modelagem das telas no Figma ([interfaces](interfaces.md)).
- Fechar as decisões em aberto da fase 1 ([decisões](decisoes.md)).

## Fase 1 — Núcleo e cadastro

**Entrega:** app abre, cadastra Tasks, Subtarefas e Repositórios, e salva em disco.

- Projeto Java configurado, pacotes criados.
- `domain` completo, com as transições de status testadas.
- `persistence` em JSON.
- UI: `MainFrame`, lista e detalhe de Task, tabela de subtarefas, cadastro de repositórios.
- F-01 a F-05, F-10, F-12, F-70.

## Fase 2 — Execução manual (Claude Code)

**Entrega:** você escolhe uma subtarefa, clica em "Executar agora" e acompanha o Claude trabalhando.

- Interface `Agente`, `ProcessoAgente`, `PromptBuilder`.
- Adaptador Claude Code.
- Log ao vivo, histórico, cancelamento.
- F-07, F-11, F-20 a F-24, F-30, F-34.

## Fase 3 — Automação com orçamento

**Entrega:** o agendador roda sozinho respeitando tetos e a sua reserva.

- Agendador, `ContabilidadeLocal`, `PoliticaDeConsumo`.
- Painel de consumo e barra do agendador.
- F-06, F-25, F-26, F-40 a F-47, F-71.

## Fase 4 — Verificação manual e multiagente

**Entrega:** subtarefas que exigem conferência param e esperam você na tela; Codex e Gemini entram como provedores.

- `VerificacaoPanel`, aprovar/rejeitar, diff.
- Adaptadores Codex e Gemini; modo automático de escolha de provedor.
- F-31 a F-33, F-50 a F-53, F-60.

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
