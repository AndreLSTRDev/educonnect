package br.ucsal.educonnect.exception;

/** Violação de regra de negócio; a mensagem é exibida ao usuário (RNF-U.4). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
