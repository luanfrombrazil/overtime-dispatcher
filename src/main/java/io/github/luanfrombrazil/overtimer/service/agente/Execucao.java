package io.github.luanfrombrazil.overtimer.service.agente;

import io.github.luanfrombrazil.overtimer.model.Acessos;
import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Modelo;
import io.github.luanfrombrazil.overtimer.model.Permissoes;

import java.nio.file.Path;
import java.time.Duration;

/**
 * O que entregar ao agente numa execução.
 *
 * @param pasta      onde o agente trabalha (a pasta do projeto, o worktree da Task ou a pasta de trabalho do
 *                   protocolo)
 * @param sessao     id da sessão a usar
 * @param sessaoNova true na primeira execução da sessão; false para continuar uma sessão existente
 * @param modelo     modelo pedido ao agente; {@link Modelo#PADRAO} deixa o agente escolher
 * @param usaGit     se a pasta é um worktree git, para liberar os comandos git ao agente
 * @param push       se o agente pode fazer push da branch da Task
 * @param acessos    acessos do protocolo, que substituem as permissões; null na execução de uma Task
 */
public record Execucao(Path pasta, String prompt, String sessao, boolean sessaoNova,
                       Esforco esforco, Modelo modelo, Permissoes permissoes, boolean usaGit, boolean push,
                       Duration limite, Acessos acessos) {

    public Execucao {
        modelo = modelo == null ? Modelo.PADRAO : modelo;
    }

    /** Execução de uma Task, com as permissões do projeto. */
    public Execucao(Path pasta, String prompt, String sessao, boolean sessaoNova,
                    Esforco esforco, Modelo modelo, Permissoes permissoes, boolean usaGit, boolean push,
                    Duration limite) {
        this(pasta, prompt, sessao, sessaoNova, esforco, modelo, permissoes, usaGit, push, limite, null);
    }

    /** Execução de um protocolo na pasta de trabalho dele, sem git. */
    public static Execucao doProtocolo(Path pasta, String prompt, String sessao, boolean sessaoNova,
                                       Esforco esforco, Acessos acessos, Duration limite) {
        return new Execucao(pasta, prompt, sessao, sessaoNova, esforco, Modelo.PADRAO,
                new Permissoes(true, false, false, false), false, false, limite, acessos);
    }
}
