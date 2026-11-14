package br.com.cafeaurora;

import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.enums.*;
import br.com.cafeaurora.model.*;
import br.com.cafeaurora.repository.*;
import br.com.cafeaurora.service.*;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercita filtros, controllers, templates e persistência reais, sem mock de serviços. */
@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:admin_etapa4;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false", "cafe.admin.username=admin", "cafe.admin.password=senha-exclusiva-para-testes"})
class AdminIntegracaoTests {
    @Autowired WebApplicationContext context;
    @Autowired Filter springSecurityFilterChain;
    @Autowired CategoriaRepository categorias;
    @Autowired ProdutoRepository produtos;
    @Autowired PedidoRepository pedidos;
    @Autowired CotacaoPedidoRepository cotacoes;
    @Autowired PedidoService checkout;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.security.core.userdetails.UserDetailsService usuarios;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    MockMvc mvc;
    MockHttpSession session;
    CsrfToken csrf;
    Categoria categoria;
    Produto produto;
    final tools.jackson.databind.json.JsonMapper json = new tools.jackson.databind.json.JsonMapper();
    final String basic = "Basic " + Base64.getEncoder().encodeToString("admin:senha-exclusiva-para-testes".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    @BeforeEach void preparar() throws Exception {
        jdbc.update("delete from historico_status_pedido");pedidos.deleteAll(); cotacoes.deleteAll(); produtos.deleteAll(); categorias.deleteAll();
        categoria=categorias.save(new Categoria("Categoria teste", "Descrição", true, TipoCardapio.CAFE, 0));
        produto=produtos.save(new Produto("Produto teste", "Descrição", new BigDecimal("10.00"), null, categoria, true, true));
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        token(null);
    }
    void token(MockHttpSession existente) throws Exception {
        var b=get("/admin/login"); if(existente!=null) b.session(existente);
        var r=mvc.perform(b).andExpect(status().isOk()).andReturn();
        csrf=(CsrfToken)r.getRequest().getAttribute(CsrfToken.class.getName());
        csrf.getToken(); session=(MockHttpSession)r.getRequest().getSession(false);
    }
    MockHttpServletRequestBuilder form(String path) {
        return post(path).session(session).param(csrf.getParameterName(),csrf.getToken());
    }
    void login() throws Exception {
        var r=mvc.perform(form("/admin/login").param("username","admin").param("password","senha-exclusiva-para-testes"))
            .andExpect(status().isFound()).andExpect(redirectedUrl("/admin")).andReturn();
        token((MockHttpSession)r.getRequest().getSession(false));
    }
    MockHttpServletRequestBuilder api(String method,String path,Object body) {
        return request(org.springframework.http.HttpMethod.valueOf(method),path).header("Authorization",basic)
            .contentType("application/json").content(json.writeValueAsString(body));
    }
    PedidoResponse pedido() {return pedido(TipoAtendimento.RETIRADA);}
    PedidoResponse pedido(TipoAtendimento tipo) {
        var itens=List.of(new ItemPedidoRequest(produto.getId(),2));
        var cotacao=checkout.cotar(new CotacaoRequest(tipo,itens));
        return checkout.criar(new PedidoRequest("Cliente fictício","11999999999","cliente@example.com",
            tipo,FormaPagamento.DINHEIRO,tipo==TipoAtendimento.ENTREGA?new EnderecoRequest("01310000","Rua","1",null,"Bairro"):null,"Observação fictícia",itens,cotacao.cotacao()),UUID.randomUUID().toString());
    }
    CategoriaRequest categoria(String nome) {return new CategoriaRequest(nome,"Descrição",true,TipoCardapio.COMIDAS,2);}
    ProdutoRequest produto(String preco,boolean ativo,boolean disponivel) {
        return new ProdutoRequest("Editado","Nova descrição",new BigDecimal(preco),"/img/1320.jpg",categoria.getId(),ativo,disponivel);
    }

    @Test void loginLogoutSessaoECadeiasSeparadas() throws Exception {
        mvc.perform(get("/admin").accept("text/html")).andExpect(status().isFound()).andExpect(redirectedUrl("/admin/login"));
        login();
        mvc.perform(get("/admin").accept("text/html").session(session)).andExpect(status().isOk()).andExpect(view().name("admin/index"));
        mvc.perform(get("/api/admin/produtos").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/categorias").session(session).contentType("application/json").content(json.writeValueAsString(categoria("Negada"))))
            .andExpect(status().isUnauthorized());
        var basicResult=mvc.perform(get("/api/admin/produtos").header("Authorization",basic)).andExpect(status().isOk()).andReturn();
        assertNull(basicResult.getRequest().getSession(false));
        mvc.perform(form("/admin/logout")).andExpect(status().isFound()).andExpect(redirectedUrl("/admin/login?logout"));
        assertTrue(session.isInvalid());
        mvc.perform(get("/admin").accept("text/html")).andExpect(status().isFound());
    }
    @ParameterizedTest @CsvSource({"admin,errada","inexistente,senha-exclusiva-para-testes"})
    void rejeitaLoginEBasicInvalidos(String user,String password) throws Exception {
        mvc.perform(form("/admin/login").param("username",user).param("password",password))
            .andExpect(redirectedUrl("/admin/login?error"));
        String invalid="Basic "+Base64.getEncoder().encodeToString((user+":"+password).getBytes());
        mvc.perform(get("/api/admin/categorias").header("Authorization",invalid)).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin").accept("text/html").session(session)).andExpect(status().isFound());
    }
    @Test void csrfLoginFormulariosCheckoutEConsole() throws Exception {
        mvc.perform(post("/admin/login").param("username","admin").param("password","senha-exclusiva-para-testes")).andExpect(status().isForbidden());
        login();
        for(String path:List.of("/admin/categorias/salvar","/admin/produtos/salvar","/admin/logout","/admin/pedidos/1/status","/api/pedidos","/api/pedidos/cotacao")) {
            mvc.perform(post(path).session(session)).andExpect(status().isForbidden());
            mvc.perform(post(path).session(session).param("_csrf","invalido")).andExpect(status().isForbidden());
        }
        mvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
        mvc.perform(get("/h2-console/").session(session)).andExpect(status().isForbidden());
        assertEquals(0,pedidos.count());
    }
    @Test void usuarioSemRoleAdminNaoObtemPrivilegiosPorParametrosOuSessao() throws Exception {
        var manager=(org.springframework.security.provisioning.UserDetailsManager)usuarios;
        manager.createUser(org.springframework.security.core.userdetails.User.withUsername("visitante")
            .password(encoder.encode("senha-ficticia" )).roles("USER").build());
        try {
            mvc.perform(form("/admin/login").param("username","visitante").param("password","senha-ficticia").param("role","ADMIN"))
                .andExpect(redirectedUrl("/admin"));
            mvc.perform(get("/admin").session(session)).andExpect(status().isForbidden());
            String auth="Basic "+Base64.getEncoder().encodeToString("visitante:senha-ficticia".getBytes());
            mvc.perform(get("/api/admin/produtos").header("Authorization",auth)).andExpect(status().isForbidden());
            assertEquals(1,produtos.count());
        } finally {manager.deleteUser("visitante");}
    }
    @Test void apiCategoriasCrudDuplicidadeExclusaoLogica() throws Exception {
        var r=mvc.perform(api("POST","/api/admin/categorias",categoria("Nova"))).andExpect(status().isCreated()).andReturn();
        String location=r.getResponse().getHeader("Location");
        mvc.perform(api("POST","/api/admin/categorias",categoria("nOVA"))).andExpect(status().isConflict());
        mvc.perform(api("PUT",location,categoria("Nova"))).andExpect(status().isOk());
        mvc.perform(api("PUT",location,categoria("Renomeada"))).andExpect(status().isOk());
        mvc.perform(get(location).header("Authorization",basic)).andExpect(jsonPath("$.nome").value("Renomeada"));
        mvc.perform(delete(location).header("Authorization",basic)).andExpect(status().isNoContent());
        mvc.perform(get(location).header("Authorization",basic)).andExpect(jsonPath("$.ativa").value(false));
        assertEquals(2,categorias.count());
        mvc.perform(get("/api/categorias")).andExpect(jsonPath("$",hasSize(1)));
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"nome\":\"\",\"ativa\":true,\"tipoCardapio\":\"CAFE\",\"ordemExibicao\":0}",
        "{\"nome\":\"N\",\"ativa\":true,\"tipoCardapio\":\"INVALIDO\",\"ordemExibicao\":0}",
        "{\"nome\":\"N\",\"ativa\":true,\"tipoCardapio\":\"CAFE\",\"ordemExibicao\":-1}"})
    void categoriasInvalidas(String body) throws Exception {
        mvc.perform(post("/api/admin/categorias").header("Authorization",basic).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertEquals(1,categorias.count());
    }
    @ParameterizedTest @ValueSource(strings={"0","-1","1.001","100000000.00"})
    void precosInvalidos(String preco) throws Exception {
        mvc.perform(api("POST","/api/admin/produtos",produto(preco,true,true))).andExpect(status().isBadRequest());
        assertEquals(1,produtos.count());
    }
    @Test void produtosCrudDisponibilidadeCategoriaEHistorico() throws Exception {
        var historico=pedido();
        String path="/api/admin/produtos/"+produto.getId();
        mvc.perform(api("PUT",path,produto("22.50",true,false))).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(true)).andExpect(jsonPath("$.disponivel").value(false));
        mvc.perform(get("/api/produtos/"+produto.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.disponivel").value(false));
        mvc.perform(delete(path).header("Authorization",basic)).andExpect(status().isNoContent());
        assertEquals(1,produtos.count());
        mvc.perform(get("/api/produtos/"+produto.getId())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/categorias/"+categoria.getId()).header("Authorization",basic)).andExpect(status().isNoContent());
        mvc.perform(api("PUT",path,produto("22.50",true,true))).andExpect(status().isConflict());
        var salvo=checkout.consultar(historico.referencia());
        assertEquals("Produto teste",salvo.itens().getFirst().nomeProduto());
        assertEquals(new BigDecimal("10.00"),salvo.itens().getFirst().precoUnitario());
        assertEquals(1,pedidos.count());
    }
    @Test void recursosInexistentesECategoriaInvalida() throws Exception {
        for(String path:List.of("/api/admin/categorias/99999","/api/admin/produtos/99999")) {
            mvc.perform(get(path).header("Authorization",basic)).andExpect(status().isNotFound());
            mvc.perform(delete(path).header("Authorization",basic)).andExpect(status().isNotFound());
        }
        var p=new ProdutoRequest("P",null,BigDecimal.ONE,null,99999L,true,true);
        mvc.perform(api("POST","/api/admin/produtos",p)).andExpect(status().isNotFound());
        mvc.perform(api("POST","/api/admin/produtos",Map.of())).andExpect(status().isBadRequest());
    }
    @Test void formulariosCategoriasValidacaoEdicaoEDuplicidade() throws Exception {
        login();
        mvc.perform(get("/admin/categorias").session(session).param("editar",categoria.getId().toString()))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Categoria teste")));
        mvc.perform(form("/admin/categorias/salvar").param("nome","").param("ativa","true").param("tipoCardapio","CAFE").param("ordemExibicao","-1"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","nome","ordemExibicao"));
        mvc.perform(form("/admin/categorias/salvar").param("nome","categoria TESTE").param("ativa","true").param("tipoCardapio","CAFE").param("ordemExibicao","0"))
            .andExpect(status().isOk()).andExpect(model().attributeExists("erroNegocio","categorias","tipos"));
        mvc.perform(form("/admin/categorias/salvar").param("id",categoria.getId().toString()).param("nome","Editada HTML").param("ativa","true").param("tipoCardapio","DRINKS").param("ordemExibicao","1"))
            .andExpect(redirectedUrl("/admin/categorias")).andExpect(flash().attributeExists("sucesso"));
        assertEquals("Editada HTML",categorias.findById(categoria.getId()).orElseThrow().getNome());
        mvc.perform(form("/admin/categorias/"+categoria.getId()+"/desativar")).andExpect(redirectedUrl("/admin/categorias"));
        assertFalse(categorias.findById(categoria.getId()).orElseThrow().isAtiva());
    }
    MockHttpServletRequestBuilder produtoForm(String nome,String preco) {
        return form("/admin/produtos/salvar").param("nome",nome).param("descricao","Descrição mantida").param("preco",preco)
            .param("categoriaId",categoria.getId().toString()).param("ativo","true").param("disponivel","false");
    }
    @Test void formulariosProdutosValidacaoEdicaoENegocio() throws Exception {
        login();
        mvc.perform(get("/admin/produtos").session(session).param("editar",produto.getId().toString()))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Produto teste")));
        mvc.perform(produtoForm("Mantido","0").param("id",produto.getId().toString()))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","preco"))
            .andExpect(model().attributeExists("editando","categorias","produtos")).andExpect(content().string(containsString("Mantido")));
        mvc.perform(produtoForm("HTML produto","12.34").param("id",produto.getId().toString()))
            .andExpect(redirectedUrl("/admin/produtos"));
        assertEquals(new BigDecimal("12.34"),produtos.findById(produto.getId()).orElseThrow().getPreco());
        categoria.desativar();categorias.save(categoria);
        mvc.perform(produtoForm("Falha negócio","10.00")).andExpect(status().isOk()).andExpect(model().attributeExists("erroNegocio","categorias"));
        mvc.perform(form("/admin/produtos/"+produto.getId()+"/desativar")).andExpect(redirectedUrl("/admin/produtos"));
        assertFalse(produtos.findById(produto.getId()).orElseThrow().isAtivo());
    }
    @Test void dashboardVazioEPovoado() throws Exception {
        login();
        mvc.perform(get("/admin").accept("text/html").session(session)).andExpect(model().attribute("totalPedidos",0L)).andExpect(model().attribute("totalProdutos",1));
        pedido();
        mvc.perform(get("/admin").accept("text/html").session(session)).andExpect(model().attribute("totalPedidos",1L))
            .andExpect(model().attribute("pendentes",1L)).andExpect(model().attribute("emPreparo",0L))
            .andExpect(model().attribute("prontos",0L)).andExpect(model().attribute("recentes",hasSize(1)));
    }
    @Test void listagemFiltrosPaginacaoEOrdenacao() throws Exception {
        login();
        mvc.perform(get("/admin/pedidos").session(session)).andExpect(model().attribute("pedidos",empty()));
        long ultimo=0;
        for(int i=0;i<16;i++) ultimo=pedido().id();
        var r=mvc.perform(get("/admin/pedidos").session(session)).andExpect(status().isOk())
            .andExpect(model().attribute("pedidos",hasSize(15))).andReturn();
        var lista=(List<?>)r.getModelAndView().getModel().get("pedidos");
        assertEquals(ultimo,((AdminPedidoService.Resumo)lista.getFirst()).id());
        mvc.perform(get("/admin/pedidos").session(session).param("pagina","1")).andExpect(model().attribute("pedidos",hasSize(1)));
        mvc.perform(get("/admin/pedidos").session(session).param("pagina","999")).andExpect(model().attribute("pedidos",empty()));
        mvc.perform(get("/admin/pedidos").session(session).param("pagina","-1")).andExpect(model().attribute("pedidos",hasSize(15)));
        for(var s:StatusPedido.values()) mvc.perform(get("/admin/pedidos").session(session).param("status",s.name()))
            .andExpect(status().isOk()).andExpect(model().attribute("pedidos",hasSize(s==StatusPedido.RECEBIDO?15:0)));
        mvc.perform(get("/admin/pedidos").session(session).param("status","DESCONHECIDO")).andExpect(status().isBadRequest());
    }
    @Test void filtrosComRegistrosDeCadaStatusEIndicadoresReais() throws Exception {
        login();
        for(var s:StatusPedido.values()) {
            var p=pedido();jdbc.update("update pedidos set status=? where id=?",s.name(),p.id());
        }
        for(var s:StatusPedido.values()) {
            var r=mvc.perform(get("/admin/pedidos").session(session).param("status",s.name()))
                .andExpect(status().isOk()).andExpect(model().attribute("pedidos",hasSize(1))).andReturn();
            var lista=(List<?>)r.getModelAndView().getModel().get("pedidos");
            assertEquals(s,((AdminPedidoService.Resumo)lista.getFirst()).status());
        }
        mvc.perform(get("/admin").session(session)).andExpect(model().attribute("totalPedidos",6L))
            .andExpect(model().attribute("pendentes",1L)).andExpect(model().attribute("emPreparo",1L))
            .andExpect(model().attribute("prontos",1L)).andExpect(model().attribute("categoriasAtivas",1L))
            .andExpect(model().attribute("produtosAtivos",1L));
    }
    @Test void criaCategoriaEProdutoPelosFormulariosReais() throws Exception {
        login();
        mvc.perform(form("/admin/categorias/salvar").param("nome","Criada HTML").param("ativa","true")
            .param("tipoCardapio","CAFE").param("ordemExibicao","3")).andExpect(redirectedUrl("/admin/categorias"));
        assertTrue(categorias.findByNomeIgnoreCase("Criada HTML").isPresent());
        mvc.perform(produtoForm("Criado HTML","15.99")).andExpect(redirectedUrl("/admin/produtos"));
        assertEquals(2,produtos.count());
        mvc.perform(get("/cardapio-cafe")).andExpect(content().string(containsString("Criado HTML")))
            .andExpect(content().string(containsString("Indisponível")));
    }
    static Stream<Arguments> transicoes() {
        return Arrays.stream(TipoAtendimento.values()).flatMap(t->Arrays.stream(StatusPedido.values()).flatMap(a->Arrays.stream(StatusPedido.values()).map(b->Arguments.of(t,a,b))));
    }
    @ParameterizedTest @MethodSource("transicoes")
    void matrizStatusPersistidaEPublica(TipoAtendimento tipo,StatusPedido atual,StatusPedido proximo) throws Exception {
        var p=pedido(tipo);
        jdbc.update("update pedidos set status=? where id=?",atual.name(),p.id());
        login();
        boolean permitido=switch(atual) {
            case RECEBIDO -> proximo==StatusPedido.EM_PREPARO||proximo==StatusPedido.CANCELADO;
            case EM_PREPARO -> proximo==StatusPedido.PRONTO||proximo==StatusPedido.CANCELADO;
            case PRONTO -> proximo==(tipo==TipoAtendimento.ENTREGA?StatusPedido.EM_ENTREGA:StatusPedido.CONCLUIDO)||proximo==StatusPedido.CANCELADO;
            case EM_ENTREGA -> tipo==TipoAtendimento.ENTREGA&&proximo==StatusPedido.CONCLUIDO;
            default -> false;
        };
        mvc.perform(form("/admin/pedidos/"+p.id()+"/status").param("status",proximo.name()))
            .andExpect(redirectedUrl("/admin/pedidos/"+p.id())).andExpect(flash().attributeExists(permitido?"sucesso":"erro"));
        assertEquals(permitido?proximo:atual,pedidos.findById(p.id()).orElseThrow().getStatus());
        assertEquals(permitido?2:1,jdbc.queryForObject("select count(*) from historico_status_pedido where pedido_id=?",Integer.class,p.id()));
        mvc.perform(get("/api/pedidos/consulta/"+p.referencia())).andExpect(jsonPath("$.status").value((permitido?proximo:atual).name()))
            .andExpect(jsonPath("$.statusPagamento").value("PENDENTE"))
            .andExpect(jsonPath("$.historico").doesNotExist()).andExpect(jsonPath("$.responsavel").doesNotExist());
    }
    @Test void detalhesLegadosHistoricoEProtecaoStatus() throws Exception {
        var p=pedido();login();
        mvc.perform(get("/admin/pedidos/"+p.id()).session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Cliente fictício"))).andExpect(content().string(containsString("Produto teste")))
            .andExpect(content().string(containsString("cliente@example.com")));
        jdbc.update("update pedidos set subtotal=null,taxa_entrega=null,email_cliente=null,referencia_publica=null where id=?",p.id());
        mvc.perform(get("/admin/pedidos/"+p.id()).session(session)).andExpect(status().isOk()).andExpect(content().string(containsString("Não registrado")));
        mvc.perform(get("/admin/pedidos/99999").session(session)).andExpect(status().isNotFound());
        mvc.perform(form("/admin/pedidos/99999/status").param("status","EM_PREPARO")).andExpect(status().isNotFound());
        token(null);
        mvc.perform(form("/admin/pedidos/"+p.id()+"/status").accept("text/html").param("status","EM_PREPARO")).andExpect(status().isFound());
        assertEquals(StatusPedido.RECEBIDO,pedidos.findById(p.id()).orElseThrow().getStatus());
    }
}
