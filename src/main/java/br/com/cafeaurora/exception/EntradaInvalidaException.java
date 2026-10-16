package br.com.cafeaurora.exception;

public class EntradaInvalidaException extends RuntimeException {
    public EntradaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
