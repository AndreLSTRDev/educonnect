package br.ucsal.educonnect.service;

import static br.ucsal.educonnect.service.Validacao.limitar;
import static br.ucsal.educonnect.service.Validacao.obrigatorio;
import static br.ucsal.educonnect.service.Validacao.opcional;

import br.ucsal.educonnect.exception.RecursoNaoEncontradoException;
import br.ucsal.educonnect.exception.RegraNegocioException;
import br.ucsal.educonnect.model.Administrador;
import br.ucsal.educonnect.model.Aluno;
import br.ucsal.educonnect.model.GestorDenuncias;
import br.ucsal.educonnect.model.Perfil;
import br.ucsal.educonnect.model.Professor;
import br.ucsal.educonnect.model.Usuario;
import br.ucsal.educonnect.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF02 - Cadastro de usuários. */
@Service
public class UsuarioService {

    private static final int TAMANHO_MINIMO_SENHA = 8;

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repository.findAllByOrderByNomeCompletoAsc();
    }

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    @Transactional
    public Usuario cadastrar(Perfil perfil, String nome, String identificador, String email,
                             String senha, String complemento) {
        if (perfil == null) {
            throw new RegraNegocioException("Selecione o perfil do usuário.");
        }
        String idf = validarIdentificacao(nome, identificador, email);
        validarSenha(senha);
        if (repository.existsByIdentificadorInstitucionalIgnoreCase(idf)) {
            throw new RegraNegocioException("O identificador institucional \"" + idf
                    + "\" já está em uso. Informe um identificador diferente.");
        }
        Usuario u = switch (perfil) {
            case ALUNO -> new Aluno();
            case PROFESSOR -> new Professor();
            case ADMINISTRADOR -> new Administrador();
            case GESTOR_DENUNCIAS -> new GestorDenuncias();
        };
        u.setNomeCompleto(nome.trim());
        u.setIdentificadorInstitucional(idf);
        u.setEmail(opcional(email));
        u.setSenhaHash(encoder.encode(senha));
        aplicarComplemento(u, complemento, idf);
        return repository.save(u);
    }

    @Transactional
    public Usuario atualizar(Long id, String nome, String identificador, String email,
                             String novaSenha, String complemento) {
        Usuario u = buscar(id);
        String idf = validarIdentificacao(nome, identificador, email);
        repository.findByIdentificadorInstitucionalIgnoreCase(idf)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegraNegocioException("O identificador institucional \"" + idf
                            + "\" já pertence a outro usuário.");
                });
        u.setNomeCompleto(nome.trim());
        u.setIdentificadorInstitucional(idf);
        u.setEmail(opcional(email));
        if (novaSenha != null && !novaSenha.isBlank()) {
            validarSenha(novaSenha);
            u.setSenhaHash(encoder.encode(novaSenha));
        }
        aplicarComplemento(u, complemento, idf);
        return u;
    }

    /** RF02: desativar (e reativar) usuários, sem apagar o histórico. */
    @Transactional
    public void alterarStatus(Long id, boolean ativo, Long executorId) {
        if (!ativo && id.equals(executorId)) {
            throw new RegraNegocioException("Você não pode desativar o seu próprio usuário.");
        }
        buscar(id).setAtivo(ativo);
    }

    private String validarIdentificacao(String nome, String identificador, String email) {
        obrigatorio(nome, "Nome completo");
        limitar(nome, "Nome completo", 150);
        String idf = obrigatorio(identificador, "Identificador institucional");
        limitar(idf, "Identificador institucional", 40);
        limitar(email, "E-mail", 150);
        if (email != null && !email.isBlank() && !email.contains("@")) {
            throw new RegraNegocioException("Informe um e-mail válido (exemplo: nome@instituicao.br).");
        }
        return idf;
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new RegraNegocioException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
    }

    private void aplicarComplemento(Usuario u, String complemento, String identificador) {
        String valor = opcional(complemento);
        if (u instanceof Aluno a) {
            a.setMatricula(valor != null ? valor : identificador);
        } else if (u instanceof Professor p) {
            p.setRegistroFuncional(valor != null ? valor : identificador);
        }
    }
}
