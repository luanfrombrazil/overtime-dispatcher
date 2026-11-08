package io.github.luanfrombrazil.overtimer.view.projeto;

import io.github.luanfrombrazil.overtimer.model.Esforco;
import io.github.luanfrombrazil.overtimer.model.Permissoes;
import io.github.luanfrombrazil.overtimer.model.Projeto;
import io.github.luanfrombrazil.overtimer.model.Provedor;
import io.github.luanfrombrazil.overtimer.view.comum.Dialogo;
import io.github.luanfrombrazil.overtimer.view.comum.Formulario;
import io.github.luanfrombrazil.overtimer.view.comum.Iniciais;
import io.github.luanfrombrazil.overtimer.view.comum.SeletorImagem;
import io.github.luanfrombrazil.overtimer.view.comum.Ui;
import io.github.luanfrombrazil.overtimer.view.tema.Tema;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Pop-up de criação e edição de projeto: nome, descrição, pasta, git, padrões e permissões.
 */
public class ProjetoDialog extends Dialogo {

    private static final List<String> SUGESTOES = List.of("main", "master", "develop");

    private final Projeto existente;
    private final Function<Path, List<String>> branchesDa;
    private String pastaVerificada;
    private final JTextField nome;
    private final Iniciais icone;
    private final JButton removerImagem;
    /** Caminho da imagem do ícone, ou null para as iniciais. */
    private String imagem;
    private final JScrollPane descricao;
    private final JTextField pasta;
    private final JCheckBox usarGit;
    private final JComboBox<String> branch;
    private final JComboBox<Provedor> provedor;
    private final JComboBox<Esforco> esforco;
    private final JCheckBox editarArquivos;
    private final JCheckBox rodarTestes;
    private final JCheckBox instalarDependencias;
    private final JCheckBox acessarInternet;
    /** Servidores MCP liberados, separados por vírgula. */
    private final JTextField conectores;

