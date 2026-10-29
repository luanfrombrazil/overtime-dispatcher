# Decisões

Registro curto de decisões. Ao fechar uma decisão em aberto, mova-a para **Tomadas** com a data e o motivo.

## Tomadas

| # | Decisão | Motivo |
|---|---|---|
| D-01 | Aplicação desktop em Java com Swing | Simples, sem servidor, roda local onde estão os CLIs e repositórios |
| D-02 | A app controla as execuções; "ocioso" = nenhuma execução da app em andamento naquele provedor | Confiável e simples; detectar uso externo fica como extra |
| D-03 | Tasks com subtarefas pequenas, executadas em ordem | Abordagem ágil; cada execução tem escopo e critério de aceite claros |
| D-04 | Task pode ou não ter Repositório; sem repositório usa `workspace/<id>/` | Permite tarefas que não são de código |
| D-05 | Prompt enviado por stdin | Evita problemas de aspas e caracteres especiais no Windows |
| D-06 | Persistência atrás de interface; começa em JSON (formato detalhado em D-16 e D-19) | Fácil de inspecionar; troca para SQLite sem afetar o resto |
| D-07 | UI só fala com o `service`; eventos para atualização | Modularidade e testes |
| D-08 | Orçamento com contabilidade própria, com fonte oficial opcional | Não há garantia de API de uso em cada provedor |
| D-09 | Comunicação por interface `CanalComunicacao`; WhatsApp é um adaptador para o projeto existente | Desacopla a app do WhatsApp |
| D-10 | Múltiplos provedores (Claude Code, Codex, Gemini) atrás da interface `Agente` | Aproveitar créditos de várias assinaturas |
| D-11 | Uma Task fica num único provedor do início ao fim | Sessões não são transferíveis entre agentes |
| D-12 | Interfaces modeladas no Figma antes da implementação da UI | Definir o visual antes de codar os painéis |
| D-13 | Look & feel **FlatLaf** sobre Swing (28/10/2026) | Visual moderno, temas claro/escuro e componentes customizáveis sem trocar de toolkit |
| D-14 | **Sem vidro fosco** nem efeitos de desfoque; visual o mais simples possível, com os componentes padrão do FlatLaf e cores sólidas (28/10/2026) | Simplicidade; o desfoque no Swing é frágil ou trabalhoso e não traz valor para o uso da app |
| D-15 | Nome do projeto: **Overtime Dispatcher** (`overtime-dispatcher`) (28/10/2026) | Não fica preso a um provedor; "overtime" remete a usar os créditos ociosos fora do seu horário e "dispatcher" ao despacho de tarefas para agentes, conexões e crawling |
| D-16 | Uma pasta por Task (`tasks/T-12/`), com as execuções em arquivos separados (28/10/2026) | O `task.json`, lido e gravado o tempo todo, fica pequeno; o histórico cresce à parte |
| D-17 | Tasks numa pasta de dados central, e não dentro de cada repositório (28/10/2026) | Tasks sem repositório existem; não mistura dados da app com o código dos projetos |
| D-18 | Campo de **esforço** por Task, em níveis neutros traduzidos por cada adaptador (28/10/2026) | Controlar qualidade × consumo de créditos por Task |
| D-19 | `task.json` enxuto: só o que o agendador e o prompt usam; o resto é derivado ou vem da configuração (28/10/2026) | Menos campos para manter em sincronia e arquivos fáceis de ler e editar |
| D-20 | RPC local (JSON-RPC 2.0 sobre HTTP em `127.0.0.1`) com o comando `odctl`; papéis `DONO` e `AGENTE` (28/10/2026) | Agentes conseguem atualizar a fila pela app, com as mesmas validações da UI, sem poder aprovar o próprio trabalho |
| D-21 | Cada Task com repositório trabalha na branch `overtime/<id>`, numa `git worktree` própria (28/10/2026) | Tasks isoladas entre si; a cópia do repositório do usuário nunca é alterada pelo agente |
| D-22 | Merge na branch padrão só depois da sua revisão do diff da Task inteira; squash e sem push automático (28/10/2026) | Controle humano sobre o que entra na main |
| D-23 | O agente pode pedir direcionamento (`AGUARDANDO_DIRECIONAMENTO`); Tasks paradas por você não bloqueiam a fila, e o agente segue para outra Task (28/10/2026) | Evita que o agente chute decisões importantes e mantém os créditos em uso |
| D-24 | Figma no plano gratuito: 3 páginas (Fundamentos, Componentes, Telas); fontes Inter e JetBrains Mono (28/10/2026) | Limite do plano; fontes disponíveis como pacotes do FlatLaf |
| D-25 | **Apenas o tema escuro** do FlatLaf (FlatDarkLaf); sem tema claro (28/10/2026) | Escolha do projeto; um tema só simplifica design e implementação |
| D-26 | **Projeto** como nível acima da Task: agrupa Tasks, guarda repositório, padrões e permissões; é a barra da esquerda (28/10/2026) | Organização por contexto; Task mais leve |
| D-27 | Estado mostrado por **borda colorida de 6px à esquerda** (verde, azul, amarelo, vermelho, cinza), sem badges de status (28/10/2026) | Leitura rápida e visual limpo |
| D-28 | **Sem IDs na interface** nem nas mensagens; nomes e `Projeto › Task › Subtarefa`; no WhatsApp, números curtos de pendência (28/10/2026) | "T-12" não diz nada para quem usa |
| D-29 | **Texto mínimo**: só em Atenção e Erro, o mais curto possível (`1 verificação`, `Falhou`) (28/10/2026) | Menos ruído; a cor já diz o resto |
| D-30 | Telas cabem em **1280 × 800** sem rolagem da janela; console por projeto; Home com estatísticas e mapa de atividade; Home e notificações como botões de ícone na barra superior (28/10/2026) | Tamanho de tela do usuário; contexto claro |
| D-31 | App roda na **bandeja do Windows** com a janela fechada; notificações no desktop para alertar e pedir orientação (28/10/2026) | Trabalha durante o dia sem ocupar a tela |
| D-32 | Ao voltar da bandeja, pop-up **"Enquanto você estava fora"** com o que aconteceu (28/10/2026) | Você se atualiza em segundos |
| D-33 | Subtarefas de uma Task **compartilham a sessão** do agente (antiga A-01) | Mais contexto e menos retrabalho |
| D-34 | Subtarefas **com dependências** (`dependeDe`); dentro da Task roda uma por vez, na mesma sessão e worktree, e o agendador pega a próxima cujas dependências estão concluídas (antiga A-02) | Uma subtarefa esperando você não trava as independentes |
| D-35 | **Jackson** para JSON (antiga A-03) | Padrão de mercado, suporta records e omissão de campos vazios |
| D-36 | **Maven + Java 25** (antiga A-04; revisada em 28/10/2026). Instalados na máquina: Temurin 25.0.5 LTS e Maven 3.10.0 | LTS atual; records, sealed interfaces, pattern matching, virtual threads e FFM (acesso nativo ao Windows sem JNA) |
| D-37 | Pacote raiz **`io.github.luanfrombrazil.overtimedispatcher`** (antiga A-05) | Baseado no GitHub do autor |
| D-38 | Reserva por **porcentagem + horários** por provedor (antiga A-06) | Teto de uso e faixas em que a automação pode rodar |
| D-39 | **MCP desde o início**: o servidor local expõe os mesmos métodos do RPC como servidor MCP, passado a cada execução por `--mcp-config`; o `odctl` continua para o terminal (antiga A-12) | Os agentes usam a fila como ferramenta nativa |
| D-40 | Campo combinado **"Provedor · modelo"** na Task, com padrão no projeto; "Padrão" deixa o CLI escolher; o consumo considera provedor, modelo e esforço (antiga A-13) | Controle de qualidade × custo por Task |
| D-41 | Notificações do Windows **com botões de ação** (Aprovar, Rejeitar, Responder), via biblioteca nativa de toast; o clique continua abrindo a pendência (antiga A-14) | Responder sem abrir a app |
| D-42 | Opções do **Claude Code confirmadas** na versão 2.1.308: `-p`, `--output-format json/stream-json`, `-r`, `--session-id`, `--permission-mode`, `--allowedTools`, `--model`, `--effort` (low, medium, high, xhigh, max), `--mcp-config` (parte da antiga A-09) | Adaptador da fase 2 pode ser implementado sem suposições |

## Em aberto

| # | Pergunta | Situação | Necessária para |
|---|---|---|---|
| A-07 | Integração com o projeto de WhatsApp | Adiada para a fase 5; depende do projeto existente | Fase 5 |
| A-08 | Antigravity tem execução sem interface? Senão, usar Gemini CLI | Não instalado nesta máquina; verificar ao instalar | Fase 4 |
| A-09 | Opções de Codex e Gemini (JSON, sessão, permissões, esforço, modelo) | Não instalados nesta máquina; verificar ao instalar | Fase 4 |
| A-15 | Qual biblioteca de toast do Windows (D-41) | Escolher e testar na implementação | Fase 3 |