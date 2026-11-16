package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.Turma;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

    List<Turma> findAllByOrderByPeriodoDescCodigoAsc();

    List<Turma> findByProfessorIdOrderByPeriodoDescCodigoAsc(Long professorId);

    boolean existsByDisciplinaIdAndCodigoIgnoreCaseAndPeriodo(Long disciplinaId, String codigo, String periodo);

    /** Consultas parametrizadas (RNF-S.6). */
    @Query("select t from Turma t join t.alunos a where a.id = :alunoId")
    List<Turma> findByAlunoId(@Param("alunoId") Long alunoId);
}
