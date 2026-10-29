# Trilhas

Tarefas **para você**, e não para os agentes: estudos, trabalhos da faculdade, projetos pessoais. Cada tarefa tem uma **duração planejada**, dividida em **blocos** de tempo que vão para a agenda (decisões D-56 a D-58).

Uma **trilha** agrupa tarefas do mesmo assunto, por exemplo "Trabalhos da faculdade", "Cálculo II" ou "TCC". As trilhas são a terceira seção da barra lateral, depois de Projetos (dos agentes) e de Protocolos.

## Vault do Obsidian

- As trilhas moram num **vault de verdade do Obsidian**: uma pasta de arquivos `.md` no seu computador.
- A app lê e escreve nesse vault, e você pode abrir a mesma pasta no Obsidian a qualquer momento ("Abrir no Obsidian").
- Como as notas, links e anotações ficam com o Obsidian, a app implementa **só a seção de tarefas**.
- Cada trilha é um arquivo, por exemplo `Vault/Trilhas/Trabalhos da faculdade.md`. As tarefas são linhas de checklist que o Obsidian entende, com metadados no próprio texto:

```markdown
- [ ] Trabalho de Redes ⏱ 6h 🧱 6×1h 📅 2026-11-12 ✅ 2/6
- [ ] Lista 4 de Cálculo ⏱ 3h 🧱 3×1h ✅ 1/3
- [x] Seminário de IA ⏱ 4h 🧱 4×1h ✅ 4/4
```

O formato exato dos metadados será definido para ser compatível com o plugin Tasks do Obsidian, sempre que possível.

## Tarefa

| Campo | Exemplo |
|---|---|
| Título | Trabalho de Redes |
| Duração planejada | 6h |
| Blocos | 6 blocos de 1h (a app sugere a divisão; você ajusta) |
| Prazo | 12/11 (opcional) |
| Andamento | blocos feitos / total (D-57) |
| Origem | você, um protocolo ou o Oráculo |

## Planejamento em blocos

1. Você (ou quem injetou a tarefa) define a duração e o prazo.
2. A app divide a tarefa em blocos e injeta cada bloco como task no protocolo **Agenda**, que o coloca na agenda do agente, nos horários livres antes do prazo.
3. Você marca cada bloco quando o termina. O progresso é `blocos feitos / total`.

Na tela, cada tarefa mostra uma fileira de blocos:

| Bloco | Significado |
|---|---|
| verde | feito |
| contorno azul | na agenda |
| cinza | a agendar |

Ao lado da fileira aparecem o andamento e o próximo bloco, por exemplo `2/6 · próximo hoje 19:00`.

## De onde vêm as tarefas

- **Você:** pelo botão "+ Tarefa" ou escrevendo direto no Obsidian.
- **Protocolos:** por exemplo, "Tarefas da faculdade" encontra um trabalho e injeta a tarefa na trilha "Trabalhos da faculdade" (injeção de tasks, D-48).
- **Oráculo:** sugere estudos (vídeos, leituras, exercícios) como tarefas em **roxo**, com o porquê embaixo e os botões Recusar e Aceitar (D-54, D-55).

## Estado visual da trilha

| Estado | Quando | Texto |
|---|---|---|
| Amarelo | Prazo próximo ou bloco atrasado | `Entrega 12/11` |
| Azul | Tem bloco hoje | nenhum |
| Cinza | Nada agendado | nenhum |
| Verde | Todas as tarefas concluídas | nenhum |

## Home como hub

A Home também agrega as trilhas (D-58). O cartão **Hoje** mostra os blocos do dia com o horário (`14:00 Cálculo II › Lista 4 · 1h`) e os próximos prazos. Além do consumo e da atividade dos agentes, a Home passa a ser o ponto de partida do seu dia.
