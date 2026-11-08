# Interfaces

O desenho das telas está no Figma: [OvertimeDispatcher](https://www.figma.com/design/0BqwZD0E88qRrWcxHGRb49/OvertimeDispatcher).

## Diretrizes

- Visual simples, com os componentes padrão do FlatLaf e cores sólidas.
- O estado de cada item é indicado por uma borda colorida à esquerda, sem rótulos de status.
- Textos apenas quando estritamente necessários, e o mais curtos possível.
- Identificadores internos (como "T-12") não são exibidos.
- Elementos relacionados ao Oráculo usam a cor roxa.
- As telas cabem em 1280 × 800.

## Janela principal

- **Barra lateral** (280 px, da altura da janela): no topo, Home, o botão do agendador (só o ícone de play; vermelho com o ícone de parar enquanto roda) e o botão de recolher, que deixa a barra com 56 px. Abaixo, o Oráculo e as listas de Projetos, Protocolos e Trilhas, com linhas de 44 px.
- **Barra superior**, só sobre a área central: o consumo de cada provedor, as notificações e as configurações. O sino abre as Configurações na seção Notificações.
- **Área central**: Home, Projeto, Protocolo ou Trilha.

## Páginas do Figma

| Página | Conteúdo |
|---|---|
| Fundamentos | Paleta e tipografia |
| Componentes | Botões, ícones (os SVGs de `src/main/resources/icones`), `BotaoIcone`, itens de status e demais peças |
| Telas | As telas como estão no código |
| Telas — ainda não no código | Desenhos que ainda não foram implementados (Oráculo em tela cheia e o pop-up de notificações) |
| Arquivo — telas até 05/11/2026 | Cópia das telas antes da atualização de 06/11/2026 |

## Telas

| Frame no Figma | Classe |
|---|---|
| HomeFrame | `HomePanel` |
| MainFrame, MainFrame — Sugestão e finalizadas | `ProjetoPanel`, `TaskCard`, `SubtarefasPanel` |
| TaskCard — Direcionamento | `DirecionamentoPanel` |
| ProtocoloFrame | `ProtocoloPanel` |
| TrilhaFrame | `TrilhaPanel` |
| ProjetoDialog — Novo projeto / Editar projeto | `ProjetoDialog` |
| TaskDialog — Nova task / Task finalizada | `TaskDialog` |
| SubtarefaDialog — Nova subtarefa / Aguardando verificação | `SubtarefaDialog` |
| ProtocoloDialog | `NovoProtocoloDialog` |
| OraculoPanel | `OraculoDialog` |
| ConfiguracoesDialog — Geral, Modelos, Notificações, Avançado | `ConfiguracoesDialog`, `ModelosPagina` |
| PreviaPromptDialog, PreviaLinhaDeComandoDialog | Prévias da seção Modelos |
