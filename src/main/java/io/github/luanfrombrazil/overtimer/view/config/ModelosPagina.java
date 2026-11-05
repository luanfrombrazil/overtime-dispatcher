package io.github.luanfrombrazil.overtimer.view.config;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Prompts;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Prompts.ModeloDePrompt;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Prompts.Uso;
import io.github.luanfrombrazil.overtimer.model.Configuracoes.Prompts.VariavelEditavel;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.view.comum.Chip;
import io.github.luanfrombrazil.overtimer.view.comum.Interruptor;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Seção Modelos das configurações: linha de comando e prompt de cada modelo, com as variáveis disponíveis. Os
 * modelos de prompt e as regras das variáveis editáveis vão para as configurações ({@link #prompts()}); a linha de
 * comando por enquanto fica só na tela.
 */
class ModelosPagina extends JPanel {

    private static final List<Provedor> MODELOS = List.of(Provedor.CLAUDE_CODE, Provedor.CODEX, Provedor.GEMINI);

    /** Grupo das variáveis cuja regra pode ser editada, onde também entram as criadas. */
    private static final String GRUPO_EDITAVEIS = "Variáveis editáveis";

    /** Nome de variável: letras, números, "_" e ".". */
    private static final Pattern NOME_DE_VARIAVEL = Pattern.compile("[\\w.]+");

    /** Nome de um modelo de prompt salvo com o campo Nome vazio. */
    private static final String SEM_NOME = "Sem nome";

    private static final int LARGURA_EDITOR = 600;
    private static final int LARGURA_EDITOR_MINIMA = 400;
    private static final int LARGURA_VARIAVEIS = 250;

    private static final Map<Provedor, String> COMANDO_PADRAO = Map.of(
            Provedor.CLAUDE_CODE, """
                    claude
                    -p
                    --output-format stream-json
                    --verbose
                    --effort {esforco}
                    --model {modelo}
                    {sessao}
                    --allowedTools {ferramentas.permitidas}
                    --disallowedTools {ferramentas.bloqueadas}
                    --mcp-config {mcp.config}""",
            Provedor.CODEX, """
                    codex
                    exec
                    --json
                    --model {modelo}
                    {sessao}""",
            Provedor.GEMINI, """
                    gemini
                    -p
                    --output-format stream-json
                    --model {modelo}
                    {sessao}""");

    private static final Map<String, List<Variavel>> VARIAVEIS_COMANDO = grupos(
            "Execução", List.of(
                    fixa("esforco", "Esforço da subtarefa"),
                    fixa("modelo", "Modelo escolhido na Task"),
                    fixa("sessao", "Retomada da conversa, quando houver")),
            "Permissões", List.of(
                    fixa("ferramentas.permitidas", "Ferramentas liberadas pelas permissões do projeto"),
                    fixa("ferramentas.bloqueadas", "Ferramentas negadas pelas permissões do projeto")),
            "Integrações", List.of(
                    fixa("mcp.config", "Configuração dos servidores MCP")),
            "Projeto", List.of(
                    fixa("pasta", "Pasta do worktree"),
                    fixa("branch", "Branch do worktree")));

    private static final Map<String, List<Variavel>> VARIAVEIS_PROMPT = grupos(
            "Projeto", List.of(
                    fixa("projeto.nome", "Nome do projeto"),
                    fixa("projeto.descricao", "Descrição do projeto"),
                    fixa("branch", "Branch do worktree")),
            "Task", List.of(
                    fixa("task.titulo", "Título da Task"),
                    fixa("task.descricao", "Descrição da Task")),
            "Subtarefa", List.of(
                    fixa("subtarefa.titulo", "Título da subtarefa"),
                    fixa("subtarefa.instrucao", "Instrução da subtarefa"),
                    fixa("subtarefa.criterio", "Critério de aceite da subtarefa"),
                    fixa("concluidas", "Subtarefas já concluídas nesta Task")),
            "Direcionamento", List.of(
                    fixa("direcionamento", "Respostas já dadas a pedidos de direcionamento")),
            GRUPO_EDITAVEIS, List.of(
                    new Variavel("regras.permissoes", "Regras das permissões do projeto", """
                            - Você pode criar e editar arquivos do projeto.
                            - Não rode comandos de teste nem de build.
                            - Não instale nem atualize dependências.
                            - Não acesse a internet."""),
                    new Variavel("regras.git", "Regras do worktree e do commit", """
                            - Você está num worktree separado, na branch `{branch}`. Ao terminar, faça commit das suas \
                            mudanças nesta branch com uma mensagem clara. Não troque de branch, não faça merge nem push."""),
                    new Variavel("regras.resposta", "Regras de como terminar a resposta", """
                            - Ao terminar, responda com um resumo curto do que fez.
                            - Dúvidas pequenas e fáceis de desfazer (nomes, estilo, detalhes internos): decida com bom \
                            senso e registre a suposição no resumo.
                            - Se faltar uma informação que muda o resultado e seria difícil de desfazer, não suponha: faça \
                            o que não depende da resposta e pare pedindo direcionamento.
                            - Se não conseguir concluir, comece a resposta com "FALHOU:" e explique o motivo.""")));

    /** Modelo de prompt sendo editado na tela: nome, texto e onde é usado por padrão. */
    private static final class ModeloEditado {
        private String nome;
        private String texto;
        private Uso usadoEm;

        ModeloEditado(ModeloDePrompt modelo) {
            nome = modelo.nome();
            texto = modelo.texto();
            usadoEm = modelo.usadoEm();
        }

        ModeloDePrompt salvo() {
            return new ModeloDePrompt(nome.isBlank() ? SEM_NOME : nome.strip(), texto, usadoEm);
        }

        @Override
        public String toString() {
            return nome.isBlank() ? SEM_NOME : nome;
        }
    }

    private final Map<Provedor, String> comandos = new EnumMap<>(COMANDO_PADRAO);
    private final Map<Provedor, Boolean> ativos = new EnumMap<>(Provedor.class);
    private final Map<Provedor, Chip> chips = new EnumMap<>(Provedor.class);
    private Provedor selecionado = Provedor.CLAUDE_CODE;

    /** Regra atual de cada variável modificável, pelo nome. */
    private final Map<String, String> regras = new HashMap<>();

    private final Interruptor ativo = new Interruptor(true);
    private final EditorDeVariaveis comando = new EditorDeVariaveis(todas(VARIAVEIS_COMANDO), this::editarRegra);
    /** Variáveis editáveis criadas pela pessoa, que entram no fim do grupo das editáveis do prompt. */
    private final List<Variavel> criadas = new ArrayList<>();
    /** Todas as variáveis do prompt, incluindo as criadas; o editor lê esta lista ao sugerir e colorir. */
    private final List<Variavel> variaveisDoPrompt = new ArrayList<>(todas(VARIAVEIS_PROMPT));
    private final EditorDeVariaveis prompt = new EditorDeVariaveis(variaveisDoPrompt, this::editarRegra);
    private final JPanel colunaDoPrompt = new JPanel(new BorderLayout());
    private final JComboBox<ModeloEditado> modeloDePrompt = new JComboBox<>();
    private final JTextField nome = new JTextField();
    private final JComboBox<Uso> usadoEm = new JComboBox<>(Uso.values());
    private final JLabel tokens = Ui.legenda("");
    /** Enquanto os campos mostram outro modelo de prompt, as mudanças neles não são edições. */
    private boolean mostrandoModelo;

    /** @param prompts os prompts salvos nas configurações */
    ModelosPagina(Prompts prompts) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        MODELOS.forEach(m -> ativos.put(m, true));

        add(topo());
        add(Box.createVerticalStrut(20));
        add(Ui.cabecalho(Ui.secao("Linha de comando"), Ui.botaoSecundario("Prévia")));
        add(Box.createVerticalStrut(10));
        add(lado(rolagem(comando, 202), variaveis(VARIAVEIS_COMANDO, comando, null)));

        add(Box.createVerticalStrut(28));
        add(Ui.cabecalho(Ui.secao("Prompt"), Ui.botaoSecundario("Prévia")));
        add(Box.createVerticalStrut(10));
        add(Ui.cabecalho(Ui.linha(16,
                campo("Modelo de prompt", tamanho(modeloDePrompt, 250)),
                campo("Nome", tamanho(nome, 190)),
                campo("Usado por padrão em", tamanho(usadoEm, 170))), null));
        add(Box.createVerticalStrut(10));
        JPanel textoDoPrompt = Ui.coluna(8, rolagem(prompt, 472), Ui.linha(0, Box.createHorizontalGlue(), tokens));
        colunaDoPrompt.setOpaque(false);
        add(lado(textoDoPrompt, colunaDoPrompt));

        ativo.addPropertyChangeListener("ligado", e -> ativos.put(selecionado, ativo.isLigado()));
        comando.getDocument().addDocumentListener(aoEditar(() -> comandos.put(selecionado, comando.getText())));
        prompt.getDocument().addDocumentListener(aoEditar(() -> {
            contarTokens();
            editarModelo(m -> m.texto = prompt.getText());
        }));
        nome.getDocument().addDocumentListener(aoEditar(() -> editarModelo(m -> {
            m.nome = nome.getText();
            modeloDePrompt.repaint();
        })));
        usadoEm.addActionListener(e -> editarModelo(m -> m.usadoEm = (Uso) usadoEm.getSelectedItem()));
        nome.setFont(Tema.mono());
        modeloDePrompt.setFont(Tema.corpo());
        usadoEm.setFont(Tema.corpo());
        modeloDePrompt.addActionListener(e -> mostrarModeloDePrompt());

        carregar(prompts);
        mostrar(Provedor.CLAUDE_CODE);
    }

    /** Os modelos de prompt e as regras como estão na tela; das regras embutidas, só as que mudaram. */
    Prompts prompts() {
        List<ModeloDePrompt> modelos = new ArrayList<>();
        for (int i = 0; i < modeloDePrompt.getItemCount(); i++) {
            modelos.add(modeloDePrompt.getItemAt(i).salvo());
        }
        List<VariavelEditavel> variaveis = new ArrayList<>();
        for (Variavel embutida : VARIAVEIS_PROMPT.get(GRUPO_EDITAVEIS)) {
            String regra = regras.get(embutida.nome());
            // A regra de exemplo da tela não vale para todo projeto: sem edição, o agendador gera a do projeto.
            if (regra != null && !regra.equals(embutida.regra())) {
                variaveis.add(new VariavelEditavel(embutida.nome(), embutida.descricao(), regra));
            }
        }
        criadas.forEach(v -> variaveis.add(
                new VariavelEditavel(v.nome(), v.descricao(), regras.getOrDefault(v.nome(), v.regra()))));
        return new Prompts(modelos, variaveis);
    }

    /** Volta a linha de comando do modelo escolhido, os modelos de prompt e as regras ao padrão. */
    void restaurarPadrao() {
        comandos.put(selecionado, COMANDO_PADRAO.get(selecionado));
        mostrar(selecionado);
        carregar(Prompts.padrao());
    }

    /** Mostra os modelos de prompt, as regras editadas e as variáveis criadas. */
    private void carregar(Prompts prompts) {
        regras.clear();
        criadas.clear();
        for (Map<String, List<Variavel>> grupos : List.of(VARIAVEIS_COMANDO, VARIAVEIS_PROMPT)) {
            todas(grupos).stream().filter(Variavel::modificavel).forEach(v -> regras.put(v.nome(), v.regra()));
        }
        for (VariavelEditavel salva : prompts.variaveis()) {
            if (regras.containsKey(salva.nome())) {
                regras.put(salva.nome(), salva.regra());
            } else if (todas(VARIAVEIS_PROMPT).stream().noneMatch(v -> v.nome().equals(salva.nome()))) {
                criadas.add(new Variavel(salva.nome(), salva.descricao(), salva.regra()));
                regras.put(salva.nome(), salva.regra());
            }
        }
        variaveisDoPrompt.clear();
        variaveisDoPrompt.addAll(todas(VARIAVEIS_PROMPT));
        variaveisDoPrompt.addAll(criadas);
        mostrarVariaveisDoPrompt();

        mostrandoModelo = true;
        modeloDePrompt.removeAllItems();
        prompts.modelos().forEach(m -> modeloDePrompt.addItem(new ModeloEditado(m)));
        mostrandoModelo = false;
        if (modeloDePrompt.getItemCount() > 0) {
            modeloDePrompt.setSelectedIndex(0);
        }
        mostrarModeloDePrompt();
    }

    /** Aplica uma edição dos campos ao modelo de prompt escolhido. */
    private void editarModelo(Consumer<ModeloEditado> edicao) {
        ModeloEditado modelo = (ModeloEditado) modeloDePrompt.getSelectedItem();
        if (!mostrandoModelo && modelo != null) {
            edicao.accept(modelo);
        }
    }

    /** Pede nome e regra de uma variável editável nova e a coloca no grupo das editáveis do prompt. */
    private void criarVariavel() {
        RegraDialog.criar(this, nome -> NOME_DE_VARIAVEL.matcher(nome).matches()
                        && variaveisDoPrompt.stream().noneMatch(v -> v.nome().equals(nome))
                        && todas(VARIAVEIS_COMANDO).stream().noneMatch(v -> v.nome().equals(nome)))
                .ifPresent(nova -> {
                    criadas.add(nova);
                    variaveisDoPrompt.add(nova);
                    regras.put(nova.nome(), nova.regra());
                    mostrarVariaveisDoPrompt();
                });
    }

    /** Remonta a coluna de variáveis do prompt, com as criadas no fim do grupo das editáveis. */
    private void mostrarVariaveisDoPrompt() {
        Map<String, List<Variavel>> grupos = new LinkedHashMap<>(VARIAVEIS_PROMPT);
        List<Variavel> editaveis = new ArrayList<>(VARIAVEIS_PROMPT.get(GRUPO_EDITAVEIS));
        editaveis.addAll(criadas);
        grupos.put(GRUPO_EDITAVEIS, editaveis);
        JComponent coluna = variaveis(grupos, prompt, this::criarVariavel);
        colunaDoPrompt.removeAll();
        colunaDoPrompt.add(coluna, BorderLayout.NORTH);
        tamanho(colunaDoPrompt, LARGURA_VARIAVEIS, coluna.getPreferredSize().height);
        colunaDoPrompt.revalidate();
        colunaDoPrompt.repaint();
    }

    /** Abre a caixa de edição da regra da variável, já com a regra atual. */
    private void editarRegra(Variavel variavel) {
        Variavel atual = variavel.comRegra(regras.getOrDefault(variavel.nome(), variavel.regra()));
        RegraDialog.editar(this, atual).ifPresent(regra -> regras.put(variavel.nome(), regra));
    }

    private JComponent topo() {
        List<JComponent> itens = new ArrayList<>();
        for (Provedor modelo : MODELOS) {
            Chip chip = new Chip(modelo.toString(), false).aoClicar(() -> mostrar(modelo));
            chips.put(modelo, chip);
            itens.add(chip);
        }
        return Ui.cabecalho(Ui.linha(8, itens.toArray(JComponent[]::new)), Ui.linha(8, Ui.corpo("Ativo"), ativo));
    }

    private void mostrar(Provedor modelo) {
        selecionado = modelo;
        chips.forEach((m, chip) -> chip.setSelecionado(m == modelo));
        ativo.setLigado(ativos.get(modelo));
        comando.setText(comandos.get(modelo));
        comando.setCaretPosition(0);
    }

    private void mostrarModeloDePrompt() {
        ModeloEditado modelo = (ModeloEditado) modeloDePrompt.getSelectedItem();
        if (modelo == null || mostrandoModelo) {
            return;
        }
        mostrandoModelo = true;
        try {
            nome.setText(modelo.nome);
            usadoEm.setSelectedItem(modelo.usadoEm);
            prompt.setText(modelo.texto);
            prompt.setCaretPosition(0);
        } finally {
            mostrandoModelo = false;
        }
    }

    private void contarTokens() {
        // Estimativa grosseira: cerca de 4 caracteres por token.
        tokens.setText("~" + Math.round(prompt.getDocument().getLength() / 4.0 / 10) * 10 + " tokens");
    }

    // ---------- Editor ----------

    private static JScrollPane rolagem(EditorDeVariaveis editor, int altura) {
        JScrollPane rolagem = new JScrollPane(editor,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.setBorder(BorderFactory.createLineBorder(Tema.ACENTO, 1));
        rolagem.getViewport().setBackground(Tema.LOG);
        // Ocupa a largura que sobra ao lado da coluna de variáveis.
        rolagem.setPreferredSize(new Dimension(LARGURA_EDITOR, altura));
        rolagem.setMinimumSize(new Dimension(LARGURA_EDITOR_MINIMA, altura));
        rolagem.setMaximumSize(new Dimension(Integer.MAX_VALUE, altura));
        return rolagem;
    }

    private static DocumentListener aoEditar(Runnable acao) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                acao.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                acao.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Mudança só de estilo (a própria coloração): nada a fazer.
            }
        };
    }

    // ---------- Variáveis ----------

    /** @param criar ação do botão "+" ao lado do grupo das editáveis, ou null para não mostrá-lo */
    private JComponent variaveis(Map<String, List<Variavel>> grupos, EditorDeVariaveis destino, Runnable criar) {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setOpaque(false);
        painel.add(Ui.secao("Variáveis"));
        grupos.forEach((grupo, doGrupo) -> {
            painel.add(Box.createVerticalStrut(14));
            painel.add(grupo.equals(GRUPO_EDITAVEIS) && criar != null
                    ? Ui.linha(6, Ui.legenda(grupo), botaoNovaVariavel(criar))
                    : Ui.legenda(grupo));
            painel.add(Box.createVerticalStrut(6));
            linhasDeVariaveis(doGrupo, destino).forEach(linha -> {
                painel.add(linha);
                painel.add(Box.createVerticalStrut(6));
            });
        });
        // Tudo alinhado à esquerda: no BoxLayout vertical, alinhamentos misturados centralizam as linhas.
        for (java.awt.Component filho : painel.getComponents()) {
            ((JComponent) filho).setAlignmentX(LEFT_ALIGNMENT);
        }
        return tamanho(painel, LARGURA_VARIAVEIS, painel.getPreferredSize().height);
    }

    /** Botão pequeno "+" que cria uma variável editável. */
    private static JButton botaoNovaVariavel(Runnable criar) {
        JButton botao = Ui.botaoSecundario("+");
        botao.putClientProperty(FlatClientProperties.STYLE, botao.getClientProperty(FlatClientProperties.STYLE)
                + "; arc:4; margin:0,0,0,0; foreground:#9DA0A8");
        botao.setFont(Tema.legenda());
        botao.addActionListener(e -> criar.run());
        return tamanho(botao, 16, 16);
    }

    /**
     * Editor à esquerda e variáveis à direita, alinhados pelo topo. Fica com o alinhamento horizontal padrão, igual
     * ao dos cabeçalhos: no BoxLayout vertical da página, alinhamentos misturados deslocam a linha para a direita.
     */
    private static JComponent lado(JComponent editor, JComponent variaveis) {
        // A altura máxima acompanha a preferida, que muda quando a coluna de variáveis é remontada.
        JPanel linha = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        linha.setLayout(new BoxLayout(linha, BoxLayout.X_AXIS));
        linha.setOpaque(false);
        editor.setAlignmentY(TOP_ALIGNMENT);
        variaveis.setAlignmentY(TOP_ALIGNMENT);
        linha.add(editor);
        linha.add(Box.createHorizontalStrut(16));
        linha.add(variaveis);
        return linha;
    }

    /** Distribui as variáveis em linhas que cabem na largura da coluna. */
    private List<JComponent> linhasDeVariaveis(List<Variavel> doGrupo, EditorDeVariaveis destino) {
        List<JComponent> linhas = new ArrayList<>();
        List<JComponent> atual = new ArrayList<>();
        int largura = 0;
        for (Variavel variavel : doGrupo) {
            VariavelChip chip = new VariavelChip(variavel, destino);
            int w = chip.getPreferredSize().width;
            if (!atual.isEmpty() && largura + 6 + w > LARGURA_VARIAVEIS) {
                linhas.add(Ui.linha(6, atual.toArray(JComponent[]::new)));
                atual = new ArrayList<>();
                largura = 0;
            }
            largura += (atual.isEmpty() ? 0 : 6) + w;
            atual.add(chip);
        }
        if (!atual.isEmpty()) {
            linhas.add(Ui.linha(6, atual.toArray(JComponent[]::new)));
        }
        return linhas;
    }

    /**
     * Variável mostrada na coluna lateral, como no texto: {@code {nome}}. Arrastada, entra no ponto do texto onde é
     * solta; clicada, entra no cursor.
     */
    private static final class VariavelChip extends JLabel {

        VariavelChip(Variavel variavel, EditorDeVariaveis destino) {
            super(variavel.marcador());
            setFont(Tema.mono().deriveFont(11f));
            setForeground(EditorDeVariaveis.corDaVariavel(variavel.modificavel()));
            setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
            setToolTipText(variavel.descricao());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setTransferHandler(new TransferHandler() {
                @Override
                public int getSourceActions(JComponent c) {
                    return COPY;
                }

                @Override
                protected Transferable createTransferable(JComponent c) {
                    return new StringSelection(variavel.marcador());
                }
            });
            MouseAdapter mouse = new MouseAdapter() {
                private boolean arrastou;

                @Override
                public void mousePressed(MouseEvent e) {
                    arrastou = false;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!arrastou) {
                        arrastou = true;
                        getTransferHandler().exportAsDrag(VariavelChip.this, e, TransferHandler.COPY);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                        destino.inserir(variavel);
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            Ui.fixo(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.SUTIL);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
            g2.setColor(Tema.BORDA);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---------- Auxiliares ----------

    private static JComponent campo(String rotulo, JComponent entrada) {
        return Ui.coluna(4, Ui.legenda(rotulo), entrada);
    }

    private static <T extends JComponent> T tamanho(T componente, int largura) {
        return tamanho(componente, largura, 30);
    }

    private static <T extends JComponent> T tamanho(T componente, int largura, int altura) {
        Dimension d = new Dimension(largura, altura);
        componente.setPreferredSize(d);
        componente.setMinimumSize(d);
        componente.setMaximumSize(d);
        return componente;
    }

    private static Variavel fixa(String nome, String descricao) {
        return new Variavel(nome, descricao, null);
    }

    private static List<Variavel> todas(Map<String, List<Variavel>> grupos) {
        return grupos.values().stream().flatMap(Collection::stream).toList();
    }

    private static Map<String, List<Variavel>> grupos(Object... pares) {
        Map<String, List<Variavel>> grupos = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            @SuppressWarnings("unchecked")
            List<Variavel> doGrupo = (List<Variavel>) pares[i + 1];
            grupos.put((String) pares[i], doGrupo);
        }
        return grupos;
    }
}
