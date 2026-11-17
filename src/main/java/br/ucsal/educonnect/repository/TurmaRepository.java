package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.Turma;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

    List<Turma> findAllByOrderByPeriodoDescCodigoAsc();

    /** Turmas em que o professor é o responsável ou um dos professores adicionais. */
    @Query("select distinct t from Turma t left join t.professoresAdicionais pa "
            + "where t.professor.id = :professorId or pa.id = :professorId "
            + "order by t.periodo desc, t.codigo asc")
    List<Turma> findDoProfessor(@Param("professorId") Long professorId);

    long countByDisciplinaId(Long disciplinaId);

    boolean existsByDisciplinaIdAndCodigoIgnoreCaseAndPeriodo(Long disciplinaId, String codigo, String periodo);

    /** Consultas parametrizadas (RNF-S.6). */
    @Query("select t from Turma t join t.alunos a where a.id = :alunoId")
    List<Turma> findByAlunoId(@Param("alunoId") Long alunoId);
}
