package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.Disciplina;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Disciplina> findAllByOrderByNomeAsc();

    long countByCursoId(Long cursoId);

    @org.springframework.data.jpa.repository.Query("select count(d) from Disciplina d join d.preRequisitos p where p.id = :id")
    long contarComoPreRequisito(@org.springframework.data.repository.query.Param("id") Long id);
}
