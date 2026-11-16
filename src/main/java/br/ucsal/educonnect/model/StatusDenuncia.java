package br.ucsal.educonnect.model;

public enum StatusDenuncia {
    ABERTA("Aberta", "primary"),
    EM_ANALISE("Em análise", "warning"),
    RESOLVIDA("Resolvida", "success"),
    ARQUIVADA("Arquivada", "secondary");

    private final String rotulo;
    private final String cor;

    StatusDenuncia(String rotulo, String cor) {
        this.rotulo = rotulo;
        this.cor = cor;
    }

    public String getRotulo() { return rotulo; }
    public String getCor() { return cor; }
}
