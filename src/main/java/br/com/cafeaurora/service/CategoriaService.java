package br.com.cafeaurora.service;

import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.exception.*;
import br.com.cafeaurora.model.Categoria;
import br.com.cafeaurora.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoriaService {
    private final CategoriaRepository repo;

    public CategoriaService(CategoriaRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return repo.findAllByOrderByNomeAsc().stream().map(CategoriaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscar(Long id) { return CategoriaResponse.de(obter(id)); }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest req) {
        String nome = req.nome().trim();
        validarDuplicidade(nome, null);
        Categoria c = new Categoria(nome, req.descricao(), req.ativa());
        return CategoriaResponse.de(repo.save(c));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest req) {
        Categoria c = obter(id);
        String nome = req.nome().trim();
        validarDuplicidade(nome, id);
        c.atualizar(nome, req.descricao(), req.ativa());
        return CategoriaResponse.de(repo.save(c));
    }

    @Transactional
    public void desativar(Long id) {
        Categoria c = obter(id);
        c.desativar();
    }

    @Transactional(readOnly = true)
    public Categoria obter(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Categoria não encontrada: " + id));
    }

    private void validarDuplicidade(String nome, Long idAtual) {
        repo.findByNomeIgnoreCase(nome).ifPresent(existente -> {
            if (!existente.getId().equals(idAtual))
                throw new RegraNegocioException("Já existe uma categoria com este nome");
        });
    }
}
