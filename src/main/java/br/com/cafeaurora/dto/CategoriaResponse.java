package br.com.cafeaurora.dto;

import br.com.cafeaurora.model.Categoria;
import br.com.cafeaurora.enums.TipoCardapio;

public record CategoriaResponse(Long id, String nome, String descricao, boolean ativa, TipoCardapio tipoCardapio, Integer ordemExibicao) {
    public static CategoriaResponse de(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNome(), c.getDescricao(), c.isAtiva(), c.getTipoCardapio(), c.getOrdemExibicao());
    }
}
