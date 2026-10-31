# Diagramas

Diagramas UML em [Mermaid](https://mermaid.js.org/). O GitHub os exibe diretamente.

## Diagrama de classes (Model)

Classes do domínio e a interface comum dos agentes. Atributos e operações aparecem só quando já foram definidos.

```mermaid
classDiagram
    direction LR

    class Projeto {
        nome
        pasta
        tipo : TipoProjeto
    }
    class Git {
        branchPrincipal
    }
    class Task {
        titulo
        esforco : Esforco
        modelo
        origem : Origem
    }
    class Subtarefa {
        instrucao
        criterioDeAceite
        requerVerificacao : boolean
    }
    class Protocolo {
        nome
        passos
        quando
    }
    class Acesso {
        <<abstract>>
    }
    class Site {
        url
        somenteLeitura
    }
    class Email {
        lerECriarRascunhos
    }
    class Calendario {
        agendaPropria
    }
    class Trilha {
        nome
    }
    class TarefaPessoal {
        titulo
        duracaoPlanejada
        prazo
        andamento()
    }
    class Bloco {
        duracao
        feito : boolean
    }
    class Oraculo {
        analisar(Projeto)
        sugerir()
    }
    class Sugestao {
        porque
        aceitar()
        recusar()
    }
    class Agente {
        <<interface>>
        executar(Subtarefa)
    }
    class ClaudeCode
    class Codex
    class Gemini

    class TipoProjeto {
        <<enumeration>>
        FINITO
        CONTINUO
    }
    class Origem {
        <<enumeration>>
        USUARIO
        PROTOCOLO
        ORACULO
    }
    class Esforco {
        <<enumeration>>
        LOW
        MEDIUM
        HIGH
        XHIGH
        MAX
    }

    Projeto "1" *-- "0..*" Task : contém
    Projeto "1" o-- "0..1" Git : usa
    Task "1" *-- "1..*" Subtarefa : dividida em
    Subtarefa "0..*" --> "0..*" Subtarefa : depende de
    Task "0..*" --> "1" Agente : executada por

    Protocolo "1" o-- "1..*" Acesso : usa
    Acesso <|-- Site
    Acesso <|-- Email
    Acesso <|-- Calendario
    Protocolo ..> Task : injeta
    Protocolo ..> TarefaPessoal : injeta

    Trilha "1" *-- "0..*" TarefaPessoal : contém
    TarefaPessoal "1" *-- "1..*" Bloco : planejada em

    Oraculo ..> Projeto : analisa
    Oraculo "1" --> "0..*" Sugestao : gera
    Sugestao ..> Task : vira

    Agente <|.. ClaudeCode
    Agente <|.. Codex
    Agente <|.. Gemini
```

## Diagrama de casos de uso

O Mermaid não tem um tipo nativo de casos de uso. A notação UML é representada com um fluxograma:
- **atores** fora da fronteira do sistema;
- **casos de uso** em formato oval;
- **associações** em linha contínua;
- `«include»` e `«extend»` em seta tracejada.

Um diagrama por área do sistema. O ator principal fica à esquerda e os atores secundários, à direita.

| Ator | Papel |
|---|---|
| **Usuário** | Quem usa a aplicação |
| **Tempo** | Dispara o que roda sozinho (horário de funcionamento, horários dos protocolos) |
| **Agente de IA** | Claude Code, Codex ou Gemini |
| **Serviços externos** | Portal da faculdade, e-mail, calendário, WhatsApp |

### Projetos e execução

```mermaid
flowchart LR
    usuario["👤 Usuário"]
    tempo["⏱ Tempo"]

    subgraph sistema["Overtime Dispatcher"]
        uc1(["Gerenciar projeto"])
        uc2(["Gerenciar tasks e subtarefas"])
        uc3(["Amarrar dependência"])
        uc4(["Verificar subtarefa"])
        uc5(["Revisar e fazer merge"])
        uc6(["Responder orientação"])
        uc7(["Executar subtarefa"])
        uc8(["Respeitar horário e reserva"])
        uc9(["Pedir orientação"])
        uc10(["Chamar funções da app"])
    end

    agente["🤖 Agente de IA"]

    usuario --- uc1
    usuario --- uc2
    usuario --- uc3
    usuario --- uc4
    usuario --- uc5
    usuario --- uc6
    tempo --- uc7

    uc7 --- agente
    uc10 --- agente

    uc7 -. "«include»" .-> uc8
    uc9 -. "«extend»" .-> uc7
    uc3 -. "«extend»" .-> uc2
```

### Protocolos

```mermaid
flowchart LR
    usuario["👤 Usuário"]
    tempo["⏱ Tempo"]

    subgraph sistema["Overtime Dispatcher"]
        uc1(["Gerenciar protocolo"])
        uc2(["Executar protocolo"])
        uc3(["Acessar site, e-mail ou calendário"])
        uc4(["Injetar task"])
        uc5(["Aceitar ou recusar proposta"])
    end

    agente["🤖 Agente de IA"]
    externos["🌐 Serviços externos"]

    usuario --- uc1
    usuario --- uc5
    tempo --- uc2

    uc2 --- agente
    uc3 --- externos

    uc2 -. "«include»" .-> uc3
    uc4 -. "«extend»" .-> uc2
```

### Trilhas e Oráculo

```mermaid
flowchart LR
    usuario["👤 Usuário"]
    tempo["⏱ Tempo"]

    subgraph sistema["Overtime Dispatcher"]
        uc1(["Gerenciar trilha"])
        uc2(["Planejar blocos"])
        uc3(["Marcar bloco feito"])
        uc4(["Injetar blocos no calendário"])
        uc5(["Analisar projeto"])
        uc6(["Aceitar ou recusar sugestão"])
    end

    agente["🤖 Agente de IA"]

    usuario --- uc1
    usuario --- uc2
    usuario --- uc3
    usuario --- uc6
    tempo --- uc5

    uc5 --- agente

    uc2 -. "«include»" .-> uc4
```

### Configuração e avisos

```mermaid
flowchart LR
    usuario["👤 Usuário"]

    subgraph sistema["Overtime Dispatcher"]
        uc1(["Definir horário de funcionamento"])
        uc2(["Definir reserva de uso"])
        uc3(["Ativar modo silencioso"])
        uc4(["Receber notificação"])
        uc5(["Responder pela notificação"])
    end

    externos["🌐 Serviços externos"]

    usuario --- uc1
    usuario --- uc2
    usuario --- uc3
    usuario --- uc4

    uc4 --- externos

    uc5 -. "«extend»" .-> uc4
```
