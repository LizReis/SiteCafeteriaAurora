package br.com.cafeaurora.service;
import br.com.cafeaurora.enums.TipoCardapio;
import br.com.cafeaurora.model.Categoria;
import br.com.cafeaurora.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class CardapioServiceTest {
    @Test void consultaDuasVezesIndependenteDaQuantidadeDeSecoes() {
        var categorias = mock(CategoriaRepository.class);
        var produtos = mock(ProdutoRepository.class);
        var c = new Categoria("Renomeada", null, true, TipoCardapio.CAFE, 0);
        ReflectionTestUtils.setField(c, "id", 12L);
        when(categorias.findByAtivaTrueAndTipoCardapioOrderByOrdemExibicaoAscIdAsc(TipoCardapio.CAFE)).thenReturn(List.of(c));
        when(produtos.findByAtivoTrueAndCategoria_AtivaTrueAndCategoria_TipoCardapioOrderByNomeAscIdAsc(TipoCardapio.CAFE)).thenReturn(List.of());
        var secoes = new CardapioService(categorias, produtos).listarPorTipo(TipoCardapio.CAFE);
        assertEquals("Renomeada", secoes.getFirst().nome());
        assertTrue(secoes.getFirst().produtos().isEmpty());
        verify(categorias).findByAtivaTrueAndTipoCardapioOrderByOrdemExibicaoAscIdAsc(TipoCardapio.CAFE);
        verify(produtos).findByAtivoTrueAndCategoria_AtivaTrueAndCategoria_TipoCardapioOrderByNomeAscIdAsc(TipoCardapio.CAFE);
        verifyNoMoreInteractions(categorias, produtos);
    }
}
