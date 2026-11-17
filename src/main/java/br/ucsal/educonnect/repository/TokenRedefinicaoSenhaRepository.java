package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.TokenRedefinicaoSenha;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRedefinicaoSenhaRepository extends JpaRepository<TokenRedefinicaoSenha, Long> {

    Optional<TokenRedefinicaoSenha> findByTokenHash(String tokenHash);
}
