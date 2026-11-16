package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.Professor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {

    List<Professor> findByAtivoTrueOrderByNomeCompletoAsc();
}
