package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.Disciplina;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Disciplina> findAllByOrderByNomeAsc();
}
