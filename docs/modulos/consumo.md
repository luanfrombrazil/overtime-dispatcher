# Módulo `consumo`

Garante que a automação use os créditos das assinaturas sem gastar a parte reservada para você.

## Horário de funcionamento
Para cada dia da semana, uma barra de **48 segmentos de 30 minutos**. Segmento selecionado = horário em que a automação pode rodar. Seleciona-se com clique e arraste. Enquanto nada foi salvo, o padrão é o tempo todo (todos os dias, o dia inteiro).

## Limite de uso
Por provedor, um controle com duas alças divide a janela de uso em três partes:

| Parte | Cor | Padrão |
|---|---|---|
| Automação (Tasks e Protocolos) | azul | 90% |
| Oráculo | roxo | 5% |
| Você | cinza | 5% |

O agendador não começa uma Task num provedor cujo uso medido (o maior entre sessão e semanal) já chegou ao fim da parte azul; pula para Tasks de outros provedores e mostra na barra superior qual provedor está no limite. Uso que não dá para medir (ex.: Gemini) não impede. O uso é medido ao abrir o programa, quando uma Task começa e depois de 30 minutos ocioso; cada medição nova faz o agendador olhar a fila de novo.

## Em aberto
Como medir o consumo de cada provedor. Ver [ideias em aberto](../ideias-em-aberto.md).