    /**
     * @param existente projeto a editar, ou null para criar um novo
     * @param aoSalvar  recebe o projeto preenchido; uma IllegalArgumentException mantém o pop-up aberto
     * @param aoExcluir ação de excluir, mostrada só na edição
     * @param branchesDa branches locais do repositório git de uma pasta (vazio se não for um repositório)
     */
    public ProjetoDialog(JFrame dono, Projeto existente, Consumer<Projeto> aoSalvar, Runnable aoExcluir,
                         Function<Path, List<String>> branchesDa) {
        super(dono, Tema.BORDA);
        this.existente = existente;
        this.branchesDa = branchesDa;
        boolean novo = existente == null;
        Permissoes permissoes = novo ? Permissoes.padrao() : existente.permissoes();

        JPanel corpo = Formulario.corpo();
        corpo.add(Ui.cabecalho(Ui.titulo(novo ? "Novo projeto" : "Editar projeto"), null));
        corpo.add(Box.createVerticalStrut(14));

        nome = Formulario.texto(novo ? "" : existente.nome());
        imagem = novo ? null : existente.imagem();
        icone = new Iniciais(Iniciais.de(nome.getText()), imagem, 36);
        icone.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        icone.setToolTipText("Clique para escolher uma imagem .png ou .jpg");
        icone.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                escolherImagem();
            }
        });
        JButton escolher = Ui.botaoSecundario("Imagem…");
        escolher.addActionListener(e -> escolherImagem());
        removerImagem = Ui.botaoSecundario("Remover");
        removerImagem.addActionListener(e -> mudarImagem(null));
        removerImagem.setVisible(imagem != null);
        corpo.add(Formulario.identidade(nome, icone, Ui.linha(8, escolher, removerImagem)));
        corpo.add(Box.createVerticalStrut(12));
        descricao = Formulario.area(novo ? "" : existente.descricao(), 3);
        corpo.add(Formulario.campo("Descrição", descricao));

        Formulario.secao(corpo, "Pasta do projeto");
        pasta = Formulario.texto(novo ? "" : existente.pasta());
        pasta.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                pastaMudou();
            }
        });
        JButton procurar = Ui.botaoSecundario("Procurar…");
        procurar.addActionListener(e -> procurarPasta());
        JPanel linhaPasta = new JPanel(new BorderLayout(8, 0));
        linhaPasta.setOpaque(false);
        linhaPasta.add(pasta, BorderLayout.CENTER);
        linhaPasta.add(procurar, BorderLayout.EAST);
        corpo.add(Ui.largo(linhaPasta));
        corpo.add(Box.createVerticalStrut(8));
        usarGit = Formulario.caixa("Usar git", novo || existente.usaGit());
        corpo.add(Ui.cabecalho(usarGit, null));
        branch = Formulario.lista(200);
        pastaVerificada = pasta.getText().strip();
        atualizarBranches(novo || !existente.usaGit() ? null : existente.branch());
        branch.setEnabled(usarGit.isSelected());
        corpo.add(Box.createVerticalStrut(6));
        corpo.add(Ui.cabecalho(Formulario.campo("Branch padrão", branch), null));
        usarGit.addActionListener(e -> branch.setEnabled(usarGit.isSelected()));

        Formulario.secao(corpo, "Padrões das tasks");
        provedor = Formulario.opcoes(244, Provedor.values());
        esforco = Formulario.opcoes(244, Esforco.values());
        provedor.setSelectedItem(novo ? Provedor.AUTOMATICO : existente.provedor());
        esforco.setSelectedItem(novo ? Esforco.MEDIUM : existente.esforco());
        corpo.add(Ui.cabecalho(Ui.linha(12,
                Formulario.campo("Provedor", provedor),
                Formulario.campo("Esforço", esforco)), null));

        Formulario.secao(corpo, "Permissões do agente");
        editarArquivos = Formulario.caixa("Editar arquivos", permissoes.editarArquivos());
        rodarTestes = Formulario.caixa("Rodar testes e build", permissoes.rodarTestesEBuild());
        instalarDependencias = Formulario.caixa("Instalar dependências", permissoes.instalarDependencias());
        acessarInternet = Formulario.caixa("Acessar a internet", permissoes.acessarInternet());
        corpo.add(Ui.cabecalho(editarArquivos, null));
        corpo.add(Ui.cabecalho(rodarTestes, null));
        corpo.add(Ui.cabecalho(instalarDependencias, null));
        corpo.add(Ui.cabecalho(acessarInternet, null));
        corpo.add(Box.createVerticalStrut(8));
        conectores = Formulario.texto(String.join(", ", permissoes.conectores()));
        corpo.add(Formulario.campo("Conectores MCP", conectores));

        JPanel raiz = (JPanel) getContentPane();
        raiz.add(corpo, BorderLayout.CENTER);
        raiz.add(Formulario.rodape(this, novo ? "Criar projeto" : "Salvar", () -> aoSalvar.accept(projeto()),
                novo ? null : aoExcluir,
                "Excluir o projeto e todas as suas tasks? A pasta do projeto não é alterada."), BorderLayout.SOUTH);
        raiz.setPreferredSize(new Dimension(560, raiz.getPreferredSize().height));
    }

    private void escolherImagem() {
        SeletorImagem.escolher(this).ifPresent(this::mudarImagem);
    }

    private void mudarImagem(String caminho) {
        imagem = caminho;
        icone.setImagem(caminho);
        removerImagem.setVisible(caminho != null);
        revalidate();
    }

    private void procurarPasta() {
        JFileChooser seletor = new JFileChooser(pasta.getText().isBlank() ? null : pasta.getText());
        seletor.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File escolhida = seletor.getSelectedFile();
            pasta.setText(escolhida.getAbsolutePath());
            if (nome.getText().isBlank()) {
                nome.setText(escolhida.getName());
            }
            pastaMudou();
        }
    }

    /** Pasta nova: marca "Usar git" conforme ela seja um repositório e lista as branches dele. */
    private void pastaMudou() {
        String atual = pasta.getText().strip();
        if (atual.equals(pastaVerificada)) {
            return;
        }
        pastaVerificada = atual;
        boolean repositorio = caminho().map(p -> new File(p.toFile(), ".git").exists()).orElse(false);
        usarGit.setSelected(repositorio);
        atualizarBranches(branchEscolhida());
        branch.setEnabled(repositorio);
    }

    /**
     * Preenche o dropdown com as branches da pasta e seleciona a preferida: a já escolhida, se existir lá;
     * senão main, master ou develop; senão a primeira. Sem repositório (ou sem commits), mostra sugestões
     * comuns e deixa digitar.
     */
    private void atualizarBranches(String preferida) {
        List<String> encontradas = caminho().map(branchesDa).orElse(List.of());
        boolean sugestoes = encontradas.isEmpty();
        List<String> opcoes = sugestoes ? SUGESTOES : encontradas;
        branch.setModel(new DefaultComboBoxModel<>(opcoes.toArray(String[]::new)));
        branch.setEditable(sugestoes);
        String selecionada = opcoes.getFirst();
        if (preferida != null && (sugestoes || opcoes.contains(preferida))) {
            selecionada = preferida;
        } else {
            selecionada = SUGESTOES.stream().filter(opcoes::contains).findFirst().orElse(selecionada);
        }
        branch.setSelectedItem(selecionada);
    }

    private Optional<Path> caminho() {
        String texto = pasta.getText().strip();
        if (texto.isEmpty()) {
            return Optional.empty();
        }
        try {
            Path p = Path.of(texto);
            return Files.isDirectory(p) ? Optional.of(p) : Optional.empty();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    private String branchEscolhida() {
        Object item = branch.isEditable() ? branch.getEditor().getItem() : branch.getSelectedItem();
        return item == null ? "" : item.toString().strip();
    }

    private Projeto projeto() {
        String nomeDigitado = nome.getText().strip();
        String branchEscolhida = usarGit.isSelected() ? branchEscolhida() : null;
        Permissoes permissoes = new Permissoes(editarArquivos.isSelected(), rodarTestes.isSelected(),
                instalarDependencias.isSelected(), acessarInternet.isSelected(),
                Permissoes.conectoresDe(conectores.getText()));
        Provedor p = (Provedor) provedor.getSelectedItem();
        Esforco e = (Esforco) esforco.getSelectedItem();
        String sigla = Iniciais.de(nomeDigitado);
        if (existente == null) {
            return Projeto.novo(nomeDigitado, sigla, Formulario.textoDa(descricao), pasta.getText().strip(),
                    branchEscolhida, p, e, permissoes).comImagem(imagem);
        }
        return new Projeto(existente.id(), nomeDigitado, sigla, Formulario.textoDa(descricao), pasta.getText().strip(),
                branchEscolhida, p, e, permissoes, existente.tasks(), imagem);
    }
}
