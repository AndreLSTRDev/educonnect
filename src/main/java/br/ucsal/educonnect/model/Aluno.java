package br.ucsal.educonnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "alunos")
public class Aluno extends Usuario {

    @Column(length = 30)
    private String matricula;

    @Override
    public Perfil getPerfil() { return Perfil.ALUNO; }

    @Override
    public String exibirPainel() { return "/aluno"; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
}
