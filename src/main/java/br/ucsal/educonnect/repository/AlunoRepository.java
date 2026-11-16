package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.Aluno;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    List<Aluno> findByAtivoTrueOrderByNomeCompletoAsc();
}
