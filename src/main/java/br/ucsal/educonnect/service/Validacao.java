package br.ucsal.educonnect.service;

import br.ucsal.educonnect.exception.RegraNegocioException;

/** Validações reutilizáveis com mensagens claras (RNF-U.4). */
final class Validacao {

    private Validacao() {
    }

    static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException("O campo \"" + campo + "\" é obrigatório.");
        }
        return valor.trim();
    }

    static String limitar(String valor, String campo, int max) {
        if (valor != null && valor.length() > max) {
            throw new RegraNegocioException("O campo \"" + campo + "\" deve ter no máximo " + max + " caracteres.");
        }
        return valor;
    }

    static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /** Aceita vírgula ou ponto como separador decimal (ex.: 7,5). */
    static double numero(String texto, String campo) {
        String limpo = obrigatorio(texto, campo).replace(',', '.');
        try {
            double v = Double.parseDouble(limpo);
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Informe um número válido em \"" + campo + "\" (exemplo: 7,5).");
        }
    }
}
