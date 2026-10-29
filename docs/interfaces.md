# Interfaces

Arquivo no Figma: [OvertimeDispatcher](https://www.figma.com/design/0BqwZD0E88qRrWcxHGRb49/OvertimeDispatcher). Cada frame tem o **mesmo nome** da classe Swing correspondente ([módulo ui](modulos/ui.md)).

## Páginas do Figma

| Página | Conteúdo |
|---|---|
| `Fundamentos` | Variáveis de cor (modo único `Escuro`), cores de atividade, espaçamentos, estilos de texto |
| `Componentes` | `ItemStatus`, `LinhaProjeto`, `PrioridadeBadge`, `EsforcoBadge`, `ProvedorBadge`, `Botao` |
| `Telas` | `MainFrame`, `HomeFrame`, `ProtocoloFrame`, `TaskDialog`, `SubtarefaDialog`, `ProjetoDialog`, `ProtocoloDialog`, `VerificacaoPanel`, `ConfiguracoesDialog` (Agendamento e Notificações) |

O plano gratuito do Figma permite só 3 páginas (decisão D-24).

## Princípios

- **Só tema escuro** (FlatDarkLaf, D-25), componentes padrão do FlatLaf, cores sólidas (D-14).
- Fontes: **Inter** na interface, **JetBrains Mono** no console.
- **Tudo cabe em 1280 × 800**, sem rolagem na janela principal (D-30).
- **Texto mínimo** (D-29): só quando estritamente necessário, o mais curto possível.
- **Sem IDs** (D-28): nomes e, se preciso, `Projeto › Task › Subtarefa`.

## Estado visual: borda colorida

Projetos, Tasks e subtarefas mostram o estado por uma **borda colorida de 6px à esquerda** (D-27). Não há badges de status.

| Estado | Cor | Texto |
|---|---|---|
| Concluído | verde | nenhum |
| Em andamento | azul | nenhum |
| Pendente (na fila) | cinza | nenhum |
| Atenção (precisa de você) | amarelo | o motivo, curtíssimo: `1 verificação`, `Direcionamento`, `Merge` |
| Erro | vermelho | `Falhou` |

O estado de um projeto ou Task é o mais grave entre os seus itens: Erro > Atenção > Em andamento > Pendente > Concluído.

## Janela principal (`MainFrame`)

```
┌──────────────────────────────────────────────────────────────────────────┐
│ [⌂] [▶ Iniciar agendador] Pausado · 5 min    Claude ▓▓░ Codex ▓░ Gemini ▓░ [🔔3] [⚙] │
├────────────────────┬─────────────────────────────────────────────────────┤
│ Projetos  [+Projeto]│ Meu App                [Automático] [Editar] [+ Task] │
│ [Buscar projeto…]   │ C:\code\meu-app · main                              │
│▌MA Meu App          │ ┌─────────────────────────────────────────────┐    │
│▌   1 verificação    │ │▌Corrigir login com e-mail em maiúsculas    ▾│    │
│▌AP API de Pagamentos│ │▌1 verificação                               │    │
│▌JU Migração JUnit 5 │ │   ▌Reproduzir o bug com um teste            │    │
│▌   Direcionamento   │ │   ▌Normalizar o e-mail no login             │    │
│▌BP Backup Pessoal   │ │   ▌Verificação                              │    │
│▌   Falhou           │ │   ▌Atualizar a documentação                 │    │
│▌LP Landing Page     │ │ [+ Subtarefa] [Editar]   [Revisar verificação]│   │
│▌PC Pesquisa Crawlers│ └─────────────────────────────────────────────┘    │
│                     │ ▌Tela de recuperação de senha                  ▸   │
│                     │ ▌Remover dependências antigas · Merge          ▸   │
│                     │ ┌ Log · Histórico · Pendências (1) ────────────┐   │
│                     │ │ 16:40 Corrigir login › Normalizar · Claude    │   │
│                     │ └──────────────────────────────────────────────┘   │
└────────────────────┴─────────────────────────────────────────────────────┘
```

- **Barra superior:** Home (só a casinha), iniciar/pausar o agendador, consumo por provedor, notificações (sino com contador) e configurações.
- **Barra da esquerda:** duas seções, **Projetos** e **Protocolos**, cada uma com o seu botão "+". Todas as linhas têm a mesma altura (52px), com ícone de iniciais, nome e motivo opcional.
- **Área central:** o projeto selecionado, com Tasks em cartões. A Task expandida mostra as subtarefas e as ações. Prioridade e esforço **não aparecem no cartão**; ficam no diálogo de edição.
- **Console do projeto:** embaixo do projeto, com Log, Histórico e Pendências **só daquele projeto**.
- **Dependências amarradas com fio:** cada subtarefa tem um **círculo** à esquerda. Arrastar de um círculo até o de outra subtarefa as **amarra**: um **fio vermelho** liga os dois círculos, que ficam vermelhos. A de baixo depende da de cima (só roda depois que ela termina). Uma subtarefa pode ter vários fios. Clicar num fio oferece **Desamarrar**. Ao reordenar, os fios acompanham; se a ordem ficar invertida, a app avisa e não deixa soltar.
- Sem botões de seta: reordenar é por **arrastar e soltar**. Sem "Executar agora" no cartão: fica no **menu de contexto** da subtarefa.

## Home (`HomeFrame`)

Aberta pelo botão da casinha. Tudo em 1280 × 800:

- **4 números** com mini-gráfico (linha com pontos sobre fundo quadriculado): concluídas em 7 dias, pendências, falhas e horas de overtime.
- **Mapa de atividade**, no estilo das contribuições do GitHub: subtarefas concluídas por dia no último ano, com a legenda "Menos → Mais" no cabeçalho.
- **Última sessão:** horário e duração, consumo por provedor e as 4 Tasks mais relevantes (`Projeto › Task`), com "+N concluídas" para o resto.
- **Precisa de você:** pendências de todos os projetos.
- **Últimas 14 noites:** gráfico de linha com pontos.

## Protocolo (`ProtocoloFrame`)

Aberto ao clicar num protocolo na barra da esquerda ([conceito](modulos/protocolos.md)).

- **Cabeçalho:** nome, provedor, esforço, chave Ativo/Desativado, Editar e **Rodar agora**. Embaixo, os horários e a próxima execução (`07:00 · 19:00 · próxima às 19:00`).
- **Passo a passo:** o texto que o agente segue.
- **Acessos:** o que o protocolo pode usar (ex.: `Calendário · agenda Overtime`) e **"Aceita direto de"**: as origens que podem injetar tasks sem passar por você.
- **Fila de entrada:** tasks injetadas por outros protocolos, esperando a próxima execução, com a origem embaixo do título.
- **Propostas:** tasks de origens não confiáveis; Aceitar, Editar ou Descartar.
- **Console:** Última execução (o que foi feito, com `+` para criado e `~` para alterado) e Histórico.

## Diálogos e painéis

| Tela | Conteúdo |
|---|---|
| `ProjetoDialog` | Ícone (iniciais) + nome, descrição, **pasta do projeto** (obrigatória, com "Procurar…"), "Usar git" + branch padrão, padrões das tasks (provedor, esforço), permissões do agente (editar arquivos, rodar testes e build, instalar dependências, acessar a internet) |
| `ProtocoloDialog` | Ícone + nome, passo a passo, **Quando** (horários fixos com chips de horário, ou intervalo), **Acessos** (Sites com URL e "Fazer login"; E-mail: leitura e rascunhos; Calendário: só a agenda do agente), **Execução** (provedor · modelo, esforço, avisar: só novidades / sempre / nunca), **Aceitar tasks direto de** |
| `TaskDialog` | Título, descrição, prioridade, esforço, "Provedor · modelo" (ex.: Claude Code · Sonnet), permissões (herdar do projeto ou personalizar) |
| `SubtarefaDialog` | Título, instrução, critério de aceite, requer verificação (as dependências são feitas amarrando no cartão, não aqui) |
| `ConfiguracoesDialog` | Navegação lateral: Geral, Agendamento, Provedores, Notificações, Projetos padrão, Avançado. **Agendamento:** horário de funcionamento (uma barra vertical larga por dia, 0h–24h, com 48 segmentos de 30 min; clicar e arrastar seleciona; verde = horário válido; + atalhos), reserva para você por provedor (controle deslizante), intervalo, tentativas, timeout. **Notificações:** modo silencioso (agora, faixa automática, falhas furam o silêncio), canais (Desktop, WhatsApp, Som), tipos de aviso |
| `VerificacaoPanel` | Cartões com borda amarela, `Projeto › Task`, o motivo (`Verificação` ou `Direcionamento`); verificação: Ver log, Ver diff, Rejeitar, Aprovar; direcionamento: pergunta, resposta, Responder |

## A desenhar

- **Ícone do app** (`.ico` e PNGs de 16 a 256 px) e **ícones da bandeja** por estado: normal, executando, precisa de você (D-43).
- `ResumoAusenciaDialog`: pop-up ao voltar da bandeja, "Enquanto você estava fora" + período; blocos com borda de estado: precisa de você (com ação direta), falhas, concluídas (só contagem e lista curta); botão "Ver pendências".
- Notificação do desktop (Windows): título curto + `Projeto › Task · motivo`; clique abre a pendência.
- `NotificacoesPopup`: lista suspensa do sino, itens com borda de estado e texto mínimo (ex.: `Meu App · 1 verificação`).
- `MergePanel`: revisão do diff de uma Task antes do merge.
- `ConsumoPanel`: orçamento por provedor, com "Vou usar agora".
- `ConfiguracoesDialog`: abas Geral, Provedores, Projetos padrão e Avançado.
- Ajustar no Figma: campo "Provedor · modelo" no `TaskDialog`.
