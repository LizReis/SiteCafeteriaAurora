package br.com.cafeaurora.dto;

import br.com.cafeaurora.model.Produto;
import java.math.BigDecimal;

public record ProdutoResponse(
        Long id, String nome, String descricao, BigDecimal preco, String imagemUrl,
        Long categoriaId, String categoriaNome, boolean ativo, boolean disponivel) {
    public static ProdutoResponse de(Produto p) {
        return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getPreco(),
                p.getImagemUrl(), p.getCategoria().getId(), p.getCategoria().getNome(),
                p.isAtivo(), p.isDisponivel());
    }
}
