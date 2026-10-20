package br.com.sistemas.das.exception;

/** Lançada quando um recurso procurado não existe (vira HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
 
