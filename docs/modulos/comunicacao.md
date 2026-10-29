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
| `CanalSwing` | Notificações dentro da própria app: o sino da barra superior, com contador de não lidas. Sempre ativo. |
| `CanalDesktop` | Notificações do Windows pelo ícone da bandeja (`java.awt.SystemTray` / `TrayIcon.displayMessage`). Ativo sempre que a janela estiver fechada ou minimizada. |
| `CanalWhatsApp` | Adaptador para o projeto de WhatsApp já existente no seu git. |

### Notificações no desktop

- Usadas para **alertar e pedir orientação** quando a janela está fechada: verificação, direcionamento, merge, falha, provedor sem login, limite atingido.
- Texto mínimo, no mesmo formato das outras mensagens (ex.: `Meu App › Normalizar o e-mail · verificação`).
- **Clicar na notificação** abre a janela direto na pendência (verificação, direcionamento ou merge), onde você responde.
- **Botões de ação** (decisão D-41): verificação → **Aprovar** / **Rejeitar**; direcionamento → **Responder** (campo de texto na própria notificação); merge → **Merge** / **Abrir**. Exige uma biblioteca nativa de toast do Windows (A-15); sem ela, a app cai para o `TrayIcon` do Java, só com clique.
- Para não incomodar: no máximo uma notificação por pendência, e falhas repetidas da mesma Task são agrupadas.

### Modo silencioso

Configurado em **Configurações › Notificações**:

| Opção | Efeito |
|---|---|
| Silenciar agora | Nenhuma notificação (desktop, WhatsApp, som) até você desligar |
| Silenciar automaticamente | Faixa diária (ex.: 23:00–07:00) sem notificações |
| Falhas furam o silêncio | Falhas avisam mesmo no modo silencioso |

Durante o silêncio, as pendências continuam sendo registradas e aparecem no sino e no "Enquanto você estava fora". O agendador **não** para: silêncio é só sobre avisos.

Também em Notificações: canais (Desktop, WhatsApp, Som) e quais tipos avisar (Verificação, Direcionamento, Merge, Falhas, Limite de uso, Sessão concluída).

## Mensagens enviadas

Sem IDs internos e com o texto mínimo (decisões D-28 e D-29). Cada pendência recebe um **número curto** (`[1]`, `[2]`...), que vale enquanto ela estiver aberta e é usado nos comandos.

| Tipo | Exemplo |
|---|---|
| Verificação | `[1] Meu App › Normalizar o e-mail · verificação` + resumo do agente |
| Direcionamento | `[2] Migração JUnit 5 › Migrar testes · direcionamento` + a pergunta |
| Merge | `[3] Meu App › Remover dependências antigas · merge` |
| Falha | `Backup Pessoal › Script de backup · falhou` |
| Tudo esperando você | `3 pendências: [1] verificação, [2] direcionamento, [3] merge` |
| Pausa por limite | `Codex no teto · volta 18:40` |
| Agente não autenticado | `Gemini sem login · desabilitado` |

O que vai para cada canal é configurável (ex.: WhatsApp só pendências e falhas). Concluídas não geram mensagem; aparecem no resumo da sessão.

## Comandos recebidos

O `RoteadorDeComandos` interpreta o texto e chama o `service`:

| Comando | Efeito |
|---|---|
| `ok <n>` | Aprova a pendência `n`: verificação → subtarefa concluída; merge → merge feito |
| `não <n>: <motivo>` | Rejeita a pendência `n`: verificação → volta para a fila com o motivo; merge → nova subtarefa com o motivo |
| `<n>: <texto>` | Responde o direcionamento `n`; a subtarefa volta para a fila |
| `status` | Agendador, execuções em andamento, pendências e consumo por provedor |
| `pendentes` | Lista numerada do que aguarda você |
| `pausar [provedor]` / `retomar [provedor]` | Controle do agendador |
| `vou usar <provedor> <horas>` | Reserva o provedor para uso manual |

Se o WhatsApp permitir responder citando uma mensagem, a resposta citada vale como se tivesse o número dela (ex.: citar a mensagem `[1]` e escrever `ok`). Comandos inválidos recebem uma resposta de ajuda.

## Segurança

- **Lista de remetentes autorizados** (o seu número). Mensagens de qualquer outro número são ignoradas e registradas.
- Comandos que mudam estado exigem o número de uma pendência **aberta**; números de pendências já resolvidas são recusados.
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
