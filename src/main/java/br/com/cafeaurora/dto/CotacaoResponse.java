package br.com.cafeaurora.dto;
import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;
public record CotacaoResponse(String cotacao, Instant expiraEm, List<ItemPedidoResponse> itens,
    BigDecimal subtotal, BigDecimal taxaEntrega, BigDecimal total) {}
