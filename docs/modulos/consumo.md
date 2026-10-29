# Módulo `consumo`

Decide **se** a automação pode rodar agora e **em qual provedor**, garantindo que uma parte do limite de cada provedor fique reservada para o seu uso manual.

## Problema

Cada provedor (Claude, Codex, Gemini) tem seus próprios créditos e limites, normalmente por janela de tempo (ex.: algumas horas) e por semana ou mês. A automação deve aproveitar esses créditos sem nunca deixar você sem uso quando precisar.

> Não há garantia de que cada provedor ofereça uma forma oficial e estável de ler "quanto do limite já foi usado". Por isso o módulo funciona com **contabilidade própria** e trata a leitura oficial como um extra opcional.

## Interfaces

```java
public interface FonteDeUsage {
    ProvedorId provedor();
    UsoAtual usoAtual();                 // consumo na janela atual e no período longo
}

public interface PoliticaDeConsumo {
    Decisao podeExecutar(ProvedorId provedor, EstimativaConsumo estimativa);
}
```

| Implementação | Como obtém o uso |
|---|---|
| `ContabilidadeLocal` | Soma o `Consumo` de cada `Execucao` da própria app, por provedor e por janela. Sempre existe. |
| `FonteOficial<Provedor>` | (futuro) Lê o uso real, se o provedor expuser isso de forma confiável. |

`Decisao` = permitido, ou negado com motivo (`TETO_DA_JANELA`, `TETO_DO_PERIODO`, `FORA_DO_HORARIO`, `PAUSA_MANUAL`, `PROVEDOR_EM_LIMITE`, `USO_MANUAL_DETECTADO`).

## Configuração (por provedor)

| Campo | Exemplo | Significado |
|---|---|---|
| `habilitado` | `true` | Se a automação pode usar este provedor |
| `tetoJanela` | `60%` ou `400k tokens` | Quanto da janela curta a automação pode gastar |
| `duracaoJanela` | `5h` | Tamanho da janela curta |
| `tetoPeriodo` | `50%` | Quanto do período longo (semana/mês) a automação pode gastar |
| `limiteEstimado` | `1.000.000 tokens` | Referência para converter porcentagem em número, quando não há fonte oficial |
| `horarios` | `22:00–07:00` | Faixas em que a automação pode rodar (opcional) |
| `prioridade` | `1` | Ordem de preferência no modo `AUTOMATICO` |

Na tela (**Configurações › Agendamento**, decisão D-38):
- **Horário de funcionamento:** uma **barra vertical larga por dia da semana**, de 0h (topo) a 24h (base), dividida em **48 segmentos de 30 minutos**. Segmento verde = selecionado = horário válido para a execução automática. Clicar alterna um segmento; clicar e arrastar seleciona (ou desmarca) vários de uma vez, inclusive entre dias. Atalhos: "Sempre", "Noites e fins de semana" e "Personalizado". Vale para todos os provedores; é guardado como uma lista de 7 × 48 valores.
- **Reserva para você:** um controle deslizante por provedor; o valor é a parte que fica para você (ex.: 40% → a automação usa até 60% da janela).
- **Execução:** intervalo do agendador, tentativas e timeout.

## Regras da política

1. **Teto da janela e do período:** consumo atual + estimativa da próxima execução ≤ teto.
2. **Estimativa:** média do consumo das últimas execuções daquele provedor **com o mesmo modelo e nível de esforço** (ou um valor padrão por nível no começo). Esforço mais alto costuma consumir bem mais, então misturar os níveis distorceria a estimativa.
3. **Freio de emergência:** se uma execução voltar com `LIMITE_DE_USO`, o provedor fica bloqueado até o fim da janela (ou por um tempo configurável).
4. **Você primeiro:**
   - botão **"Vou usar agora"**: pausa a automação de um provedor (ou de todos) por X horas;
   - detecção opcional de uso manual (ex.: processo do CLI aberto fora da app) → não inicia novas execuções naquele provedor.
5. **Horários:** fora das faixas configuradas, nega.

## Escolha do provedor (modo `AUTOMATICO`)

Para uma Task com `provedor = AUTOMATICO` (ainda sem nenhuma execução):

1. Filtrar provedores habilitados, disponíveis e com `Decisao` permitida.
2. Ordenar por `prioridade` configurada; em empate, por maior folga de orçamento.
3. Gravar o escolhido em `provedor`, substituindo `AUTOMATICO`.

Se a Task já tem um provedor fixo e ele está sem orçamento, a Task **espera** — não troca de provedor sozinha (ver regra 6 do [domínio](../dominio.md)).

## Dados exibidos na tela

Por provedor: consumo na janela atual × teto, consumo no período × teto, tempo até a janela renovar, estado (`LIVRE`, `NO_TETO`, `EM_LIMITE`, `PAUSADO`).
