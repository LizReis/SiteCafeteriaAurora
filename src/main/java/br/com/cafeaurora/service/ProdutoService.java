package br.com.cafeaurora.service;

import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.exception.*;
import br.com.cafeaurora.model.*;
import br.com.cafeaurora.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProdutoService {
    private final ProdutoRepository repo;
    private final CategoriaService categorias;

    public ProdutoService(ProdutoRepository repo, CategoriaService categorias) {
        this.repo = repo;
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar() {
        return repo.findAllByOrderByNomeAsc().stream().map(ProdutoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscar(Long id) { return ProdutoResponse.de(obter(id)); }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest req) {
        Categoria categoria = validarCategoria(req.categoriaId(), req.ativo());
        Produto produto = new Produto(req.nome().trim(), req.descricao(), req.preco(),
                req.imagemUrl(), categoria, req.ativo(), req.disponivel());
        return ProdutoResponse.de(repo.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest req) {
        Produto produto = obter(id);
        Categoria categoria = validarCategoria(req.categoriaId(), req.ativo());
        produto.atualizar(req.nome().trim(), req.descricao(), req.preco(), req.imagemUrl(),
                categoria, req.ativo(), req.disponivel());
        return ProdutoResponse.de(repo.save(produto));
    }

    @Transactional
    public void desativar(Long id) { obter(id).desativar(); }

    private Produto obter(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Produto não encontrado: " + id));
    }

    private Categoria validarCategoria(Long id, boolean ativo) {
        Categoria categoria = categorias.obter(id);
        if (ativo && !categoria.isAtiva()) {
            throw new RegraNegocioException("Não é permitido ativar produto de categoria inativa");
        }
        return categoria;
    }
}
