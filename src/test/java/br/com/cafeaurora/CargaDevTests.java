package br.com.cafeaurora;
import br.com.cafeaurora.repository.*;
import br.com.cafeaurora.service.CargaCardapioDev;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:carga_dev;DB_CLOSE_DELAY=-1")
@ActiveProfiles("dev")
class CargaDevTests {
    @Autowired CargaCardapioDev carga;
    @Autowired CategoriaRepository categorias;
    @Autowired ProdutoRepository produtos;
    @Test void cargaCoerenteEIdempotenteNaoSobrescreveEdicoesOuReativa() {
        assertEquals(6, categorias.count()); assertEquals(9, produtos.count());
        var p = produtos.findAllByOrderByNomeAsc().getFirst();
        p.atualizar("Edição preservada", "Descrição", new java.math.BigDecimal("77.25"),
            "/img/1320.jpg", p.getCategoria(), false, false);
        produtos.save(p);
        carga.carregar(); carga.carregar();
        var salvo = produtos.findById(p.getId()).orElseThrow();
        assertEquals("Edição preservada", salvo.getNome()); assertEquals(new java.math.BigDecimal("77.25"),salvo.getPreco());
        assertFalse(salvo.isAtivo()); assertFalse(salvo.isDisponivel());
        assertEquals(6, categorias.count()); assertEquals(9, produtos.count());
        assertTrue(categorias.findAll().stream().allMatch(c->c.getTipoCardapio()!=null));
        for (var produto : produtos.findAll()) {
            assertNotNull(produto.getImagemUrl());
            assertTrue(new org.springframework.core.io.ClassPathResource("static"+produto.getImagemUrl()).exists());
        }
    }
}
