package br.com.cafeaurora.repository;
import br.com.cafeaurora.model.Avaliacao;
import br.com.cafeaurora.enums.StatusModeracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
public interface AvaliacaoRepository extends JpaRepository<Avaliacao,Long> {
    boolean existsByPedidoId(Long pedidoId);
    Page<Avaliacao> findByStatusModeracao(StatusModeracao status,Pageable pagina);
}
