package br.com.cafeaurora.service;

import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.enums.TipoCardapio;
import br.com.cafeaurora.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CardapioService {
    private final CategoriaRepository categorias;
    private final ProdutoRepository produtos;
    public CardapioService(CategoriaRepository categorias, ProdutoRepository produtos) {
        this.categorias = categorias;
        this.produtos = produtos;
    }
    public List<CardapioSecao> listarPorTipo(TipoCardapio tipo) {
        var secoes = categorias.findByAtivaTrueAndTipoCardapioOrderByOrdemExibicaoAscIdAsc(tipo);
        var porCategoria = produtos.findByAtivoTrueAndCategoria_AtivaTrueAndCategoria_TipoCardapioOrderByNomeAscIdAsc(tipo)
            .stream().map(CardapioProduto::de).collect(Collectors.groupingBy(CardapioProduto::categoriaId));
        return secoes.stream().map(c -> new CardapioSecao(c.getId(), c.getNome(),
            porCategoria.getOrDefault(c.getId(), List.of()))).toList();
    }
    public List<CategoriaResponse> categoriasPublicas() {
        return categorias.findByAtivaTrueOrderByNomeAsc().stream().map(CategoriaResponse::de).toList();
    }
    public List<CardapioProduto> produtosPublicos(Long categoriaId) {
        var lista = categoriaId == null ? produtos.findByAtivoTrueAndCategoria_AtivaTrueOrderByNomeAsc()
            : produtos.findByAtivoTrueAndCategoria_AtivaTrueAndCategoria_IdOrderByNomeAsc(categoriaId);
        return lista.stream().map(CardapioProduto::de).toList();
    }
    public CardapioProduto produtoPublico(Long id) {
        return produtos.findByIdAndAtivoTrueAndCategoria_AtivaTrue(id).map(CardapioProduto::de)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    }
}
