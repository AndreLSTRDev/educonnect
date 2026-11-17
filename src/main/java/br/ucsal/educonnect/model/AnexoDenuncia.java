package br.ucsal.educonnect.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Pedido do cliente (validação do RF09): arquivo (foto ou documento) anexado à denúncia. */
@Entity
@Table(name = "anexos_denuncia")
public class AnexoDenuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "denuncia_id")
    private Denuncia denuncia;

    @Column(nullable = false, length = 200)
    private String nomeArquivo;

    @Column(nullable = false, length = 100)
    private String tipoConteudo;

    private long tamanho;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] dados;

    public Long getId() { return id; }
    public Denuncia getDenuncia() { return denuncia; }
    public void setDenuncia(Denuncia denuncia) { this.denuncia = denuncia; }
    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }
    public String getTipoConteudo() { return tipoConteudo; }
    public void setTipoConteudo(String tipoConteudo) { this.tipoConteudo = tipoConteudo; }
    public long getTamanho() { return tamanho; }
    public void setTamanho(long tamanho) { this.tamanho = tamanho; }
    public byte[] getDados() { return dados; }
    public void setDados(byte[] dados) { this.dados = dados; }

    /** Tamanho legível, ex.: "230 KB". */
    public String getTamanhoFormatado() {
        if (tamanho < 1024) return tamanho + " B";
        if (tamanho < 1024 * 1024) return Math.round(tamanho / 1024.0) + " KB";
        return String.format("%.1f MB", tamanho / (1024.0 * 1024.0));
    }
}
