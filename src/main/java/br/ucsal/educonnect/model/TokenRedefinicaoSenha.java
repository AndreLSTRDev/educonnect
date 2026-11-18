package br.ucsal.educonnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Pedido do cliente (validação do RF01): link de uso único para redefinir a senha ("Esqueci minha senha"). */
@Entity
@Table(name = "tokens_redefinicao_senha")
public class TokenRedefinicaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Guardamos só o hash SHA-256 do token; o valor original vai apenas no link enviado. */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime expiraEm;

    private boolean usado;

    public boolean valido() {
        return !usado && LocalDateTime.now().isBefore(expiraEm);
    }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
