package br.com.cafeaurora;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica a renderização real dos fragmentos compartilhados (head, header e footer).
 * Evita regressões em que os templates passam a referenciar um fragmento não declarado.
 */
@SpringBootTest
class PaginasCompartilhadasTests {

    @Autowired
    private WebApplicationContext context;

    @ParameterizedTest
    @ValueSource(strings = {
        "/", "/cardapio.html", "/cardapio-cafe.html",
        "/cardapio-comidas.html", "/cardapio-drinks.html",
        "/sacola-pedido.html", "/localizacao.html", "/pedido-confirmado.html"
    })
    void renderizaPaginasComRodapeCompartilhado(String caminho) throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

        mvc.perform(get(caminho))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("class=\"site-footer\"")))
            .andExpect(content().string(containsString("2026 Café Aurora")));
    }
}
