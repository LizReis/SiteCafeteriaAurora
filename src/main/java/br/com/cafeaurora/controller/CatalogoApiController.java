package br.com.cafeaurora.controller;
import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.service.CardapioService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogoApiController {
    private final CardapioService cardapio;
    public CatalogoApiController(CardapioService cardapio) { this.cardapio = cardapio; }
    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() { return cardapio.categoriasPublicas(); }
    @GetMapping("/produtos")
    public List<CardapioProduto> listarProdutos(@RequestParam(required = false) Long categoriaId) {
        return cardapio.produtosPublicos(categoriaId);
    }
    @GetMapping("/produtos/{id}")
    public CardapioProduto buscarProduto(@PathVariable Long id) { return cardapio.produtoPublico(id); }
}
