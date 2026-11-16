package br.ucsal.educonnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_denuncias")
public class HistoricoDenuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "denuncia_id")
    private Denuncia denuncia;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusDenuncia statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDenuncia statusNovo;

    /** Gestor que fez a alteração (nulo no registro inicial feito pelo aluno). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @Column(length = 500)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime dataAlteracao = LocalDateTime.now();

    public Long getId() { return id; }
    public Denuncia getDenuncia() { return denuncia; }
    public void setDenuncia(Denuncia denuncia) { this.denuncia = denuncia; }
    public StatusDenuncia getStatusAnterior() { return statusAnterior; }
    public void setStatusAnterior(StatusDenuncia statusAnterior) { this.statusAnterior = statusAnterior; }
    public StatusDenuncia getStatusNovo() { return statusNovo; }
    public void setStatusNovo(StatusDenuncia statusNovo) { this.statusNovo = statusNovo; }
    public Usuario getResponsavel() { return responsavel; }
    public void setResponsavel(Usuario responsavel) { this.responsavel = responsavel; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public LocalDateTime getDataAlteracao() { return dataAlteracao; }
}
