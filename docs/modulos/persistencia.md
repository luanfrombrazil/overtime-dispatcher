# Módulo `persistence`

Guarda e carrega o estado da aplicação. Tudo passa por interfaces, para que o formato possa mudar sem afetar o resto.

## Interfaces

```java
public interface TaskStore {
    List<Task> listar();
    Optional<Task> buscar(String id);
    void salvar(Task task);          // cria ou atualiza a Task e suas subtarefas
    void remover(String id);
}

public interface ExecucaoStore {
    void salvar(Execucao execucao);
    List<Execucao> listarPorSubtarefa(String subtarefaId);
    Path caminhoDoLog(String execucaoId);
}

public interface ProjetoStore {
    List<Projeto> listar();
    void salvar(Projeto projeto);
    void remover(String id);             // só sem Tasks em andamento
}
public interface ConfigStore { Config carregar(); void salvar(Config config); }
public interface SequenciaStore { String proximoId(TipoId tipo); }   // T-13, E-0032 (internos)
```

## Primeira implementação: arquivos JSON numa pasta central

Leitura e gravação com **Jackson** (decisão D-35), omitindo campos vazios.

Pasta de dados padrão: `%USERPROFILE%\.overtime-dispatcher\` (configurável). Todos os projetos ficam nela, tenham ou não repositório (decisão D-17). A estrutura segue a hierarquia Projeto › Task (decisão D-26):

```
.overtime-dispatcher\
  config.json              configurações gerais, provedores, tetos, canais
  sequencia.json           próximos IDs internos
  projetos\
    meu-app\               uma pasta por projeto (id = slug do nome)
      projeto.json         nome, descrição, repositório, padrões, permissões
      tasks\
        T-12\              uma pasta por Task (decisão D-16)
          task.json        a Task e suas subtarefas
          execucoes\
            E-0031.json    metadados da execução (subtarefa, consumo, resultado, provedor, esforço)
            E-0031.log     saída completa do agente
  protocolos\
    tarefas-faculdade\     uma pasta por protocolo
      protocolo.json       passos, quando roda, acessos, provedor, aviso
      resultados\
        R-0007.json        o que foi encontrado, o que é novo, ações feitas, consumo
        R-0007.log         saída completa do agente
  navegador\               perfil do navegador usado no crawling (sessões de login feitas por você)
  workspace\
    backup-pessoal-T-15\   pasta de trabalho de Tasks de projetos sem repositório
  worktrees\
    meu-app-T-12\          git worktree da Task (branch overtime/T-12)
  estatisticas\
    atividade.json         contagem diária para o mapa de atividade e os gráficos da Home
    sessoes.json           resumo de cada sessão de overtime (início, fim, consumo, Tasks)
```

A pasta por Task mantém o `task.json` pequeno: o histórico de execuções, que cresce a cada tentativa, fica em arquivos separados. As estatísticas são agregadas a cada execução, para a Home abrir rápido sem varrer todas as execuções.

## Exemplo de `projeto.json`

```json
{
  "id": "meu-app",
  "nome": "Meu App",
  "descricao": "App web de agendamento. Java 21, Spring Boot, testes com JUnit 5.",
  "repositorio": { "caminho": "C:\\code\\meu-app", "branchPadrao": "main" },
  "provedorPadrao": "AUTOMATICO",
  "esforcoPadrao": "MEDIO",
  "permissoes": ["EDITAR_ARQUIVOS", "RODAR_TESTES_E_BUILD"]
}
```

## Exemplo de `task.json`

```json
{
  "id": "T-12",
  "titulo": "Corrigir login",
  "descricao": "O login falha quando o e-mail tem letras maiúsculas.",
  "prioridade": "ALTA",
  "esforco": "MEDIO",
  "provedor": "CLAUDE_CODE",
  "modelo": "sonnet",
  "sessionId": "8f3c2a1e-...",
  "branch": "overtime/T-12",
  "commitBase": "8a20c85",
  "pausada": false,
  "subtarefas": [
    {
      "id": "T-12.1",
      "titulo": "Reproduzir o bug",
      "instrucao": "Escreva um teste que reproduza a falha de login com e-mail em maiúsculas.",
      "criterioDeAceite": "Existe um teste que falha pelo motivo descrito.",
      "requerVerificacao": false,
      "status": "CONCLUIDA",
      "tentativas": 1
    },
    {
      "id": "T-12.2",
      "titulo": "Corrigir",
      "instrucao": "Normalize o e-mail antes da comparação.",
      "criterioDeAceite": "O teste da T-12.1 passa e nenhum outro quebra.",
      "requerVerificacao": true,
      "dependeDe": ["T-12.1"],
      "status": "A_FAZER",
      "tentativas": 0
    }
  ]
}
```

Só entra o que o agendador ou o prompt usam:

| Campo | Para quê |
|---|---|
| `prioridade`, `pausada`, `status` | Escolher a próxima subtarefa |
| (pasta do projeto) | O projeto é a pasta onde o `task.json` está; o repositório e as permissões vêm do `projeto.json` |
| `provedor`, `modelo`, `esforco`, `sessionId` | Como chamar o agente |
| `dependeDe` | Quais subtarefas podem rodar agora |
| `branch`, `commitBase`, `mergeada` | Worktree da Task, diff para revisão e se o merge já foi feito |
| `descricao`, `instrucao`, `criterioDeAceite`, `feedback` | Montar o prompt |
| `requerVerificacao`, `tentativas` | O que fazer quando a execução termina |
| `titulo` | Tela e notificações |

Ficaram de fora, porque podem ser derivados ou vir da configuração:

- **status da Task:** calculado a partir das subtarefas e de `pausada`;
- **ordem das subtarefas:** é a ordem da lista;
- **máximo de tentativas e permissões:** vêm da configuração e do repositório;
- **histórico de execuções:** está na pasta `execucoes/`;
- **datas de criação:** o ID sequencial já desempata a fila;
- **campos vazios** (`sessionId`, `branch`, `commitBase`, `mergeada`, `feedback`, `pergunta`): são omitidos do arquivo.

## Regras de gravação

1. **Um único escritor:** só o `service` grava; a UI e o agendador passam por ele.
2. **Gravação atômica:** grava em `task.json.tmp` e renomeia, para não corromper em caso de queda.
3. **Cache em memória:** na abertura, a app carrega todas as Tasks; cada mudança é gravada em disco na hora.
4. **Edição externa:** com a app fechada, pode editar à vontade. Com a app aberta, quem editar um `task.json` (você ou um agente) precisa avisar a app pelo [RPC](rpc.md) (`tarefa.recarregar`); a app valida o arquivo e, se estiver inválido, mantém a versão anterior. Sem esse aviso, a alteração é sobrescrita na próxima gravação. Sempre que possível, prefira os métodos de RPC (como `subtarefa.adicionar`) a editar o arquivo.

## Versão do esquema

A versão do formato fica em `config.json` (`versaoDados`), e não em cada arquivo. Se o modelo mudar, a app migra todos os arquivos de uma vez ao abrir.

## Futuro

- `SqliteTaskStore` quando o volume ou as consultas (ex.: métricas de consumo) justificarem. A troca é feita só no `Main`.
- Exportar as Tasks de um repositório para dentro dele (ex.: `.overtime/tasks/`), para versioná-las junto com o código.
