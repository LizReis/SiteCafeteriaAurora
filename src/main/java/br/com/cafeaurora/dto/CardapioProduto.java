package br.com.cafeaurora.dto;

import br.com.cafeaurora.model.Produto;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public record CardapioProduto(Long id, String nome, String descricao, BigDecimal preco,
                              String imagemUrl, boolean disponivel, Long categoriaId, String categoria) {
    public static CardapioProduto de(Produto p) {
        return new CardapioProduto(p.getId(), p.getNome(), p.getDescricao(), p.getPreco(),
            p.getImagemUrl(), p.isDisponivel(), p.getCategoria().getId(), p.getCategoria().getNome());
    }
    public String getPrecoFormatado() {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(preco);
    }
    public long getPrecoCentavos() { return preco.movePointRight(2).longValueExact(); }
    public String getImagem() { return imagemSegura(imagemUrl); }
    public static String imagemSegura(String imagemUrl) {
        if (imagemUrl == null || imagemUrl.isBlank()) return "/img/produto-sem-imagem.svg";
        // Apenas caminhos locais absolutos e URLs HTTPS, nunca esquemas executáveis.
        return imagemUrl.matches("^/(?!/).*|^https://.+") ? imagemUrl : "/img/produto-sem-imagem.svg";
    }
}
