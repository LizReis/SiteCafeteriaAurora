package br.com.cafeaurora.dto;

import br.com.cafeaurora.model.Categoria;

public record CategoriaResponse(Long id, String nome, String descricao, boolean ativa) {
    public static CategoriaResponse de(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNome(), c.getDescricao(), c.isAtiva());
    }
}
