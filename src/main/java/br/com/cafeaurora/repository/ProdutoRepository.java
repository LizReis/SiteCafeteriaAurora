package br.com.cafeaurora.repository;

import br.com.cafeaurora.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findAllByOrderByNomeAsc();
    List<Produto> findByAtivoTrueAndCategoria_AtivaTrueOrderByNomeAsc();
    List<Produto> findByAtivoTrueAndCategoria_AtivaTrueAndCategoria_IdOrderByNomeAsc(Long categoriaId);
    Optional<Produto> findByIdAndAtivoTrueAndCategoria_AtivaTrue(Long id);
}
