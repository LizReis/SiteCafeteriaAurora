package br.com.cafeaurora.controller;

import br.com.cafeaurora.model.Categoria;
import br.com.cafeaurora.model.Produto;
import br.com.cafeaurora.repository.CategoriaRepository;
import br.com.cafeaurora.repository.ProdutoRepository;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogoApiController {
    private final CategoriaRepository categorias;
    private final ProdutoRepository produtos;

    public CatalogoApiController(CategoriaRepository categorias, ProdutoRepository produtos) {
        this.categorias = categorias;
        this.produtos = produtos;
    }

    public record CategoriaResposta(Long id, String nome, String descricao) {
        public static CategoriaResposta de(Categoria c) {
            return new CategoriaResposta(c.getId(), c.getNome(), c.getDescricao());
        }
    }
    public record ProdutoResposta(Long id, String nome, String descricao, BigDecimal preco,
                                  String imagemUrl, boolean disponivel, Long categoriaId, String categoria) {
        public static ProdutoResposta de(Produto p) {
            return new ProdutoResposta(p.getId(), p.getNome(), p.getDescricao(), p.getPreco(),
                    p.getImagemUrl(), p.isDisponivel(), p.getCategoria().getId(), p.getCategoria().getNome());
        }
    }

    @GetMapping("/categorias")
    public List<CategoriaResposta> listarCategorias() {
        return categorias.findByAtivaTrueOrderByNomeAsc().stream().map(CategoriaResposta::de).toList();
    }

    @GetMapping("/produtos")
    public List<ProdutoResposta> listarProdutos(@RequestParam(required = false) Long categoriaId) {
        List<Produto> lista = categoriaId == null
                ? produtos.findByAtivoTrueAndCategoria_AtivaTrueOrderByNomeAsc()
                : produtos.findByAtivoTrueAndCategoria_AtivaTrueAndCategoria_IdOrderByNomeAsc(categoriaId);
        return lista.stream().map(ProdutoResposta::de).toList();
    }

    @GetMapping("/produtos/{id}")
    public ProdutoResposta buscarProduto(@PathVariable Long id) {
        return produtos.findByIdAndAtivoTrueAndCategoria_AtivaTrue(id)
                .map(ProdutoResposta::de)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Produto não encontrado"));
    }
}
