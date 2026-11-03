package br.com.cafeaurora.repository;

import br.com.cafeaurora.model.Categoria;
import br.com.cafeaurora.enums.TipoCardapio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByAtivaTrueAndTipoCardapioOrderByOrdemExibicaoAscIdAsc(TipoCardapio tipo);
    Optional<Categoria> findByNomeIgnoreCase(String nome);
    List<Categoria> findByAtivaTrueOrderByNomeAsc();
    List<Categoria> findAllByOrderByNomeAsc();
}
