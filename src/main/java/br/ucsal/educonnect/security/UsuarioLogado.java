package br.ucsal.educonnect.security;

import br.ucsal.educonnect.model.Perfil;
import br.ucsal.educonnect.model.Usuario;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Usuário autenticado guardado na sessão (RF01/RNF-S.2). */
public class UsuarioLogado implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String identificador;
    private final String senhaHash;
    private final String nome;
    private final Perfil perfil;
    private final boolean ativo;

    public UsuarioLogado(Usuario u) {
        this.id = u.getId();
        this.identificador = u.getIdentificadorInstitucional();
        this.senhaHash = u.getSenhaHash();
        this.nome = u.getNomeCompleto();
        this.perfil = u.getPerfil();
        this.ativo = u.isAtivo();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Perfil getPerfil() { return perfil; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override public String getPassword() { return senhaHash; }
    @Override public String getUsername() { return identificador; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return ativo; }
}
