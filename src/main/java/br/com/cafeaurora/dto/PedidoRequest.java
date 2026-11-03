package br.com.cafeaurora.dto;
import br.com.cafeaurora.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record PedidoRequest(
    @NotBlank @Size(max=120) String nomeCliente,
    @NotBlank @Pattern(regexp="[0-9]{10,11}") String telefoneCliente,
    @NotBlank @Email @Size(max=150) String emailCliente,
    @NotNull TipoAtendimento tipoAtendimento, @NotNull FormaPagamento formaPagamento,
    @Valid EnderecoRequest endereco, @Size(max=500) String observacoes,
    @NotEmpty @Size(max=50) List<@NotNull @Valid ItemPedidoRequest> itens,
    @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String cotacao) {
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejeitarCampo(String nome, Object valor) {
        throw new IllegalArgumentException("Campo não permitido");
    }
}
