package br.ucsal.educonnect.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "administradores")
public class Administrador extends Usuario {

    @Override
    public Perfil getPerfil() { return Perfil.ADMINISTRADOR; }

    @Override
    public String exibirPainel() { return "/admin"; }
}
