package br.ucsal.educonnect.security;

import br.ucsal.educonnect.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        String chave = identificador == null ? "" : identificador.trim();
        // Pedido do cliente (validação do RF01): entrar com o identificador institucional ou com o e-mail.
        return usuarioRepository.findByIdentificadorInstitucionalIgnoreCase(chave)
                .or(() -> usuarioRepository.findFirstByEmailIgnoreCase(chave))
                .map(UsuarioLogado::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
