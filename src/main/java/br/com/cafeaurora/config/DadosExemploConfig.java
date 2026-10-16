package br.com.cafeaurora.config;

import br.com.cafeaurora.model.Categoria;
import br.com.cafeaurora.model.Produto;
import br.com.cafeaurora.repository.CategoriaRepository;
import br.com.cafeaurora.repository.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.math.BigDecimal;

@Configuration
@Profile("dev")
public class DadosExemploConfig {
    @Bean
    CommandLineRunner alimentarCatalogo(CategoriaRepository categorias, ProdutoRepository produtos) {
        return args -> {
            if (categorias.count() > 0 || produtos.count() > 0) return;
            Categoria cafes = categorias.save(new Categoria("Cafés Especiais", "Espressos e bebidas à base de café", true));
            Categoria bebidas = categorias.save(new Categoria("Drinks e Bebidas", "Bebidas quentes e geladas", true));
            Categoria comidas = categorias.save(new Categoria("Doces e Salgados", "Acompanhamentos artesanais", true));
            adicionar(produtos, cafes, "Espresso Italiano", "A nossa especialidade. Do clássico espresso às criações autorais.", "10.00");
            adicionar(produtos, cafes, "Cappuccino", "Cappuccino clássico com leite vaporizado e polvilhado com cacau.", "14.00");
            adicionar(produtos, cafes, "Latte Macchiato", "Leite cremoso manchado com um shot de café espresso intenso.", "16.00");
            adicionar(produtos, bebidas, "Frappuccino de Chocolate", "Bebida gelada de café e chocolate.", "18.00");
            adicionar(produtos, bebidas, "Soda Italiana de Frutas Vermelhas", "Bebida gaseificada de frutas vermelhas.", "12.00");
            adicionar(produtos, bebidas, "Cold Brew com Caramelo", "Café extraído a frio com caramelo.", "15.00");
            adicionar(produtos, comidas, "Croissant de Amêndoas", "Croissant com amêndoas.", "12.00");
            adicionar(produtos, comidas, "Brownie com Sorvete", "Brownie com sorvete.", "22.00");
            adicionar(produtos, comidas, "Torta de Frutas Silvestres", "Torta com frutas silvestres.", "16.00");
        };
    }

    private void adicionar(ProdutoRepository repo, Categoria categoria, String nome,
                          String descricao, String preco) {
        repo.save(new Produto(nome, descricao, new BigDecimal(preco), null, categoria, true, true));
    }
}
