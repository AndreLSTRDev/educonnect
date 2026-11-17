package br.ucsal.educonnect.service;

import static br.ucsal.educonnect.service.Validacao.limitar;
import static br.ucsal.educonnect.service.Validacao.obrigatorio;

import br.ucsal.educonnect.exception.RecursoNaoEncontradoException;
import br.ucsal.educonnect.exception.RegraNegocioException;
import br.ucsal.educonnect.model.Aluno;
import br.ucsal.educonnect.model.AnexoDenuncia;
import br.ucsal.educonnect.model.CategoriaDenuncia;
import br.ucsal.educonnect.model.Denuncia;
import br.ucsal.educonnect.model.HistoricoDenuncia;
import br.ucsal.educonnect.model.StatusDenuncia;
import br.ucsal.educonnect.model.Usuario;
import br.ucsal.educonnect.repository.AlunoRepository;
import br.ucsal.educonnect.repository.AnexoDenunciaRepository;
import br.ucsal.educonnect.repository.DenunciaRepository;
import br.ucsal.educonnect.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF09 (registro) e RF10 (gerenciamento) de denúncias, com os ajustes pedidos na validação com o cliente. */
@Service
public class DenunciaService {

    private static final Logger log = LoggerFactory.getLogger(DenunciaService.class);

    static final int MAX_ANEXOS = 3;
    static final long MAX_BYTES_ANEXO = 5L * 1024 * 1024;
    static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf");

    /** Arquivo recebido do formulário, independente da API web. */
    public record ArquivoEnviado(String nome, String tipo, byte[] dados) { }

    /** Filtros da lista do gestor (pedido do cliente na validação do RF10). Campos nulos são ignorados. */
    public record FiltroDenuncia(StatusDenuncia status, CategoriaDenuncia categoria, LocalDate de, LocalDate ate,
                                 String aluno, String protocolo) {
        public static FiltroDenuncia vazio() { return new FiltroDenuncia(null, null, null, null, null, null); }
    }

    private final DenunciaRepository denuncias;
    private final AlunoRepository alunos;
    private final UsuarioRepository usuarios;
    private final AnexoDenunciaRepository anexos;
    private final NotificacaoService notificacao;

    public DenunciaService(DenunciaRepository denuncias, AlunoRepository alunos, UsuarioRepository usuarios,
                           AnexoDenunciaRepository anexos, NotificacaoService notificacao) {
        this.denuncias = denuncias;
        this.alunos = alunos;
        this.usuarios = usuarios;
        this.anexos = anexos;
        this.notificacao = notificacao;
    }

    /** RF09: registra a denúncia e gera o protocolo. */
    @Transactional
    public Denuncia registrar(Long alunoId, String titulo, CategoriaDenuncia categoria, String descricao) {
        return registrar(alunoId, titulo, categoria, descricao, false, List.of());
    }

    /** Sobrecarga com sigilo da identidade e anexos (pedidos do cliente). */
    @Transactional
    public Denuncia registrar(Long alunoId, String titulo, CategoriaDenuncia categoria, String descricao,
                              boolean anonima, List<ArquivoEnviado> arquivos) {
        Aluno aluno = alunos.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        obrigatorio(titulo, "Título");
        limitar(titulo, "Título", 120);
        if (categoria == null) {
            throw new RegraNegocioException("Selecione a categoria da denúncia.");
        }
        obrigatorio(descricao, "Descrição");
        limitar(descricao, "Descrição", 2000);
        List<ArquivoEnviado> validos = validarAnexos(arquivos);

        Denuncia d = new Denuncia();
        d.setAluno(aluno);
        d.setTitulo(titulo.trim());
        d.setCategoria(categoria);
        d.setDescricao(descricao.trim());
        d.setAnonima(anonima);
        do {
            d.gerarProtocolo();
        } while (denuncias.existsByProtocolo(d.getProtocolo()));

        for (ArquivoEnviado arq : validos) {
            AnexoDenuncia a = new AnexoDenuncia();
            a.setDenuncia(d);
            a.setNomeArquivo(nomeSeguro(arq.nome()));
            a.setTipoConteudo(arq.tipo());
            a.setTamanho(arq.dados().length);
            a.setDados(arq.dados());
            d.getAnexos().add(a);
        }

        HistoricoDenuncia h = new HistoricoDenuncia();
        h.setDenuncia(d);
        h.setStatusNovo(StatusDenuncia.ABERTA);
        h.setComentario(anonima ? "Denúncia registrada (identidade em sigilo)." : "Denúncia registrada pelo aluno.");
        d.getHistorico().add(h);

        Denuncia salva = denuncias.save(d);
        log.info("Denúncia {} registrada ({} anexo(s), anônima: {})", salva.getProtocolo(), validos.size(), anonima);
        return salva;
    }

    private List<ArquivoEnviado> validarAnexos(List<ArquivoEnviado> arquivos) {
        List<ArquivoEnviado> validos = arquivos == null ? List.of()
                : arquivos.stream().filter(a -> a != null && a.dados() != null && a.dados().length > 0).toList();
        if (validos.size() > MAX_ANEXOS) {
            throw new RegraNegocioException("Anexe no máximo " + MAX_ANEXOS + " arquivos.");
        }
        for (ArquivoEnviado a : validos) {
            if (a.dados().length > MAX_BYTES_ANEXO) {
                throw new RegraNegocioException("O arquivo \"" + a.nome() + "\" passa de 5 MB.");
            }
            String tipo = a.tipo() == null ? "" : a.tipo().toLowerCase(Locale.ROOT);
            if (!TIPOS_PERMITIDOS.contains(tipo)) {
                throw new RegraNegocioException("O arquivo \"" + a.nome()
                        + "\" não é permitido. Envie imagens (JPG, PNG, GIF, WEBP) ou PDF.");
            }
        }
        return validos;
    }

