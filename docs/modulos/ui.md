# Módulo `ui` (Swing)

Telas da aplicação, construídas com Swing e `JPanel`s pequenos e reaproveitáveis. O visual é definido no Figma ([interfaces](../interfaces.md)); este documento define a **estrutura em classes**.

## Regras

1. Cada painel é uma classe própria, recebe o que precisa pelo construtor e não cria serviços.
2. Painéis não conversam entre si diretamente: a `MainFrame` liga a navegação (Home, projeto selecionado) aos painéis.
3. Toda chamada ao `service` que possa demorar roda em `SwingWorker`.
4. Eventos do `service` chegam por `SwingUtilities.invokeLater`.
5. Cores, fontes e espaçamentos ficam numa classe `Tema`, espelhando as variáveis e estilos do Figma.
6. Look & feel **FlatLaf**, apenas no tema escuro (`FlatDarkLaf`; decisões D-13 e D-25). Ajustes globais vão em propriedades do FlatLaf, não em cada componente.
7. Visual o mais simples possível (decisão D-14): componentes padrão do FlatLaf, cores sólidas, sem desfoque nem transparências.
8. **Texto mínimo** (decisão D-29): só aparece texto quando é estritamente necessário, e o mais curto possível.
9. **Sem IDs na tela** (decisão D-28): mostrar nomes; quando precisar de caminho, `Projeto › Task › Subtarefa`.
10. **Estado por borda colorida** (decisão D-27), usando sempre `ItemStatus` ou `LinhaProjeto`; nada de badges de status.

## Estrutura

```
ui
├─ MainFrame                       janela; troca o painel central entre Home e Projeto
├─ tema/Tema                       cores, fontes, espaçamentos, ícones
├─ comum/                          componentes reutilizáveis
│   ├─ ItemStatus                  item com borda de estado + título + detalhe opcional
│   ├─ BotaoIcone                  botão quadrado só com ícone (Home, notificações, configurações)
│   ├─ ContadorBadge               contador vermelho sobre um ícone
│   ├─ ProvedorBadge               rótulo do provedor
│   ├─ BarraDeConsumo              barra de uso × teto
│   └─ GraficoLinha                linha com pontos sobre fundo quadriculado
├─ topo/
│   └─ BarraSuperior               Home · iniciar/pausar agendador · consumo · notificações · configurações
├─ projeto/
│   ├─ ProjetoListPanel            barra da esquerda: lista de projetos
│   ├─ LinhaProjeto                linha de altura fixa: borda de estado, ícone, nome, motivo
│   ├─ ProjetoDetailPanel          cabeçalho do projeto + cartões de Task + console
│   ├─ TaskCard                    Task com borda de estado; expandida mostra subtarefas e ações
│   ├─ ProjetoLogPanel             console do projeto: Log · Histórico · Pendências
│   └─ ProjetoDialog               criar/editar projeto
├─ task/
│   ├─ TaskDialog                  criar/editar Task
│   └─ SubtarefaDialog             criar/editar subtarefa
├─ home/
│   ├─ HomePanel                   painel central da Home
│   ├─ EstatisticaCard             número + legenda + GraficoLinha
│   ├─ MapaAtividade               grade estilo "contributions" do GitHub
│   ├─ UltimaSessaoPanel           resumo da última sessão de overtime
│   └─ PrecisaDeVocePanel          pendências de todos os projetos
├─ pendencias/
│   ├─ VerificacaoPanel            verificações e direcionamentos
│   └─ MergePanel                  revisão do diff antes do merge
├─ notificacoes/
│   ├─ NotificacoesPopup           lista suspensa aberta pelo sino
│   └─ ResumoAusenciaDialog        "enquanto você estava fora": aparece ao voltar da bandeja
├─ bandeja/
│   └─ IconeBandeja                TrayIcon: estado geral, menu Abrir / Pausar / Sair, notificações do desktop
├─ consumo/
│   └─ ConsumoPanel                orçamento por provedor
└─ config/
    └─ ConfiguracoesDialog         geral, provedores, comunicação
```

## Interações

| Ação | Como |
|---|---|
| Ir para a Home | Botão da casinha na `BarraSuperior` |
| Abrir projeto | Clique em `LinhaProjeto` |
| Expandir Task | Clique no `TaskCard` |
| Reordenar Tasks e subtarefas | **Arrastar e soltar** (sem botões de seta) |
| Executar uma subtarefa agora | Menu de contexto (botão direito) na subtarefa |
| Ver notificações | Sino na `BarraSuperior`; o contador mostra as não lidas |
| Fechar a janela | Vai para a bandeja; a app continua trabalhando |
| Voltar da bandeja | Duplo clique no ícone ou clique numa notificação; abre o `ResumoAusenciaDialog` se algo aconteceu |

## Ligação com o Figma

- Cada frame do Figma corresponde a uma classe acima, com o mesmo nome.
- Componentes do Figma (`ItemStatus`, `LinhaProjeto`, badges, `Botao`) correspondem às classes em `comum/` e `projeto/`.
- Variáveis de cor (modo `Escuro`) e estilos de texto viram constantes em `Tema`.
