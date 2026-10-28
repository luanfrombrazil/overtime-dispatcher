# Interfaces (guia para o Figma)

Lista das telas e componentes a modelar. Cada frame do Figma deve ter o **mesmo nome** da classe Swing correspondente (ver [módulo ui](modulos/ui.md)).

## Sugestão de organização no Figma

| Página | Conteúdo |
|---|---|
| `Fundamentos` | Cores, tipografia, espaçamentos, ícones de status e de provedor |
| `Componentes` | Badges, barras de consumo, botões, linhas de tabela |
| `Telas` | Frames das telas abaixo |
| `Fluxos` | Protótipos de ponta a ponta (ex.: verificação) |

## Fundamentos

- **Status** (subtarefa e Task): uma cor e um ícone para cada — `A_FAZER`, `EM_ANDAMENTO`, `AGUARDANDO_VERIFICACAO`, `CONCLUIDA`, `FALHOU`, `CANCELADA`, `PAUSADA`, `RASCUNHO`.
- **Provedores:** identidade visual neutra por provedor (cor + sigla), sem usar logotipos oficiais.
- **Prioridade:** `BAIXA`, `MEDIA`, `ALTA`, `URGENTE`.
- Tema claro e escuro, se for usar FlatLaf (ver decisão A-11).
- Tamanho de referência da janela: 1280 × 800, mínimo 1024 × 640.

## Componentes

| Componente | Classe | Variações |
|---|---|---|
| Badge de status | `StatusBadge` | um por status |
| Badge de provedor | `ProvedorBadge` | um por provedor + `AUTOMATICO` |
| Barra de consumo | `BarraDeConsumo` | livre / perto do teto / no teto / em limite / pausado |
| Linha de Task | — | normal, selecionada, aguardando você |
| Linha de subtarefa | — | por status; com/sem "requer verificação" |
| Barra do agendador | `BarraAgendador` | rodando / pausado / sem orçamento |

## Telas

### 1. `MainFrame` — janela principal

```
┌────────────────────────────────────────────────────────────────────────────┐
│ BarraAgendador: [▶ Iniciar] intervalo 5 min · próximo ciclo 02:13          │
│                 Claude ▓▓▓░░ 52%   Codex ▓░░░░ 18%   Gemini ▓▓░░░ 31%   ⚙ │
├──────────────────┬─────────────────────────────────────────────────────────┤
│ TaskListPanel    │ TaskDetailPanel                                         │
│ [Buscar...]      │  T-12 · Corrigir login              [ALTA] [Claude]     │
│ Filtros ▾        │  Repositório: meu-app (main)                            │
│                  │  Descrição ...                                          │
│ ● T-12 Corrigir  │ ───────────────────────────────────────────────────     │
│ ◐ T-9  Docs API  │  SubtarefaTablePanel                                    │
│ ✓ T-7  Backup    │   # │ Subtarefa         │ Verif. │ Status    │ Tent.     │
│                  │   1 │ Reproduzir o bug  │        │ Concluída │ 1         │
│ [+ Nova Task]    │   2 │ Escrever o teste  │   ✓    │ Aguardando│ 1         │
│                  │   3 │ Corrigir          │        │ A fazer   │ 0         │
│                  │  [+] [✎] [−] [↑] [↓]   [▶ Executar agora]               │
├──────────────────┴─────────────────────────────────────────────────────────┤
│ Abas: [Log ao vivo] [Histórico] [Verificações (2)]                         │
└────────────────────────────────────────────────────────────────────────────┘
```

Estados a desenhar: sem nenhuma Task (vazio com convite para criar), Task selecionada, execução em andamento (log rolando).

### 2. `SubtarefaDialog` — criar/editar subtarefa

Campos: título, instrução (texto longo), critério de aceite (texto longo), requer verificação (checkbox), máximo de tentativas.

### 3. Nova/editar Task

Pode ser no próprio `TaskDetailPanel` (edição inline) ou num diálogo — decidir no Figma. Campos: título, descrição, prioridade, repositório (ou "Nenhum"), provedor (específico ou automático), permissões (herdar do repositório ou personalizar).

### 4. `VerificacaoPanel` — pendências

```
┌─ Verificações pendentes ──────────────────────────────────────────┐
│ T-12.2 Escrever o teste          Claude · há 14 min               │
│   Resumo do agente: ...                                           │
│   Critério de aceite: ...                                         │
│   [Ver log] [Ver diff]                                            │
│   Comentário: [_____________________________]                     │
│   [✓ Aprovar]  [✗ Rejeitar]                                       │
└───────────────────────────────────────────────────────────────────┘
```

### 5. `ConsumoPanel` — orçamento por provedor

Por provedor: barra da janela atual (uso × teto × limite estimado), barra do período, tempo até renovar, estado, botão **"Vou usar agora"** com duração.

### 6. `RepositoriosDialog`

Lista de repositórios + formulário: nome, pasta (com seletor), branch padrão, permissões padrão. Indicador de pasta inválida.

### 7. `ConfiguracoesDialog`

Abas:
- **Geral:** pasta de dados, intervalo do agendador, tentativas e timeout padrão.
- **Provedores:** por provedor — habilitado, caminho do CLI, status (instalado/autenticado), tetos, horários, prioridade.
- **Comunicação:** WhatsApp habilitado, números autorizados, quais avisos enviar.

## Fluxos para prototipar

1. Criar Task → adicionar 3 subtarefas → executar a primeira manualmente.
2. Agendador pega a subtarefa → termina → pede verificação → você rejeita com comentário → nova execução → aprova.
3. Provedor atinge o teto → agendador pausa aquele provedor → barra de consumo muda de estado.
