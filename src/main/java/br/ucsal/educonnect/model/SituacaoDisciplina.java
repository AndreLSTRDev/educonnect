package br.ucsal.educonnect.model;

public enum SituacaoDisciplina {
    EM_ANDAMENTO("Em andamento", "primary"),
    APROVADO("Aprovado", "success"),
    REPROVADO_POR_NOTA("Reprovado por nota", "danger"),
    REPROVADO_POR_FALTA("Reprovado por falta", "danger");

    private final String rotulo;
    private final String cor;

    SituacaoDisciplina(String rotulo, String cor) {
        this.rotulo = rotulo;
        this.cor = cor;
    }

    public String getRotulo() { return rotulo; }
    public String getCor() { return cor; }
}
