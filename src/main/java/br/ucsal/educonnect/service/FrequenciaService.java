package br.ucsal.educonnect.service;

import br.ucsal.educonnect.exception.RegraNegocioException;
import br.ucsal.educonnect.model.Aluno;
import br.ucsal.educonnect.model.RegistroFrequencia;
import br.ucsal.educonnect.model.Turma;
import br.ucsal.educonnect.repository.FrequenciaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF06 - Frequência. */
@Service
public class FrequenciaService {

    private final FrequenciaRepository repository;
    private final CadastroService cadastro;

    public FrequenciaService(FrequenciaRepository repository, CadastroService cadastro) {
        this.repository = repository;
        this.cadastro = cadastro;
    }

    /** Registra presença (ids marcados) e ausência (demais alunos da turma) para a data. */
    @Transactional
    public void registrar(Long professorId, Long turmaId, LocalDate data, Collection<Long> presentes) {
        Turma turma = cadastro.turmaDoProfessor(professorId, turmaId);
        if (data == null) {
            throw new RegraNegocioException("Informe a data da aula.");
        }
        if (data.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("Não é possível registrar frequência em uma data futura.");
        }
        if (turma.getAlunos().isEmpty()) {
            throw new RegraNegocioException("A turma ainda não possui alunos vinculados.");
        }
        for (Aluno aluno : turma.getAlunos()) {
            RegistroFrequencia r = repository
                    .findByTurmaIdAndAlunoIdAndDataRegistro(turmaId, aluno.getId(), data)
                    .orElseGet(RegistroFrequencia::new);
            r.setTurma(turma);
            r.setAluno(aluno);
            r.setDataRegistro(data);
            r.setPresente(presentes != null && presentes.contains(aluno.getId()));
            repository.save(r);
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> presencasDoDia(Long turmaId, LocalDate data) {
        Map<Long, Boolean> mapa = new HashMap<>();
        repository.findByTurmaIdAndDataRegistro(turmaId, data)
                .forEach(r -> mapa.put(r.getAluno().getId(), r.isPresente()));
        return mapa;
    }

    /** Percentual de presença do aluno na turma (nulo se ainda não há registros). */
    @Transactional(readOnly = true)
    public Double percentual(Long turmaId, Long alunoId) {
        long total = repository.countByTurmaIdAndAlunoId(turmaId, alunoId);
        if (total == 0) {
            return null;
        }
        long presencas = repository.countByTurmaIdAndAlunoIdAndPresenteTrue(turmaId, alunoId);
        return 100.0 * presencas / total;
    }
}
