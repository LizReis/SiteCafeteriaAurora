package br.com.cafeaurora.exception;
public class PedidoPersistenciaException extends RuntimeException {
    public PedidoPersistenciaException() { super("Não foi possível registrar o pedido. Tente novamente."); }
}
