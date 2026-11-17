package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByIdentificadorInstitucionalIgnoreCase(String identificador);

    Optional<Usuario> findFirstByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByIdentificadorInstitucionalIgnoreCase(String identificador);

    List<Usuario> findAllByOrderByNomeCompletoAsc();
}
