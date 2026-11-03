package br.com.cafeaurora.dto;

import br.com.cafeaurora.enums.TipoCardapio;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank @Size(max = 80) String nome,
        @Size(max = 255) String descricao,
        @NotNull Boolean ativa,
        @NotNull TipoCardapio tipoCardapio,
        @NotNull @Min(0) Integer ordemExibicao) {
    public CategoriaRequest(String nome, String descricao, Boolean ativa) {
        this(nome, descricao, ativa, null, 0);
    }
}
