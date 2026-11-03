package br.com.cafeaurora.dto;
import java.math.BigDecimal;
import br.com.cafeaurora.model.ItemPedido;
public record ItemPedidoResponse(Long produtoId, String nomeProduto, int quantidade,
    BigDecimal precoUnitario, BigDecimal subtotal, String imagemUrl) {
    public static ItemPedidoResponse de(ItemPedido item) {
        return new ItemPedidoResponse(item.getProduto().getId(), item.getNomeProduto(),
            item.getQuantidade(), item.getPrecoUnitario(), item.getSubtotal(), CardapioProduto.imagemSegura(item.getImagemUrl()));
    }
}
