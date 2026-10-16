package br.com.cafeaurora.repository;

import br.com.cafeaurora.model.Pedido;
import br.com.cafeaurora.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findAllByOrderByCriadoEmDesc();
    List<Pedido> findByStatusOrderByCriadoEmDesc(StatusPedido status);
}
