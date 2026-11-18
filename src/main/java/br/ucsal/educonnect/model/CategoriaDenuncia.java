package br.ucsal.educonnect.model;

public enum CategoriaDenuncia {
    ACADEMICA("Acadêmica"),
    ADMINISTRATIVA("Administrativa"),
    ASSEDIO("Assédio"),
    CONDUTA("Conduta"),
    DISCRIMINACAO("Discriminação"),
    INFRAESTRUTURA("Infraestrutura"),
    OUTRA("Outra");

    private final String rotulo;

    CategoriaDenuncia(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() { return rotulo; }
}
