package br.com.cafeaurora.service;
import br.com.cafeaurora.model.*;
import br.com.cafeaurora.enums.TipoCardapio;
import br.com.cafeaurora.repository.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
@Service @Profile("dev")
public class CargaCardapioDev {
    private final CategoriaRepository categorias;
    private final ProdutoRepository produtos;
    public CargaCardapioDev(CategoriaRepository categorias, ProdutoRepository produtos) {
        this.categorias = categorias; this.produtos = produtos;
    }
    @Transactional
    public void carregar() {
        if (categorias.count() > 0 || produtos.count() > 0) return;
        var espresso = categoria("Espressos", TipoCardapio.CAFE, 0);
        var capuccino = categoria("Capuccino", TipoCardapio.CAFE, 1);
        var alcool = categoria("Alcoólicos", TipoCardapio.DRINKS, 0);
        var sucos = categoria("Sucos", TipoCardapio.DRINKS, 1);
        var salgados = categoria("Salgados", TipoCardapio.COMIDAS, 0);
        var doces = categoria("Doces", TipoCardapio.COMIDAS, 1);
        produto(espresso, "Espresso Italiano", "Café espresso intenso servido em xícara.", "10.00", "produto-sem-imagem.svg");
        produto(espresso, "Latte Macchiato", "Leite cremoso com um shot de café espresso.", "16.00", "42953.jpg");
        produto(capuccino, "Cappuccino", "Café com leite vaporizado e cacau.", "14.00", "1050.jpg");
        produto(alcool, "Coquetel de Frutas Vermelhas", "Coquetel alcoólico com frutas vermelhas.", "24.00", "5493.jpg");
        produto(alcool, "Coquetel de Café", "Coquetel alcoólico de café servido em taça.", "26.00", "9492.jpg");
        produto(sucos, "Suco de Laranja", "Suco natural de laranja.", "12.00", "20691.jpg");
        produto(salgados, "Sanduíche da Casa", "Pão macio, queijo e ingredientes frescos.", "22.00", "4368.jpg");
        produto(salgados, "Croissant de Queijo", "Massa folhada crocante e recheio de queijo.", "15.00", "1320.jpg");
        produto(doces, "Torta de Frutas Silvestres", "Fatia de torta com frutas e geleia da casa.", "16.00", "2151900810.jpg");
    }
    private Categoria categoria(String nome, TipoCardapio tipo, int ordem) {
        return categorias.save(new Categoria(nome, null, true, tipo, ordem));
    }
    private void produto(Categoria categoria, String nome, String descricao, String preco, String imagem) {
        produtos.save(new Produto(nome, descricao, new BigDecimal(preco), "/img/" + imagem, categoria, true, true));
    }
}
