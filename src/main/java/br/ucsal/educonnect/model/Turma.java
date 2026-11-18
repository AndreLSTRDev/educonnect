package br.ucsal.educonnect.model;

import br.ucsal.educonnect.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "turmas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"disciplina_id", "codigo", "periodo"}))
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    /** Ex.: 2026.1 */
    @Column(nullable = false, length = 20)
    private String periodo;

    /** Pedido do cliente (validação do RF04): sala onde a turma tem aula. */
    @Column(length = 60)
    private String sala;

    @ManyToOne(optional = false)
    @JoinColumn(name = "disciplina_id")
    private Disciplina disciplina;

    /** Professor responsável (RF04). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    /** Pedido do cliente (validação do RF04): a turma pode ter mais de um professor. */
    @ManyToMany
    @JoinTable(name = "turma_professores",
            joinColumns = @JoinColumn(name = "turma_id"),
            inverseJoinColumns = @JoinColumn(name = "professor_id"))
    private Set<Professor> professoresAdicionais = new LinkedHashSet<>();

    /** Um Set + chave composta na tabela de junção impedem o aluno duplicado (RF04). */
    @ManyToMany
    @JoinTable(name = "turma_alunos",
            joinColumns = @JoinColumn(name = "turma_id"),
            inverseJoinColumns = @JoinColumn(name = "aluno_id"))
    private Set<Aluno> alunos = new LinkedHashSet<>();

    /** Turma encerrada = resultados finais (aprovado/reprovado) passam a valer. */
    private boolean encerrada;

    public void vincularAluno(Aluno aluno) {
        if (!alunos.add(aluno)) {
            throw new RegraNegocioException("O aluno " + aluno.getNomeCompleto()
                    + " já está vinculado a esta turma.");
        }
    }

    public void removerAluno(Aluno aluno) {
        alunos.remove(aluno);
    }

    public void vincularProfessor(Professor professor) {
        this.professor = professor;
        professoresAdicionais.remove(professor);
    }

    public void adicionarProfessor(Professor outro) {
        if (outro.equals(professor) || !professoresAdicionais.add(outro)) {
            throw new RegraNegocioException("O professor " + outro.getNomeCompleto()
                    + " já faz parte desta turma.");
        }
    }

    public void removerProfessor(Professor outro) {
        professoresAdicionais.remove(outro);
    }

    /** O professor responsável ou um dos adicionais pode trabalhar na turma. */
    public boolean temProfessor(Long professorId) {
        return (professor != null && professor.getId().equals(professorId))
                || professoresAdicionais.stream().anyMatch(p -> p.getId().equals(professorId));
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }
    public String getSala() { return sala; }
    public void setSala(String sala) { this.sala = sala; }
    public Disciplina getDisciplina() { return disciplina; }
    public void setDisciplina(Disciplina disciplina) { this.disciplina = disciplina; }
    public Professor getProfessor() { return professor; }
    public Set<Professor> getProfessoresAdicionais() { return professoresAdicionais; }
    public Set<Aluno> getAlunos() { return alunos; }
    public boolean isEncerrada() { return encerrada; }
    public void setEncerrada(boolean encerrada) { this.encerrada = encerrada; }
}
