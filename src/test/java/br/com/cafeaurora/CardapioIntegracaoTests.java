package br.com.cafeaurora;

import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.enums.TipoCardapio;
import br.com.cafeaurora.model.*;
import br.com.cafeaurora.repository.*;
import br.com.cafeaurora.service.*;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
class CardapioIntegracaoTests {
    @Autowired CategoriaRepository categorias;
    @Autowired ProdutoRepository produtos;
    @Autowired CardapioService cardapio;
    @Autowired CategoriaService categoriaService;
    @Autowired CompatibilidadeCategorias compatibilidade;
    @Autowired WebApplicationContext context;
    @Autowired Filter springSecurityFilterChain;
    MockMvc mvc;
    String auth = "Basic " + Base64.getEncoder().encodeToString("admin:senha-exclusiva-para-testes".getBytes());
    @BeforeEach void preparar() {
        produtos.deleteAll(); categorias.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
    }
    Categoria categoria(String nome, TipoCardapio tipo, int ordem) {
        return categorias.save(new Categoria(nome, "Seção", true, tipo, ordem));
    }
    Produto produto(Categoria c, String nome, boolean ativo, boolean disponivel) {
        return produtos.save(new Produto(nome, "Descrição do banco", new BigDecimal("14.35"), "/img/1050.jpg", c, ativo, disponivel));
    }
    @Test void separaOrdenaFiltraEPreservaVazias() {
        assertTrue(cardapio.listarPorTipo(TipoCardapio.CAFE).isEmpty());
        var segunda = categoria("Segunda", TipoCardapio.CAFE, 2);
        var primeira = categoria("Primeira", TipoCardapio.CAFE, 1);
        var vazia = categoria("Vazia", TipoCardapio.CAFE, 3);
        var inativa = categoria("Oculta", TipoCardapio.CAFE, 0); inativa.desativar(); categorias.save(inativa);
        produto(primeira, "Disponível", true, true);
        produto(primeira, "Indisponível", true, false);
        produto(primeira, "Desativado", false, true);
        produto(inativa, "Oculto", true, true);
        produto(categoria("Sucos", TipoCardapio.DRINKS, 0), "Suco", true, true);
        produto(categoria("Doces", TipoCardapio.COMIDAS, 0), "Doce", true, true);
        var secoes = cardapio.listarPorTipo(TipoCardapio.CAFE);
        assertEquals(java.util.List.of(primeira.getId(), segunda.getId(), vazia.getId()), secoes.stream().map(CardapioSecao::id).toList());
        assertEquals(2, secoes.getFirst().produtos().size());
        assertFalse(secoes.getFirst().produtos().get(1).disponivel());
        assertEquals("/img/1050.jpg", secoes.getFirst().produtos().getFirst().getImagem());
        assertEquals(1435, secoes.getFirst().produtos().getFirst().getPrecoCentavos());
        assertTrue(secoes.get(1).produtos().isEmpty());
        assertEquals("Suco", cardapio.listarPorTipo(TipoCardapio.DRINKS).getFirst().produtos().getFirst().nome());
        assertEquals("Doce", cardapio.listarPorTipo(TipoCardapio.COMIDAS).getFirst().produtos().getFirst().nome());
    }
    @Test void migraSomenteCamposNovosSemPerderOuReativarDados() {
        var antiga = categorias.save(new Categoria("Doces e Salgados", "Preservar", false));
        var personalizado = categorias.save(new Categoria("Receita familiar", "Escolher tipo pelo CRUD", true));
        var p = produto(antiga, "Editado antes", false, false);
        compatibilidade.atualizarLegadas(); compatibilidade.atualizarLegadas();
        var migrada = categorias.findById(antiga.getId()).orElseThrow();
        assertEquals(TipoCardapio.COMIDAS, migrada.getTipoCardapio());
        assertFalse(migrada.isAtiva()); assertEquals("Preservar", migrada.getDescricao());
        assertNull(categorias.findById(personalizado.getId()).orElseThrow().getTipoCardapio());
        assertEquals(2, categorias.count()); assertEquals(1, produtos.count());
        assertFalse(produtos.findById(p.getId()).orElseThrow().isAtivo());
        categoriaService.atualizar(antiga.getId(), new CategoriaRequest("Renomeada", "Preservar", true, TipoCardapio.COMIDAS, 0));
        compatibilidade.atualizarLegadas();
        assertEquals(TipoCardapio.COMIDAS, categorias.findById(antiga.getId()).orElseThrow().getTipoCardapio());
    }
    @Test void crudAdministrativoRenderizaNosTresMenus() throws Exception {
        for (var tipo : TipoCardapio.values()) {
            var c = categoria("Seção " + tipo, tipo, 0);
            String path = "/cardapio-" + (tipo == TipoCardapio.CAFE ? "cafe" : tipo == TipoCardapio.DRINKS ? "drinks" : "comidas");
            String body = """
                {"nome":"Produto novo %s","descricao":"Descrição cadastrada","preco":14.35,
                "imagemUrl":"/img/1050.jpg","categoriaId":%d,"ativo":true,"disponivel":true}
                """.formatted(tipo, c.getId());
            var result = mvc.perform(post("/api/admin/produtos").header("Authorization",auth).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn();
            String location = result.getResponse().getHeader("Location");
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().string(containsString("Produto novo " + tipo)))
                .andExpect(content().string(containsString("Descrição cadastrada")))
                .andExpect(content().string(containsString("14,35")))
                .andExpect(content().string(containsString("data-price-cents=\"1435\"")))
                .andExpect(content().string(not(containsString("th:each"))));
            body = body.replace("Produto novo", "Produto editado").replace("Descrição cadastrada", "Descrição editada")
                .replace("14.35", "27.90").replace("1050.jpg", "1320.jpg").replace("\"disponivel\":true", "\"disponivel\":false");
            mvc.perform(put(location).header("Authorization",auth).contentType("application/json").content(body)).andExpect(status().isOk());
            mvc.perform(get(path + ".html")).andExpect(status().isOk()).andExpect(content().string(containsString("27,90")))
                .andExpect(content().string(containsString("Produto editado " + tipo)))
                .andExpect(content().string(containsString("Descrição editada")))
                .andExpect(content().string(containsString("/img/1320.jpg")))
                .andExpect(content().string(containsString("Indisponível")))
                .andExpect(content().string(containsString("disabled=\"disabled\"")));
            mvc.perform(get("/api/produtos")).andExpect(status().isOk());
            mvc.perform(delete(location).header("Authorization",auth)).andExpect(status().isNoContent());
            mvc.perform(get(path)).andExpect(content().string(not(containsString("Produto editado " + tipo))));
        }
        mvc.perform(get("/api/admin/produtos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/categorias").header("Authorization",auth).contentType("application/json")
            .content("{\"nome\":\"Inválida\",\"ativa\":true,\"ordemExibicao\":-1}" )).andExpect(status().isBadRequest());
    }
    @Test void renomeiaDesativaEMoveProdutoEntreCardapios() throws Exception {
        var cafe = categoria("Original", TipoCardapio.CAFE, 0);
        var comida = categoria("Destino", TipoCardapio.COMIDAS, 0);
        var p = produto(cafe, "Movimentado", true, true);
        categoriaService.atualizar(cafe.getId(), new CategoriaRequest("Novo título", "Seção", true, TipoCardapio.CAFE, 1));
        mvc.perform(get("/cardapio-cafe")).andExpect(content().string(containsString("Novo título"))).andExpect(content().string(containsString("Movimentado")));
        p.atualizar(p.getNome(), p.getDescricao(), p.getPreco(), p.getImagemUrl(), comida, true, true); produtos.save(p);
        mvc.perform(get("/cardapio-cafe")).andExpect(content().string(not(containsString("Movimentado"))));
        mvc.perform(get("/cardapio-comidas")).andExpect(content().string(containsString("Movimentado")));
        categoriaService.desativar(comida.getId());
        mvc.perform(get("/cardapio-comidas")).andExpect(content().string(not(containsString("Movimentado"))));
        assertEquals("Movimentado", produtos.findById(p.getId()).orElseThrow().getNome());
    }
}