    private static String nomeSeguro(String nome) {
        String base = nome == null || nome.isBlank() ? "anexo" : nome.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();
        return base.length() > 200 ? base.substring(base.length() - 200) : base;
    }

    @Transactional(readOnly = true)
    public List<Denuncia> doAluno(Long alunoId) {
        return denuncias.findByAlunoIdOrderByDataAberturaDesc(alunoId);
    }

    /** O aluno só enxerga as próprias denúncias. */
    @Transactional(readOnly = true)
    public Denuncia doAluno(Long alunoId, Long denunciaId) {
        Denuncia d = buscar(denunciaId);
        if (!d.getAluno().getId().equals(alunoId)) {
            throw new AccessDeniedException("Denúncia pertence a outro aluno.");
        }
        return d;
    }

    @Transactional(readOnly = true)
    public List<Denuncia> listar(StatusDenuncia filtro) {
        return listar(new FiltroDenuncia(filtro, null, null, null, null, null));
    }

    /** RF10 com os filtros pedidos pelo cliente: status, categoria, período de abertura, aluno e protocolo. */
    @Transactional(readOnly = true)
    public List<Denuncia> listar(FiltroDenuncia f) {
        FiltroDenuncia filtro = f == null ? FiltroDenuncia.vazio() : f;
        String aluno = normalizar(filtro.aluno());
        String protocolo = normalizar(filtro.protocolo());
        return denuncias.findAllByOrderByDataAberturaDesc().stream()
                .filter(d -> filtro.status() == null || d.getStatus() == filtro.status())
                .filter(d -> filtro.categoria() == null || d.getCategoria() == filtro.categoria())
                .filter(d -> filtro.de() == null || !d.getDataAbertura().toLocalDate().isBefore(filtro.de()))
                .filter(d -> filtro.ate() == null || !d.getDataAbertura().toLocalDate().isAfter(filtro.ate()))
                // Denúncias anônimas não aparecem na busca por nome, para não revelar o autor.
                .filter(d -> aluno.isEmpty()
                        || (!d.isAnonima() && normalizar(d.getAluno().getNomeCompleto()).contains(aluno)))
                .filter(d -> protocolo.isEmpty() || normalizar(d.getProtocolo()).contains(protocolo))
                .toList();
    }

    private static String normalizar(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public Denuncia buscar(Long id) {
        return denuncias.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Denúncia não encontrada."));
    }

    /** Anexo para o gestor. */
    @Transactional(readOnly = true)
    public AnexoDenuncia anexo(Long anexoId) {
        AnexoDenuncia a = anexos.findById(anexoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anexo não encontrado."));
        a.getDados(); // carrega o conteúdo dentro da transação
        return a;
    }

    /** Anexo para o aluno: só das próprias denúncias. */
    @Transactional(readOnly = true)
    public AnexoDenuncia anexoDoAluno(Long alunoId, Long anexoId) {
        AnexoDenuncia a = anexo(anexoId);
        if (!a.getDenuncia().getAluno().getId().equals(alunoId)) {
            throw new AccessDeniedException("Anexo de denúncia de outro aluno.");
        }
        return a;
    }

    /** RF10: altera o status, registra a mudança no histórico e avisa o aluno por e-mail. */
    @Transactional
    public Denuncia atualizarStatus(Long gestorId, Long denunciaId, StatusDenuncia novoStatus, String comentario) {
        Denuncia d = buscar(denunciaId);
        if (novoStatus == null) {
            throw new RegraNegocioException("Selecione o novo status da denúncia.");
        }
        if (novoStatus == d.getStatus()) {
            throw new RegraNegocioException("A denúncia já está com o status \"" + novoStatus.getRotulo() + "\".");
        }
        // Pedido do cliente: o comentário passa a ser obrigatório em toda mudança de status.
        String texto = obrigatorio(comentario, "Comentário");
        limitar(texto, "Comentário", 500);
        Usuario gestor = usuarios.findById(gestorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor não encontrado."));

        HistoricoDenuncia h = new HistoricoDenuncia();
        h.setDenuncia(d);
        h.setStatusAnterior(d.getStatus());
        h.setStatusNovo(novoStatus);
        h.setResponsavel(gestor);
        h.setComentario(texto);
        d.getHistorico().add(h);
        d.atualizarStatus(novoStatus);
        log.info("Denúncia {}: {} -> {} por {}", d.getProtocolo(), h.getStatusAnterior(), novoStatus,
                gestor.getIdentificadorInstitucional());

        // Pedido do cliente: avisar o aluno por e-mail a cada mudança de status.
        notificacao.enviar(d.getAluno().getEmail(),
                "EduConnect: denúncia " + d.getProtocolo() + " agora está \"" + novoStatus.getRotulo() + "\"",
                "Olá, " + d.getAluno().getNomeCompleto() + ".\n\n"
                + "O status da sua denúncia \"" + d.getTitulo() + "\" (protocolo " + d.getProtocolo()
                + ") mudou de \"" + h.getStatusAnterior().getRotulo() + "\" para \"" + novoStatus.getRotulo() + "\".\n\n"
                + "Comentário do gestor: " + texto + "\n\n"
                + "Acompanhe pelo EduConnect, na área Denúncias.");
        return d;
    }
}
