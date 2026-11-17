package br.ucsal.educonnect.config;

import br.ucsal.educonnect.model.Curso;
import br.ucsal.educonnect.model.Disciplina;
import br.ucsal.educonnect.model.Perfil;
import br.ucsal.educonnect.model.Turma;
import br.ucsal.educonnect.repository.UsuarioRepository;
import br.ucsal.educonnect.service.CadastroService;
import br.ucsal.educonnect.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Cria usuários de demonstração apenas quando o banco está vazio. Troque as senhas em produção. */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository repository;
    private final UsuarioService usuarios;
    private final CadastroService cadastro;
    private final boolean habilitado;
    private final String senha;

    public DataInitializer(UsuarioRepository repository, UsuarioService usuarios, CadastroService cadastro,
                           @Value("${educonnect.seed.enabled:false}") boolean habilitado,
                           @Value("${educonnect.seed.senha:Edu@12345}") String senha) {
        this.repository = repository;
        this.usuarios = usuarios;
        this.cadastro = cadastro;
        this.habilitado = habilitado;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (!habilitado || repository.count() > 0) {
            return;
        }
        usuarios.cadastrar(Perfil.ADMINISTRADOR, "Administrador Geral", "admin", "admin@educonnect.local", senha, null);
        var prof = usuarios.cadastrar(Perfil.PROFESSOR, "Prof. Elton Figueiredo da Silva", "prof", "prof@educonnect.local", senha, "RF-0001");
        var aluno1 = usuarios.cadastrar(Perfil.ALUNO, "Gabriel Oliveira Leônidas", "aluno1", "aluno1@educonnect.local", senha, "2026001");
        var aluno2 = usuarios.cadastrar(Perfil.ALUNO, "Thiago Santos", "aluno2", "aluno2@educonnect.local", senha, "2026002");
        usuarios.cadastrar(Perfil.GESTOR_DENUNCIAS, "Gestor de Denúncias", "gestor", "gestor@educonnect.local", senha, null);

        Curso curso = cadastro.criarCurso("Engenharia de Software");
        Disciplina disc = cadastro.criarDisciplina(curso.getId(), "Engenharia de Requisitos", "ESW101", "60");
        Turma turma = cadastro.criarTurma(disc.getId(), prof.getId(), "T01", "2026.1", "B-204");
        cadastro.vincularAluno(turma.getId(), aluno1.getId());
        cadastro.vincularAluno(turma.getId(), aluno2.getId());
        log.info("Dados de demonstração criados (logins: admin, prof, aluno1, aluno2, gestor).");
    }
}
