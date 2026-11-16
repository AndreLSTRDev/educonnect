package br.ucsal.educonnect.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "gestores_denuncias")
public class GestorDenuncias extends Usuario {

    @Override
    public Perfil getPerfil() { return Perfil.GESTOR_DENUNCIAS; }

    @Override
    public String exibirPainel() { return "/gestor"; }
}
