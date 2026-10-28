package br.com.cafeaurora.service;

import br.com.cafeaurora.dto.CategoriaRequest;
import br.com.cafeaurora.exception.RegraNegocioException;
import br.com.cafeaurora.model.Categoria;
import br.com.cafeaurora.repository.CategoriaRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoriaServiceTest {
    private final CategoriaRepository repositorio = mock(CategoriaRepository.class);
    private final CategoriaService service = new CategoriaService(repositorio);

    @Test
    void naoPermiteNomeDuplicadoIgnorandoMaiusculas() {
        when(repositorio.findByNomeIgnoreCase("Cafés")).thenReturn(
                Optional.of(new Categoria("Cafés", null, true)));
        assertThrows(RegraNegocioException.class,
                () -> service.criar(new CategoriaRequest(" Cafés ", null, true)));
        verify(repositorio, never()).save(any());
    }

    @Test
    void atualizacaoPodeManterNomeDaMesmaCategoria() {
        Categoria existente = new Categoria("Cafés", null, true);
        org.springframework.test.util.ReflectionTestUtils.setField(existente, "id", 1L);
        when(repositorio.findById(1L)).thenReturn(Optional.of(existente));
        when(repositorio.findByNomeIgnoreCase("Cafés")).thenReturn(Optional.of(existente));
        when(repositorio.save(existente)).thenReturn(existente);

        assertDoesNotThrow(() ->
                service.atualizar(1L, new CategoriaRequest("Cafés", null, true)));
    }

    @Test
    void desativacaoNaoExcluiFisicamenteCategoria() {
        Categoria categoria = new Categoria("Chás", null, true);
        when(repositorio.findById(1L)).thenReturn(Optional.of(categoria));

        service.desativar(1L);

        assertFalse(categoria.isAtiva());
        verify(repositorio, never()).delete(any());
    }
}
