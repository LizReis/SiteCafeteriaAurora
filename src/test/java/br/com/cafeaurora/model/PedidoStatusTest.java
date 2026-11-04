package br.com.cafeaurora.model;

import br.com.cafeaurora.enums.*;
import br.com.cafeaurora.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PedidoStatusTest {
    private Pedido novoPedido() {
        Categoria categoria = new Categoria("Cafés", "", true);
        Produto produto = new Produto("Expresso", "", new BigDecimal("10.00"),
            "/img/1050.jpg", categoria, true, true);
        Pedido pedido = new Pedido("Cliente", "71999999999", "cliente@example.com", null, null,
            TipoAtendimento.RETIRADA, FormaPagamento.DINHEIRO, new BigDecimal("0.00"),
            "referencia", "idempotencia", "requisicao", "cotacao");
        pedido.adicionarItem(produto, 1);
        return pedido;
    }

    @Test
    void permiteSomenteSequenciaDeAtendimento() {
        Pedido pedido = novoPedido();
        assertEquals(List.of(StatusPedido.EM_PREPARO, StatusPedido.CANCELADO),
            pedido.proximosStatus());
        pedido.alterarStatus(StatusPedido.EM_PREPARO);
        pedido.alterarStatus(StatusPedido.PRONTO);
        pedido.alterarStatus(StatusPedido.CONCLUIDO);
        assertEquals(StatusPedido.CONCLUIDO, pedido.getStatus());
        assertTrue(pedido.proximosStatus().isEmpty());
    }

    @Test
    void impedeSaltarEtapasEReabrirConcluido() {
        Pedido pedido = novoPedido();
        assertThrows(RegraNegocioException.class, () -> pedido.alterarStatus(StatusPedido.CONCLUIDO));
        assertEquals(StatusPedido.RECEBIDO, pedido.getStatus());
        pedido.alterarStatus(StatusPedido.EM_PREPARO);
        pedido.alterarStatus(StatusPedido.PRONTO);
        pedido.alterarStatus(StatusPedido.CONCLUIDO);
        assertThrows(RegraNegocioException.class, () -> pedido.alterarStatus(StatusPedido.RECEBIDO));
    }

    @Test
    void cancelamentoEFinal() {
        Pedido pedido = novoPedido();
        pedido.alterarStatus(StatusPedido.CANCELADO);
        assertThrows(RegraNegocioException.class, () -> pedido.alterarStatus(StatusPedido.EM_PREPARO));
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
    }
}
