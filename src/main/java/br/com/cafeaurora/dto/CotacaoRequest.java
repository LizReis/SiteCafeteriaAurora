package br.com.cafeaurora.dto;
import br.com.cafeaurora.enums.TipoAtendimento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record CotacaoRequest(@NotNull TipoAtendimento tipoAtendimento,
    @NotEmpty @Size(max=50) List<@NotNull @Valid ItemPedidoRequest> itens) {
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejeitarCampo(String nome, Object valor) {
        throw new IllegalArgumentException("Campo não permitido");
    }
}
