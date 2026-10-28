# Módulo `persistence`

Guarda e carrega o estado da aplicação. Tudo passa por interfaces, para que o formato possa mudar sem afetar o resto.

## Interfaces

```java
public interface TaskStore {
    List<Task> listar();
    Optional<Task> buscar(String id);
    void salvar(Task task);          // cria ou atualiza, incluindo subtarefas e execuções
    void remover(String id);
}

public interface RepositorioStore { ... }   // mesmo formato, para Repositorio
public interface ConfigStore { Config carregar(); void salvar(Config config); }
```

## Primeira implementação: arquivos JSON

```
<pasta de dados>/
  config.json
  repositorios.json
  tasks/
    T-1.json
    T-2.json
  logs/
    T-1/<execucaoId>.log
  workspace/
    T-2/            pasta de trabalho de Tasks sem repositório
```

- Um arquivo por Task: alterações isoladas e fáceis de inspecionar ou versionar.
- Escrita **atômica**: grava num arquivo temporário e renomeia, para não corromper em caso de queda.
- Os logs ficam fora do JSON (só o caminho é guardado).
- Pasta de dados padrão: `%USERPROFILE%\.auto-claude\` (configurável).

## Futuro

`SqliteTaskStore` quando o volume ou as consultas (ex.: métricas de consumo) justificarem. A troca é feita só no `Main`.

## Versão do esquema

Cada arquivo carrega um campo `versaoEsquema`, para permitir migrações quando o modelo mudar.
