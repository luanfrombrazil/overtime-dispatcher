package io.github.luanfrombrazil.overtimedispatcher.service.agente;

import io.github.luanfrombrazil.overtimedispatcher.model.Esforco;
import io.github.luanfrombrazil.overtimedispatcher.model.Permissoes;

import java.nio.file.Path;
import java.time.Duration;

/**
 * O que entregar ao agente numa execução.
 *
 * @param pasta      onde o agente trabalha (a pasta do projeto ou o worktree da Task)
 * @param sessao     id da sessão a usar
 * @param sessaoNova true na primeira execução da sessão; false para continuar uma sessão existente
 * @param usaGit     se a pasta é um worktree git, para liberar os comandos git ao agente
 */
public record Execucao(Path pasta, String prompt, String sessao, boolean sessaoNova,
                       Esforco esforco, Permissoes permissoes, boolean usaGit, Duration limite) {
}
