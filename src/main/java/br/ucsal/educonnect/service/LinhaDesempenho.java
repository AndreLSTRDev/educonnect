package br.ucsal.educonnect.service;

import br.ucsal.educonnect.model.SituacaoDisciplina;

/** Uma linha do desempenho (RF07) ou do histórico (RF08) do aluno. */
public record LinhaDesempenho(
        Long turmaId,
        String codigoDisciplina,
        String nomeDisciplina,
        int cargaHoraria,
        String periodo,
        Double media,
        Double frequencia,
        SituacaoDisciplina situacao) {
}
