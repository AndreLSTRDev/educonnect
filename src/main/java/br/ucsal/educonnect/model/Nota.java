package br.ucsal.educonnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "notas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "avaliacao_id"}))
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double valor;

    @Column(length = 255)
    private String observacoes;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "avaliacao_id")
    private Avaliacao avaliacao;

    /** RF05: o valor deve estar entre 0 e o valor máximo da avaliação (escala da instituição). */
    public boolean validar() {
        return valor >= 0 && valor <= avaliacao.getValorMaximo();
    }

    public Long getId() { return id; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public Avaliacao getAvaliacao() { return avaliacao; }
    public void setAvaliacao(Avaliacao avaliacao) { this.avaliacao = avaliacao; }
}
