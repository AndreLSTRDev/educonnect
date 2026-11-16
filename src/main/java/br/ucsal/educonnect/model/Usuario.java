package br.ucsal.educonnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Superclasse de todos os perfis (herança do diagrama de classes). */
@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nomeCompleto;

    @Column(nullable = false, unique = true, length = 40)
    private String identificadorInstitucional;

    /** RNF-S.3: nunca armazenamos a senha em texto, apenas o hash BCrypt. */
    @Column(nullable = false)
    private String senhaHash;

    @Column(length = 150)
    private String email;

    private boolean ativo = true;

    public abstract Perfil getPerfil();

    /** Polimorfismo: cada subclasse devolve o caminho do seu painel. */
    public abstract String exibirPainel();

    public boolean autenticar(String senha, PasswordEncoder encoder) {
        return ativo && senha != null && encoder.matches(senha, senhaHash);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario outro)) return false;
        return getId() != null && getId().equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return Usuario.class.hashCode();
    }

    public Long getId() { return id; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getIdentificadorInstitucional() { return identificadorInstitucional; }
    public void setIdentificadorInstitucional(String v) { this.identificadorInstitucional = v; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
