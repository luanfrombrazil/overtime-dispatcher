# Registro de decisões

Decisões técnicas e de arquitetura do projeto. Regras de funcionamento estão nos documentos de cada [módulo](modulos/); propostas não confirmadas estão em [ideias em aberto](ideias-em-aberto.md).

## Decisões vigentes

| Código | Decisão | Data |
|---|---|---|
| D-01 | A aplicação é desktop, para Windows, desenvolvida em Java. | 28/10/2026 |
| D-02 | A arquitetura segue o padrão MVC (Model, View, Controller). | 31/10/2026 |
| D-03 | A interface é construída em Swing com o look and feel FlatLaf, exclusivamente no tema escuro. | 28/10/2026 |
| D-04 | O desenho das telas é feito no Figma antes da implementação. | 28/10/2026 |
| D-05 | Versão da linguagem: Java 25 (LTS). Ferramenta de build: Maven. | 28/10/2026 |
| D-06 | Pacote raiz: `io.github.luanfrombrazil.overtimedispatcher`. | 28/10/2026 |
| D-07 | Os dados da aplicação são persistidos em arquivos JSON, com a biblioteca Jackson, numa pasta central da aplicação. | 28/10/2026 |
| D-08 | A distribuição final é um executável `.exe` com o runtime Java embutido e reduzido (`jpackage` e `jlink`). | 29/10/2026 |
| D-09 | A execução dos agentes é feita pelos CLIs dos provedores (Claude Code, Codex, Gemini), atrás de uma interface comum. | 28/10/2026 |
| D-10 | As subtarefas de uma mesma Task compartilham a sessão do agente. | 28/10/2026 |
| D-11 | A aplicação expõe funções aos agentes por RPC e, desde a primeira versão, como servidor MCP. | 28/10/2026 |
| D-12 | Cada projeto aponta para uma pasta do sistema de arquivos; o uso de git é opcional. | 29/10/2026 |
| D-13 | Em projetos com git, cada Task é executada numa branch própria com o prefixo `overtime/`; o merge na branch principal ocorre somente após verificação humana. | 28/10/2026 |
| D-14 | Acessos dos Protocolos: sites por crawling, somente leitura; e-mail por conector, com leitura e criação de rascunhos; calendário por conector, restrito a uma agenda própria do agente. | 29/10/2026 |
| D-15 | As tarefas das Trilhas são armazenadas num vault do Obsidian; a aplicação implementa apenas a gestão dessas tarefas. | 29/10/2026 |
| D-16 | A aplicação permanece em execução na bandeja do sistema quando a janela é fechada. | 28/10/2026 |
| D-17 | As notificações do Windows possuem botões de ação. | 28/10/2026 |
| D-18 | A integração com o WhatsApp reutiliza um projeto existente do autor. | 28/10/2026 |

## Pendentes

| Código | Questão |
|---|---|
| P-01 | Forma de integração com o projeto de WhatsApp. |
| P-02 | Viabilidade de execução do Antigravity sem interface; alternativa: Gemini CLI. |
| P-03 | Opções de linha de comando do Codex e do Gemini. |
| P-04 | Biblioteca de notificações do Windows com botões de ação. |
