package br.com.cafeaurora.repository;

import br.com.cafeaurora.model.Pedido;
import br.com.cafeaurora.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Pedido p where p.referenciaPublica = :referencia")
    java.util.Optional<Pedido> buscarParaAvaliacao(@org.springframework.data.repository.query.Param("referencia") String referencia);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"itens", "itens.produto"})
    java.util.Optional<Pedido> findByReferenciaPublica(String referencia);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"itens", "itens.produto"})
    java.util.Optional<Pedido> findByIdempotenciaHash(String hash);
    boolean existsByCotacaoHash(String hash);
    List<Pedido> findAllByOrderByCriadoEmDesc();
    List<Pedido> findByStatusOrderByCriadoEmDesc(StatusPedido status);
    org.springframework.data.domain.Page<Pedido> findByStatus(StatusPedido status, org.springframework.data.domain.Pageable pagina);
    long countByStatus(StatusPedido status);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"itens"})
    @org.springframework.data.jpa.repository.Query("select p from Pedido p where p.id = :id")
    java.util.Optional<Pedido> buscarComItens(@org.springframework.data.repository.query.Param("id") Long id);
}
