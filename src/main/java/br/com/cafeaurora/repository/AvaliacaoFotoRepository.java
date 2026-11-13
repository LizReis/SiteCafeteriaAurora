package br.com.cafeaurora.repository;
import br.com.cafeaurora.model.AvaliacaoFoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AvaliacaoFotoRepository extends JpaRepository<AvaliacaoFoto,Long> {
    Optional<AvaliacaoFoto> findByAvaliacaoId(Long id);
    boolean existsByAvaliacaoId(Long id);
}
