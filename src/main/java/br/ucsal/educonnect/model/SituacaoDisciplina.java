package br.ucsal.educonnect.model;

public enum SituacaoDisciplina {
    EM_ANDAMENTO("Em andamento", "primary"),
    /** Turma aberta, mas a média parcial está abaixo da mínima (pedido do cliente no RF07). */
    EM_RISCO_POR_NOTA("Em risco por nota", "warning"),
    /** Turma aberta, mas a frequência parcial está abaixo da mínima. */
    EM_RISCO_POR_FALTA("Em risco por falta", "warning"),
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
