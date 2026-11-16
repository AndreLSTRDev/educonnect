package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByIdentificadorInstitucionalIgnoreCase(String identificador);

    boolean existsByIdentificadorInstitucionalIgnoreCase(String identificador);

    List<Usuario> findAllByOrderByNomeCompletoAsc();
}
