package br.ucsal.educonnect.model;

public enum Perfil {
    ALUNO("Aluno"),
    PROFESSOR("Professor"),
    ADMINISTRADOR("Administrador"),
    GESTOR_DENUNCIAS("Gestor de Denúncias");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
