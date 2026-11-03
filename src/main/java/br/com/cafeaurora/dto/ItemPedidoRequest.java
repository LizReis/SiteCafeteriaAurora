package br.com.cafeaurora.dto;
import jakarta.validation.constraints.*;
public record ItemPedidoRequest(@NotNull @Positive Long produtoId,
                                @NotNull @Min(1) @Max(99) Integer quantidade) {
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejeitarCampo(String nome, Object valor) {
        throw new IllegalArgumentException("Campo não permitido");
    }
}
