package br.com.cafeaurora.dto;
import br.com.cafeaurora.enums.*;
import br.com.cafeaurora.model.Pedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
// Consulta por referência-bearer: não retorna nome, telefone, e-mail ou observações.
public record PedidoResponse(Long id, String referencia, StatusPedido status, Instant criadoEm,
    TipoAtendimento tipoAtendimento, FormaPagamento formaPagamento, StatusPagamento statusPagamento,
    List<ItemPedidoResponse> itens, BigDecimal subtotal, BigDecimal taxaEntrega, BigDecimal total,
    String enderecoEntrega, boolean jaAvaliado) {
    public static PedidoResponse de(Pedido p) {
        return new PedidoResponse(p.getId(), p.getReferenciaPublica(), p.getStatus(), p.getCriadoEm(),
            p.getTipoAtendimento(), p.getFormaPagamento(), p.getStatusPagamento(),
            p.getItens().stream().map(ItemPedidoResponse::de).toList(), p.getSubtotal(), p.getTaxaEntrega(),
            p.getTotal(), p.getEnderecoEntrega(), p.isJaAvaliado());
    }
}
