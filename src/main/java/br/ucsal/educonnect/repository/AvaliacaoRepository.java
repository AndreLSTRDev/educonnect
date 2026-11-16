package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.Avaliacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByTurmaIdOrderByDataAvaliacaoAscIdAsc(Long turmaId);
}
