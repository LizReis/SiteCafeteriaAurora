package br.com.cafeaurora.service;

import br.com.cafeaurora.enums.TipoCardapio;
import br.com.cafeaurora.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class CompatibilidadeCategorias {
    private final CategoriaRepository categorias;
    public CompatibilidadeCategorias(CategoriaRepository categorias) { this.categorias = categorias; }
    @Transactional
    public void atualizarLegadas() {
        // Somente reconhecimento inicial. Nunca roteamos consultas pelo nome.
        var tipos = Map.of("Cafés Especiais", TipoCardapio.CAFE, "Drinks e Bebidas", TipoCardapio.DRINKS,
            "Doces e Salgados", TipoCardapio.COMIDAS, "Espressos", TipoCardapio.CAFE,
            "Capuccino", TipoCardapio.CAFE, "Alcoólicos", TipoCardapio.DRINKS,
            "Sucos", TipoCardapio.DRINKS, "Salgados", TipoCardapio.COMIDAS, "Doces", TipoCardapio.COMIDAS);
        for (var c : categorias.findAll()) {
            if (c.getTipoCardapio() == null || c.getOrdemExibicao() == null) {
                c.classificar(c.getTipoCardapio() == null ? tipos.get(c.getNome()) : c.getTipoCardapio(),
                    c.getOrdemExibicao() == null ? 0 : c.getOrdemExibicao());
            }
        }
    }
}
