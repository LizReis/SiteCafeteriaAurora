package br.com.cafeaurora.dto;
import jakarta.validation.constraints.*;
public record EnderecoRequest(@NotBlank @Pattern(regexp="[0-9]{8}") String cep,
    @NotBlank @Size(max=180) String logradouro, @NotBlank @Size(max=12) String numero,
    @Size(max=80) String complemento, @NotBlank @Size(max=80) String bairro) {
    public String formatado() {
        return logradouro.trim() + ", " + numero.trim()
            + (complemento == null || complemento.isBlank() ? "" : " — " + complemento.trim())
            + " — " + bairro.trim() + " — CEP " + cep;
    }

    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejeitarCampo(String nome, Object valor) {
        throw new IllegalArgumentException("Campo não permitido");
    }
}
