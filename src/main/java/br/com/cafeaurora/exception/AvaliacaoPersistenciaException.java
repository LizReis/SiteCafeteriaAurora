package br.com.cafeaurora.exception;
public class AvaliacaoPersistenciaException extends RuntimeException {
    public AvaliacaoPersistenciaException(){super("Não foi possível registrar a avaliação. Consulte o pedido antes de tentar novamente.");}
}
