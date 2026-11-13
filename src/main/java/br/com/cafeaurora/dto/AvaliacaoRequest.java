package br.com.cafeaurora.dto;
import jakarta.validation.constraints.*;
public record AvaliacaoRequest(String referencia,
    @NotBlank @Size(min=2,max=100) String nomeAutor,
    @NotNull @Min(1) @Max(5) Integer nota,
    @NotBlank @Size(min=5,max=1000) String comentario,
    @NotNull @AssertTrue Boolean consentimentoPublicacao) {
    public AvaliacaoRequest {
        nomeAutor=nomeAutor==null?null:nomeAutor.strip().replaceAll("\\s+"," ");
        comentario=comentario==null?null:comentario.strip();
    }
}
