package br.ucsal.educonnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "professores")
public class Professor extends Usuario {

    @Column(length = 30)
    private String registroFuncional;

    @Override
    public Perfil getPerfil() { return Perfil.PROFESSOR; }

    @Override
    public String exibirPainel() { return "/professor"; }

    public String getRegistroFuncional() { return registroFuncional; }
    public void setRegistroFuncional(String registroFuncional) { this.registroFuncional = registroFuncional; }
}
