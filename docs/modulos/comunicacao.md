# Módulo `comunicacao`

Leva avisos da app até você e traz seus comandos de volta, por vários canais. A app não conhece o WhatsApp diretamente: ela fala com `CanalComunicacao`.

## Interface

```java
public interface CanalComunicacao {
    String nome();
    void enviar(Mensagem mensagem);
    void aoReceber(Consumer<ComandoRecebido> ouvinte);
    void iniciar();
    void encerrar();
}
```

| Implementação | Descrição |
|---|---|
| `CanalSwing` | Notificações dentro da própria app (painel de verificações, toasts). Sempre ativo. |
| `CanalWhatsApp` | Adaptador para o projeto de WhatsApp já existente no seu git. |

## Mensagens enviadas

| Tipo | Exemplo |
|---|---|
| Verificação solicitada | `#T-12.3 aguarda verificação — "Escrever teste do login". Resumo: ... Responda: aprovar T-12.3 / rejeitar T-12.3: motivo` |
| Task concluída | `✓ T-12 "Corrigir login" concluída (4 subtarefas, Claude Code).` |
| Falha | `✗ T-12.3 falhou 3 vezes. Último erro: ...` |
| Pausa por limite | `⏸ Codex atingiu o teto da janela. Retoma às 18:40.` |
| Agente não autenticado | `⚠ Gemini não está autenticado; provedor desabilitado.` |

O nível de detalhe por canal é configurável (ex.: WhatsApp só verificações e falhas).

## Comandos recebidos

O `RoteadorDeComandos` interpreta o texto e chama o `service`:

| Comando | Efeito |
|---|---|
| `aprovar <id>` | Subtarefa → `CONCLUIDA` |
| `rejeitar <id>: <motivo>` | Subtarefa → `A_FAZER` com o motivo no feedback |
| `status` | Resumo: agendador, execuções em andamento, pendências suas, orçamento por provedor |
| `pendentes` | Lista o que aguarda você |
| `pausar [provedor]` / `retomar [provedor]` | Controle do agendador |
| `vou usar <provedor> <horas>` | Reserva o provedor para uso manual |

Comandos inválidos recebem uma resposta de ajuda.

## Segurança

- **Lista de remetentes autorizados** (o seu número). Mensagens de qualquer outro número são ignoradas e registradas.
- Comandos que mudam estado exigem o ID exato da subtarefa.
- Nada de executar texto recebido como instrução para os agentes nesta fase: o WhatsApp aprova e rejeita, não cria Tasks. (Criar Tasks por mensagem pode ser avaliado depois, com confirmação.)

## Integração com o projeto de WhatsApp

> A definir depois de ler o projeto existente.

Possibilidades, dependendo de como ele foi construído:

| Se o projeto... | Integração |
|---|---|
| expõe uma API HTTP | `CanalWhatsApp` faz chamadas HTTP para enviar e recebe webhooks (ou consulta periodicamente) |
| é uma biblioteca Java | Dependência direta |
| é um processo separado (ex.: Node) | Comunicação por HTTP local ou fila simples; a app pode iniciá-lo como processo filho |

Requisitos para o projeto de WhatsApp: enviar texto para um número, entregar mensagens recebidas com o número do remetente, e manter a sessão autenticada sem intervenção frequente.
