package br.ucsal.educonnect.service;

import static br.ucsal.educonnect.service.Validacao.limitar;
import static br.ucsal.educonnect.service.Validacao.obrigatorio;
import static br.ucsal.educonnect.service.Validacao.opcional;

import br.ucsal.educonnect.exception.RecursoNaoEncontradoException;
import br.ucsal.educonnect.exception.RegraNegocioException;
import br.ucsal.educonnect.model.Aluno;
import br.ucsal.educonnect.model.CategoriaDenuncia;
import br.ucsal.educonnect.model.Denuncia;
import br.ucsal.educonnect.model.HistoricoDenuncia;
import br.ucsal.educonnect.model.StatusDenuncia;
import br.ucsal.educonnect.model.Usuario;
import br.ucsal.educonnect.repository.AlunoRepository;
import br.ucsal.educonnect.repository.DenunciaRepository;
import br.ucsal.educonnect.repository.UsuarioRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF09 (registro) e RF10 (gerenciamento) de denúncias. */
@Service
public class DenunciaService {

    private static final Logger log = LoggerFactory.getLogger(DenunciaService.class);

    private final DenunciaRepository denuncias;
    private final AlunoRepository alunos;
    private final UsuarioRepository usuarios;

    public DenunciaService(DenunciaRepository denuncias, AlunoRepository alunos, UsuarioRepository usuarios) {
        this.denuncias = denuncias;
        this.alunos = alunos;
        this.usuarios = usuarios;
    }

    /** RF09: registra a denúncia e gera o protocolo. */
    @Transactional
    public Denuncia registrar(Long alunoId, String titulo, CategoriaDenuncia categoria, String descricao) {
        Aluno aluno = alunos.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        obrigatorio(titulo, "Título");
        limitar(titulo, "Título", 120);
        if (categoria == null) {
            throw new RegraNegocioException("Selecione a categoria da denúncia.");
        }
        obrigatorio(descricao, "Descrição");
        limitar(descricao, "Descrição", 2000);

        Denuncia d = new Denuncia();
        d.setAluno(aluno);
        d.setTitulo(titulo.trim());
        d.setCategoria(categoria);
        d.setDescricao(descricao.trim());
        do {
            d.gerarProtocolo();
        } while (denuncias.existsByProtocolo(d.getProtocolo()));

        HistoricoDenuncia h = new HistoricoDenuncia();
        h.setDenuncia(d);
        h.setStatusNovo(StatusDenuncia.ABERTA);
        h.setComentario("Denúncia registrada pelo aluno.");
        d.getHistorico().add(h);

        Denuncia salva = denuncias.save(d);
        log.info("Denúncia {} registrada", salva.getProtocolo());
        return salva;
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
        return filtro == null
                ? denuncias.findAllByOrderByDataAberturaDesc()
                : denuncias.findByStatusOrderByDataAberturaDesc(filtro);
    }

    @Transactional(readOnly = true)
    public Denuncia buscar(Long id) {
        return denuncias.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Denúncia não encontrada."));
    }

    /** RF10: altera o status e registra a mudança no histórico. */
    @Transactional
    public Denuncia atualizarStatus(Long gestorId, Long denunciaId, StatusDenuncia novoStatus, String comentario) {
        Denuncia d = buscar(denunciaId);
        if (novoStatus == null) {
            throw new RegraNegocioException("Selecione o novo status da denúncia.");
        }
        if (novoStatus == d.getStatus()) {
            throw new RegraNegocioException("A denúncia já está com o status \"" + novoStatus.getRotulo() + "\".");
        }
        limitar(comentario, "Comentário", 500);
        Usuario gestor = usuarios.findById(gestorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor não encontrado."));

        HistoricoDenuncia h = new HistoricoDenuncia();
        h.setDenuncia(d);
        h.setStatusAnterior(d.getStatus());
        h.setStatusNovo(novoStatus);
        h.setResponsavel(gestor);
        h.setComentario(opcional(comentario));
        d.getHistorico().add(h);
        d.atualizarStatus(novoStatus);
        log.info("Denúncia {}: {} -> {} por {}", d.getProtocolo(), h.getStatusAnterior(), novoStatus,
                gestor.getIdentificadorInstitucional());
        return d;
    }
}
