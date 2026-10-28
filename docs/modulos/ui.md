# Módulo `ui` (Swing)

Telas da aplicação, construídas com Swing e `JPanel`s pequenos e reaproveitáveis. O layout visual é definido no Figma; este documento define a **estrutura em classes**. A lista de telas está em [interfaces](../interfaces.md).

## Regras

1. Cada painel é uma classe própria, recebe o que precisa pelo construtor e não cria serviços.
2. Painéis não conversam entre si diretamente: a `MainFrame` (ou um controlador) liga a seleção de um painel ao outro.
3. Toda chamada ao `service` que possa demorar roda em `SwingWorker`.
4. Eventos do `service` chegam por `SwingUtilities.invokeLater`.
5. Tabelas usam `TableModel` próprio (ex.: `SubtarefaTableModel`), não `DefaultTableModel`.
6. Cores, fontes e espaçamentos ficam numa classe `Tema`, espelhando os estilos do Figma.

## Estrutura

```
ui
├─ MainFrame                      janela principal; monta os painéis
├─ tema/Tema                      cores, fontes, espaçamentos, ícones
├─ comum/                         componentes reutilizáveis
│   ├─ StatusBadge                rótulo colorido de status
│   ├─ ProvedorBadge              ícone/rótulo do provedor
│   └─ BarraDeConsumo             barra de uso × teto
├─ task/
│   ├─ TaskListPanel              lista e filtros de Tasks
│   ├─ TaskDetailPanel            dados da Task selecionada
│   ├─ SubtarefaTablePanel        tabela de subtarefas + ações
│   └─ SubtarefaDialog            criar/editar subtarefa
├─ execucao/
│   ├─ LogPanel                   saída ao vivo
│   └─ HistoricoExecucoesPanel    execuções de uma subtarefa
├─ verificacao/
│   └─ VerificacaoPanel           pendências: aprovar / rejeitar
├─ agendador/
│   └─ BarraAgendador             iniciar/pausar, estado, próximo ciclo
├─ consumo/
│   └─ ConsumoPanel               orçamento por provedor
├─ repositorio/
│   └─ RepositoriosDialog         cadastro de repositórios
└─ config/
    └─ ConfiguracoesDialog        provedores, tetos, canais, intervalo
```

## Ligação com o Figma

- Cada frame do Figma corresponde a uma classe acima, com o mesmo nome.
- Componentes do Figma (badge, barra de consumo, botões) correspondem às classes em `comum/`.
- Estilos de cor e texto do Figma viram constantes em `Tema`.
