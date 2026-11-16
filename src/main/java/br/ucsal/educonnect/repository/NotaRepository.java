package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.Nota;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotaRepository extends JpaRepository<Nota, Long> {

    Optional<Nota> findByAvaliacaoIdAndAlunoId(Long avaliacaoId, Long alunoId);

    List<Nota> findByAvaliacaoId(Long avaliacaoId);

    @Query("select n from Nota n where n.aluno.id = :alunoId and n.avaliacao.turma.id = :turmaId")
    List<Nota> findByAlunoETurma(@Param("alunoId") Long alunoId, @Param("turmaId") Long turmaId);
}
