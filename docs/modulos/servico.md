# Módulo `service`

Casos de uso da aplicação. É a **única** porta de entrada para a UI e para os canais de comunicação.

## Responsabilidades

- Validar e aplicar mudanças no domínio.
- Persistir pelo store.
- Publicar eventos (ver [arquitetura](../arquitetura.md#eventos)).
- Coordenar agendador, agentes, consumo e canais.

## Serviços

| Serviço | Operações principais |
|---|---|
| `TaskService` | criar, editar, remover, pausar/retomar Task; adicionar, editar, reordenar, cancelar, reabrir Subtarefa |
| `VerificacaoService` | listar pendências, aprovar, rejeitar (com motivo) |
| `RepositorioService` | cadastrar, editar, remover Repositório (não remove se houver Task vinculada) |
| `ExecucaoService` | executar agora, cancelar, consultar histórico e logs |
| `AgendadorService` | iniciar, pausar, pausar provedor, estado atual |
| `ConsumoService` | uso por provedor, "vou usar agora", configuração de tetos |
| `ConfigService` | ler e salvar configurações gerais, provedores e canais |

Podem começar como uma única classe `TaskService` e ser separados quando crescerem.

## Eventos

```java
public interface OuvinteDeEventos { void aoOcorrer(Evento evento); }
```

- `Evento` é uma `sealed interface` com um `record` por tipo de evento.
- O `service` nunca chama a UI diretamente; a UI se inscreve como ouvinte.
- Os canais de comunicação também são ouvintes (ex.: `VerificacaoSolicitada` → mensagem no WhatsApp).

## Futuro: quebrar Task em subtarefas

`TaskService.sugerirSubtarefas(taskId)` pede a um agente que leia a descrição (e o repositório, se houver) e devolva subtarefas em JSON. O resultado é **sugestão**: aparece para você revisar antes de entrar na Task.
