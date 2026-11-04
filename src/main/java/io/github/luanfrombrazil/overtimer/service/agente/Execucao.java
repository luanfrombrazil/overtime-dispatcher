package io.github.luanfrombrazil.overtimer.service.agente;

import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Modelo;
import io.github.luanfrombrazil.overtimer.model.Permissoes;

import java.nio.file.Path;
import java.time.Duration;

/**
 * O que entregar ao agente numa execução.
 *
 * @param pasta      onde o agente trabalha (a pasta do projeto ou o worktree da Task)
 * @param sessao     id da sessão a usar
 * @param sessaoNova true na primeira execução da sessão; false para continuar uma sessão existente
 * @param modelo     modelo pedido ao agente; {@link Modelo#PADRAO} deixa o agente escolher
 * @param usaGit     se a pasta é um worktree git, para liberar os comandos git ao agente
 */
public record Execucao(Path pasta, String prompt, String sessao, boolean sessaoNova,
                       Esforco esforco, Modelo modelo, Permissoes permissoes, boolean usaGit, Duration limite) {

    public Execucao {
        modelo = modelo == null ? Modelo.PADRAO : modelo;
    }
}
