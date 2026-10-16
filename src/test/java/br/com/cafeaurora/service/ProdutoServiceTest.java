package br.com.cafeaurora.service;

import br.com.cafeaurora.dto.ProdutoRequest;
import br.com.cafeaurora.exception.RegraNegocioException;
import br.com.cafeaurora.model.Categoria;
import br.com.cafeaurora.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProdutoServiceTest {
    private final ProdutoRepository repositorio = mock(ProdutoRepository.class);
    private final CategoriaService categorias = mock(CategoriaService.class);
    private final ProdutoService service = new ProdutoService(repositorio, categorias);

    @Test
    void naoPermiteProdutoAtivoEmCategoriaInativa() {
        when(categorias.obter(1L)).thenReturn(new Categoria("Inativa", null, false));
        ProdutoRequest request = new ProdutoRequest("Café", null, new BigDecimal("10.00"),
                null, 1L, true, true);

        assertThrows(RegraNegocioException.class, () -> service.criar(request));
        verifyNoInteractions(repositorio);
    }
}
