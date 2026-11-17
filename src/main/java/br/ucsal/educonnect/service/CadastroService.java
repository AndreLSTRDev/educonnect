package br.ucsal.educonnect.service;

import static br.ucsal.educonnect.service.Validacao.limitar;
import static br.ucsal.educonnect.service.Validacao.obrigatorio;
import static br.ucsal.educonnect.service.Validacao.opcional;

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
import br.ucsal.educonnect.repository.FrequenciaRepository;
import br.ucsal.educonnect.repository.NotaRepository;
import br.ucsal.educonnect.repository.ProfessorRepository;
import br.ucsal.educonnect.repository.TurmaRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF03 (cursos e disciplinas) e RF04 (turmas), com os ajustes pedidos na validação com o cliente. */
@Service
public class CadastroService {

    private final CursoRepository cursos;
    private final DisciplinaRepository disciplinas;
    private final TurmaRepository turmas;
    private final AlunoRepository alunos;
    private final ProfessorRepository professores;
    private final NotaRepository notas;
    private final FrequenciaRepository frequencias;

    public CadastroService(CursoRepository cursos, DisciplinaRepository disciplinas, TurmaRepository turmas,
                           AlunoRepository alunos, ProfessorRepository professores, NotaRepository notas,
                           FrequenciaRepository frequencias) {
        this.cursos = cursos;
        this.disciplinas = disciplinas;
        this.turmas = turmas;
        this.alunos = alunos;
        this.professores = professores;
        this.notas = notas;
        this.frequencias = frequencias;
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

    @Transactional(readOnly = true)
    public Curso buscarCurso(Long id) {
        return cursos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado."));
    }

    @Transactional(readOnly = true)
    public Disciplina buscarDisciplina(Long id) {
        return disciplinas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina não encontrada."));
    }

    /** Garante que o professor só acessa as turmas em que leciona (RNF-S.2). */
    @Transactional(readOnly = true)
    public Turma turmaDoProfessor(Long professorId, Long turmaId) {
        Turma t = buscarTurma(turmaId);
        if (!t.temProfessor(professorId)) {
            throw new AccessDeniedException("Turma pertence a outro professor.");
        }
        return t;
    }

    @Transactional(readOnly = true)
    public List<Turma> turmasDoProfessor(Long professorId) {
        return turmas.findDoProfessor(professorId);
    }

    // ---------- RF03: cursos ----------

    @Transactional
    public Curso criarCurso(String nome) {
        String n = validarNomeCurso(nome, null);
        Curso c = new Curso();
        c.setNome(n);
        return cursos.save(c);
    }

    @Transactional
    public void renomearCurso(Long id, String nome) {
        buscarCurso(id).setNome(validarNomeCurso(nome, id));
    }

    @Transactional
    public void excluirCurso(Long id) {
        Curso c = buscarCurso(id);
        if (disciplinas.countByCursoId(id) > 0) {
            throw new RegraNegocioException("O curso \"" + c.getNome()
                    + "\" ainda tem disciplinas. Exclua ou mova as disciplinas antes de excluir o curso.");
        }
        cursos.delete(c);
    }

    private String validarNomeCurso(String nome, Long idAtual) {
        obrigatorio(nome, "Nome do curso");
        limitar(nome, "Nome do curso", 150);
        String n = nome.trim();
        boolean repetido = cursos.findAllByOrderByNomeAsc().stream()
                .anyMatch(c -> c.getNome().equalsIgnoreCase(n) && !c.getId().equals(idAtual));
        if (repetido) {
            throw new RegraNegocioException("Já existe um curso com o nome \"" + n + "\".");
        }
        return n;
    }

    // ---------- RF03: disciplinas ----------

    @Transactional
    public Disciplina criarDisciplina(Long cursoId, String nome, String codigo, String cargaHoraria) {
        return criarDisciplina(cursoId, nome, codigo, cargaHoraria, null, List.of());
    }

    /** Sobrecarga com ementa e pré-requisitos (pedido do cliente). */
    @Transactional
    public Disciplina criarDisciplina(Long cursoId, String nome, String codigo, String cargaHoraria,
                                      String ementa, Collection<Long> preRequisitos) {
        Disciplina d = new Disciplina();
        preencherDisciplina(d, cursoId, nome, codigo, cargaHoraria, ementa, preRequisitos);
        return disciplinas.save(d);
    }

    @Transactional
    public void atualizarDisciplina(Long id, Long cursoId, String nome, String codigo, String cargaHoraria,
                                    String ementa, Collection<Long> preRequisitos) {
        preencherDisciplina(buscarDisciplina(id), cursoId, nome, codigo, cargaHoraria, ementa, preRequisitos);
    }

    @Transactional
    public void excluirDisciplina(Long id) {
        Disciplina d = buscarDisciplina(id);
        if (turmas.countByDisciplinaId(id) > 0) {
            throw new RegraNegocioException("A disciplina " + d.getCodigo()
                    + " tem turmas cadastradas e não pode ser excluída (o histórico dos alunos seria perdido).");
        }
        if (disciplinas.contarComoPreRequisito(id) > 0) {
            throw new RegraNegocioException("A disciplina " + d.getCodigo()
                    + " é pré-requisito de outra disciplina. Retire-a dos pré-requisitos antes de excluir.");
        }
        d.getCurso().getDisciplinas().remove(d);
        disciplinas.delete(d);
    }

    private void preencherDisciplina(Disciplina d, Long cursoId, String nome, String codigo, String cargaHoraria,
                                     String ementa, Collection<Long> preRequisitos) {
        Curso curso = cursos.findById(cursoId == null ? -1L : cursoId)
                .orElseThrow(() -> new RegraNegocioException("Selecione o curso da disciplina."));
        obrigatorio(nome, "Nome da disciplina");
        limitar(nome, "Nome da disciplina", 150);
        String cod = obrigatorio(codigo, "Código");
        limitar(cod, "Código", 30);
        int carga = inteiroPositivo(cargaHoraria, "Carga horária");
        limitar(ementa, "Ementa", 2000);
        boolean codigoRepetido = disciplinas.findAllByOrderByNomeAsc().stream()
                .anyMatch(o -> o.getCodigo().equalsIgnoreCase(cod) && !o.getId().equals(d.getId()));
        if (codigoRepetido) {
            throw new RegraNegocioException("O código de disciplina \"" + cod + "\" já está cadastrado.");
        }
        d.setNome(nome.trim());
        d.setCodigo(cod);
        d.setCargaHoraria(carga);
        d.setEmenta(opcional(ementa));
        d.setCurso(curso);
        d.getPreRequisitos().clear();
        if (preRequisitos != null) {
            for (Long preId : preRequisitos) {
                if (preId.equals(d.getId())) {
                    throw new RegraNegocioException("Uma disciplina não pode ser pré-requisito dela mesma.");
                }
                d.getPreRequisitos().add(buscarDisciplina(preId));
            }
        }
    }

    // ---------- RF04 ----------

    @Transactional
    public Turma criarTurma(Long disciplinaId, Long professorId, String codigo, String periodo) {
        return criarTurma(disciplinaId, professorId, codigo, periodo, null);
    }

    /** Sobrecarga com a sala (pedido do cliente). */
    @Transactional
    public Turma criarTurma(Long disciplinaId, Long professorId, String codigo, String periodo, String sala) {
        Disciplina disciplina = disciplinas.findById(disciplinaId == null ? -1L : disciplinaId)
                .orElseThrow(() -> new RegraNegocioException("Selecione a disciplina da turma."));
        Professor professor = professorAtivo(professorId);
        String cod = obrigatorio(codigo, "Código da turma");
        limitar(cod, "Código da turma", 30);
        String per = obrigatorio(periodo, "Período");
        limitar(per, "Período", 20);
        limitar(sala, "Sala", 60);
        if (turmas.existsByDisciplinaIdAndCodigoIgnoreCaseAndPeriodo(disciplina.getId(), cod, per)) {
            throw new RegraNegocioException("Já existe a turma " + cod + " de " + disciplina.getNome()
                    + " no período " + per + ".");
        }
        Turma t = new Turma();
        t.setDisciplina(disciplina);
        t.setCodigo(cod);
        t.setPeriodo(per);
        t.setSala(opcional(sala));
        t.vincularProfessor(professor);
        return turmas.save(t);
    }

    @Transactional
    public void atualizarSala(Long turmaId, String sala) {
        limitar(sala, "Sala", 60);
        buscarTurma(turmaId).setSala(opcional(sala));
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

    /** Pedido do cliente: remover aluno vinculado por engano, desde que ainda não tenha notas nem frequência. */
    @Transactional
    public void removerAluno(Long turmaId, Long alunoId) {
        Turma turma = buscarTurma(turmaId);
        Aluno aluno = turma.getAlunos().stream().filter(a -> a.getId().equals(alunoId)).findFirst()
                .orElseThrow(() -> new RegraNegocioException("O aluno não está vinculado a esta turma."));
        if (turma.isEncerrada()) {
            throw new RegraNegocioException("A turma está encerrada; o aluno não pode mais ser removido.");
        }
        if (!notas.findByAlunoETurma(alunoId, turmaId).isEmpty()
                || frequencias.countByTurmaIdAndAlunoId(turmaId, alunoId) > 0) {
            throw new RegraNegocioException("O aluno " + aluno.getNomeCompleto()
                    + " já tem notas ou frequência nesta turma e não pode ser removido.");
        }
        turma.removerAluno(aluno);
    }

    @Transactional
    public void vincularProfessor(Long turmaId, Long professorId) {
        buscarTurma(turmaId).vincularProfessor(professorAtivo(professorId));
    }

    /** Pedido do cliente: a turma pode ter mais de um professor. */
    @Transactional
    public void adicionarProfessor(Long turmaId, Long professorId) {
        buscarTurma(turmaId).adicionarProfessor(professorAtivo(professorId));
    }

    @Transactional
    public void removerProfessor(Long turmaId, Long professorId) {
        Turma turma = buscarTurma(turmaId);
        turma.getProfessoresAdicionais().stream().filter(p -> p.getId().equals(professorId)).findFirst()
                .ifPresent(turma::removerProfessor);
    }

    /** Pedido do cliente: só o professor da turma encerra a turma (define aprovado/reprovado). */
    @Transactional
    public void encerrarTurma(Long professorId, Long turmaId) {
        Turma turma = turmaDoProfessor(professorId, turmaId);
        if (turma.isEncerrada()) {
            throw new RegraNegocioException("A turma já está encerrada.");
        }
        turma.setEncerrada(true);
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
