package br.ucsal.educonnect.service;

import static br.ucsal.educonnect.service.Validacao.limitar;
import static br.ucsal.educonnect.service.Validacao.obrigatorio;

import br.ucsal.educonnect.exception.RecursoNaoEncontradoException;
import br.ucsal.educonnect.exception.RegraNegocioException;
import br.ucsal.educonnect.model.Aluno;
import br.ucsal.educonnect.model.Curso;
import br.ucsal.educonnect.model.Disciplina;
import br.ucsal.educonnect.model.Professor;
import br.ucsal.educonnect.model.Turma;
import br.ucsal.educonnect.repository.AlunoRepository;
import br.ucsal.educonnect.repository.CursoRepository;
import br.ucsal.educonnect.repository.DisciplinaRepository;
import br.ucsal.educonnect.repository.ProfessorRepository;
import br.ucsal.educonnect.repository.TurmaRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF03 (cursos e disciplinas) e RF04 (turmas). */
@Service
public class CadastroService {

    private final CursoRepository cursos;
    private final DisciplinaRepository disciplinas;
    private final TurmaRepository turmas;
    private final AlunoRepository alunos;
    private final ProfessorRepository professores;

    public CadastroService(CursoRepository cursos, DisciplinaRepository disciplinas, TurmaRepository turmas,
                           AlunoRepository alunos, ProfessorRepository professores) {
        this.cursos = cursos;
        this.disciplinas = disciplinas;
        this.turmas = turmas;
        this.alunos = alunos;
        this.professores = professores;
    }

    // ---------- consultas ----------

    @Transactional(readOnly = true)
    public List<Curso> listarCursos() { return cursos.findAllByOrderByNomeAsc(); }

    @Transactional(readOnly = true)
    public List<Disciplina> listarDisciplinas() { return disciplinas.findAllByOrderByNomeAsc(); }

    @Transactional(readOnly = true)
    public List<Turma> listarTurmas() { return turmas.findAllByOrderByPeriodoDescCodigoAsc(); }

    @Transactional(readOnly = true)
    public List<Aluno> alunosAtivos() { return alunos.findByAtivoTrueOrderByNomeCompletoAsc(); }

    @Transactional(readOnly = true)
    public List<Professor> professoresAtivos() { return professores.findByAtivoTrueOrderByNomeCompletoAsc(); }

    @Transactional(readOnly = true)
    public Turma buscarTurma(Long id) {
        return turmas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
    }

    /** Garante que o professor só acessa as suas próprias turmas (RNF-S.2). */
    @Transactional(readOnly = true)
    public Turma turmaDoProfessor(Long professorId, Long turmaId) {
        Turma t = buscarTurma(turmaId);
        if (t.getProfessor() == null || !t.getProfessor().getId().equals(professorId)) {
            throw new AccessDeniedException("Turma pertence a outro professor.");
        }
        return t;
    }

    @Transactional(readOnly = true)
    public List<Turma> turmasDoProfessor(Long professorId) {
        return turmas.findByProfessorIdOrderByPeriodoDescCodigoAsc(professorId);
    }

    // ---------- RF03 ----------

    @Transactional
    public Curso criarCurso(String nome) {
        obrigatorio(nome, "Nome do curso");
        limitar(nome, "Nome do curso", 150);
        if (cursos.existsByNomeIgnoreCase(nome.trim())) {
            throw new RegraNegocioException("Já existe um curso com o nome \"" + nome.trim() + "\".");
        }
        Curso c = new Curso();
        c.setNome(nome.trim());
        return cursos.save(c);
    }

    @Transactional
    public Disciplina criarDisciplina(Long cursoId, String nome, String codigo, String cargaHoraria) {
        Curso curso = cursos.findById(cursoId == null ? -1L : cursoId)
                .orElseThrow(() -> new RegraNegocioException("Selecione o curso da disciplina."));
        obrigatorio(nome, "Nome da disciplina");
        limitar(nome, "Nome da disciplina", 150);
        String cod = obrigatorio(codigo, "Código");
        limitar(cod, "Código", 30);
        int carga = inteiroPositivo(cargaHoraria, "Carga horária");
        if (disciplinas.existsByCodigoIgnoreCase(cod)) {
            throw new RegraNegocioException("O código de disciplina \"" + cod + "\" já está cadastrado.");
        }
        Disciplina d = new Disciplina();
        d.setNome(nome.trim());
        d.setCodigo(cod);
        d.setCargaHoraria(carga);
        d.setCurso(curso);
        return disciplinas.save(d);
    }

    // ---------- RF04 ----------

    @Transactional
    public Turma criarTurma(Long disciplinaId, Long professorId, String codigo, String periodo) {
        Disciplina disciplina = disciplinas.findById(disciplinaId == null ? -1L : disciplinaId)
                .orElseThrow(() -> new RegraNegocioException("Selecione a disciplina da turma."));
        Professor professor = professorAtivo(professorId);
        String cod = obrigatorio(codigo, "Código da turma");
        limitar(cod, "Código da turma", 30);
        String per = obrigatorio(periodo, "Período");
        limitar(per, "Período", 20);
        if (turmas.existsByDisciplinaIdAndCodigoIgnoreCaseAndPeriodo(disciplina.getId(), cod, per)) {
            throw new RegraNegocioException("Já existe a turma " + cod + " de " + disciplina.getNome()
                    + " no período " + per + ".");
        }
        Turma t = new Turma();
        t.setDisciplina(disciplina);
        t.setCodigo(cod);
        t.setPeriodo(per);
        t.vincularProfessor(professor);
        return turmas.save(t);
    }

    @Transactional
    public void vincularAluno(Long turmaId, Long alunoId) {
        Turma turma = buscarTurma(turmaId);
        Aluno aluno = alunos.findById(alunoId == null ? -1L : alunoId)
                .filter(Aluno::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("Selecione um aluno ativo para vincular."));
        if (turma.isEncerrada()) {
            throw new RegraNegocioException("A turma está encerrada e não aceita novos alunos.");
        }
        turma.vincularAluno(aluno);
    }

    @Transactional
    public void vincularProfessor(Long turmaId, Long professorId) {
        buscarTurma(turmaId).vincularProfessor(professorAtivo(professorId));
    }

    @Transactional
    public void encerrarTurma(Long turmaId) {
        buscarTurma(turmaId).setEncerrada(true);
    }

    private Professor professorAtivo(Long id) {
        return professores.findById(id == null ? -1L : id)
                .filter(Professor::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("Selecione um professor ativo."));
    }

    private int inteiroPositivo(String texto, String campo) {
        try {
            int v = Integer.parseInt(obrigatorio(texto, campo));
            if (v <= 0) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Informe um número inteiro maior que zero em \"" + campo + "\".");
        }
    }
}
