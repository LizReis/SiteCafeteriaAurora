package br.com.cafeaurora.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 1000) String descricao,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal preco,
        @Size(max = 500) String imagemUrl,
        @NotNull @Positive Long categoriaId,
        @NotNull Boolean ativo,
        @NotNull Boolean disponivel) {}
