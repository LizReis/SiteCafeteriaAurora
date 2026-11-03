package br.com.cafeaurora.repository;

import br.com.cafeaurora.model.Pedido;
import br.com.cafeaurora.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"itens", "itens.produto"})
    java.util.Optional<Pedido> findByReferenciaPublica(String referencia);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"itens", "itens.produto"})
    java.util.Optional<Pedido> findByIdempotenciaHash(String hash);
    boolean existsByCotacaoHash(String hash);
    List<Pedido> findAllByOrderByCriadoEmDesc();
    List<Pedido> findByStatusOrderByCriadoEmDesc(StatusPedido status);
}
