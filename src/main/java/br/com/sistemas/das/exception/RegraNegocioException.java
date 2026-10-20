package br.com.sistemas.das.exception;

/** Lançada quando uma regra de negócio é violada (vira HTTP 409). */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
