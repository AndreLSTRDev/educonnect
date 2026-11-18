package br.ucsal.educonnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "disciplinas")
public class Disciplina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false)
    private int cargaHoraria;

    /** Pedido do cliente (validação do RF03): ementa da disciplina. */
    @Column(length = 2000)
    private String ementa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    /** Pedido do cliente (validação do RF03): disciplinas que precisam ser cursadas antes. */
    @ManyToMany
    @JoinTable(name = "disciplina_pre_requisitos",
            joinColumns = @JoinColumn(name = "disciplina_id"),
            inverseJoinColumns = @JoinColumn(name = "pre_requisito_id"))
    @OrderBy("codigo ASC")
    private Set<Disciplina> preRequisitos = new LinkedHashSet<>();

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public int getCargaHoraria() { return cargaHoraria; }
    public void setCargaHoraria(int cargaHoraria) { this.cargaHoraria = cargaHoraria; }
    public String getEmenta() { return ementa; }
    public void setEmenta(String ementa) { this.ementa = ementa; }
    public Curso getCurso() { return curso; }
    public void setCurso(Curso curso) { this.curso = curso; }
    public Set<Disciplina> getPreRequisitos() { return preRequisitos; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Disciplina outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return Disciplina.class.hashCode();
    }
}
