# Módulo `consumo`

Garante que a automação use os créditos das assinaturas sem gastar a parte reservada para você.

## Horário de funcionamento
Para cada dia da semana, uma barra de **48 segmentos de 30 minutos**. Segmento selecionado = horário em que a automação pode rodar. Seleciona-se com clique e arraste.

## Reserva
Por provedor, um controle com duas alças divide a janela de uso em três partes:

| Parte | Cor |
|---|---|
| Automação (Tasks e Protocolos) | azul |
| Oráculo | roxo |
| Você | cinza |

## Medição
As barras da barra superior mostram o uso da janela curta e da semana de cada provedor:

| Provedor | De onde vem |
|---|---|
| Claude | Endpoint de uso da Anthropic, com o login do Claude Code (`~/.claude/.credentials.json`) |
| Codex | Endpoint de uso do ChatGPT, com o login do Codex (`~/.codex/auth.json`); sem ele, o último `rate_limits` gravado em `~/.codex/sessions` |
| Gemini | Servidor local do Antigravity CLI (`agy`), achado pelo log em `~/.gemini/antigravity-cli/log`. Só mede com o `agy` aberto; fechado, repete a última medição. Mostra a cota do modelo selecionado no `agy` (o Antigravity não tem limite semanal separado) |
