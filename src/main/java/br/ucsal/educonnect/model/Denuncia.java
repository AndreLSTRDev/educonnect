package br.ucsal.educonnect.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "denuncias")
public class Denuncia {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String protocolo;

    @Column(nullable = false, length = 120)
    private String titulo;

    /** Gravada como texto (não como ENUM do banco) para aceitar novas categorias sem migração. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private CategoriaDenuncia categoria;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDenuncia status = StatusDenuncia.ABERTA;

    @Column(nullable = false)
    private LocalDateTime dataAbertura = LocalDateTime.now();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    /** Pedido do cliente (validação do RF09): o gestor não vê quem denunciou. O aluno continua
     *  vinculado para poder acompanhar a própria denúncia. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean anonima;

    /** Pedido do cliente (validação do RF09): fotos e documentos anexados. */
    @OneToMany(mappedBy = "denuncia", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<AnexoDenuncia> anexos = new ArrayList<>();

    /** RF10: cada alteração de status fica registrada. */
    @OneToMany(mappedBy = "denuncia", cascade = CascadeType.ALL)
    @OrderBy("dataAlteracao ASC, id ASC")
    private List<HistoricoDenuncia> historico = new ArrayList<>();

    /** Gera e atribui um protocolo no formato EDU-AAAA-NNNNNN (RF09). */
    public String gerarProtocolo() {
        this.protocolo = String.format("EDU-%d-%06d", LocalDateTime.now().getYear(), RANDOM.nextInt(1_000_000));
        return this.protocolo;
    }

    public void atualizarStatus(StatusDenuncia novoStatus) {
        this.status = novoStatus;
    }

    public Long getId() { return id; }
    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public CategoriaDenuncia getCategoria() { return categoria; }
    public void setCategoria(CategoriaDenuncia categoria) { this.categoria = categoria; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public StatusDenuncia getStatus() { return status; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public boolean isAnonima() { return anonima; }
    public void setAnonima(boolean anonima) { this.anonima = anonima; }
    public List<AnexoDenuncia> getAnexos() { return anexos; }

    /** Nome exibido ao gestor: "Anônimo" quando o aluno pediu sigilo. */
    public String getAutorExibido() { return anonima ? "Anônimo" : aluno.getNomeCompleto(); }
    public List<HistoricoDenuncia> getHistorico() { return historico; }
}
