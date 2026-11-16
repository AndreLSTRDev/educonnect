package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.Denuncia;
import br.ucsal.educonnect.model.StatusDenuncia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {

    boolean existsByProtocolo(String protocolo);

    Optional<Denuncia> findByProtocolo(String protocolo);

    List<Denuncia> findByAlunoIdOrderByDataAberturaDesc(Long alunoId);

    List<Denuncia> findAllByOrderByDataAberturaDesc();

    List<Denuncia> findByStatusOrderByDataAberturaDesc(StatusDenuncia status);
}
