package br.com.cafeaurora.repository;
import br.com.cafeaurora.model.HistoricoStatusPedido;
import org.springframework.data.repository.Repository;
import java.util.List;
public interface HistoricoStatusPedidoRepository extends Repository<HistoricoStatusPedido,Long> {
    HistoricoStatusPedido save(HistoricoStatusPedido evento);
    List<HistoricoStatusPedido> findByPedidoIdOrderByAlteradoEmAscIdAsc(Long pedidoId);
}
