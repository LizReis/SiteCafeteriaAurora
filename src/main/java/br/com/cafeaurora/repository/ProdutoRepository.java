package br.com.cafeaurora.repository;

import br.com.cafeaurora.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import br.com.cafeaurora.enums.TipoCardapio;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Produto p join fetch p.categoria where p.id in :ids order by p.id")
    List<Produto> buscarParaPedido(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);

    @EntityGraph(attributePaths = "categoria")
    List<Produto> findAllByOrderByNomeAsc();
    @EntityGraph(attributePaths = "categoria")
    List<Produto> findByAtivoTrueAndCategoria_AtivaTrueOrderByNomeAsc();
    @EntityGraph(attributePaths = "categoria")
    List<Produto> findByAtivoTrueAndCategoria_AtivaTrueAndCategoria_IdOrderByNomeAsc(Long categoriaId);
    @EntityGraph(attributePaths = "categoria")
    Optional<Produto> findByIdAndAtivoTrueAndCategoria_AtivaTrue(Long id);
    @EntityGraph(attributePaths = "categoria")
    List<Produto> findByAtivoTrueAndCategoria_AtivaTrueAndCategoria_TipoCardapioOrderByNomeAscIdAsc(TipoCardapio tipo);
}
