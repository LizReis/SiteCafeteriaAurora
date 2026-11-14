package br.com.cafeaurora;

import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.enums.*;
import br.com.cafeaurora.exception.*;
import br.com.cafeaurora.model.*;
import br.com.cafeaurora.repository.*;
import br.com.cafeaurora.service.PedidoService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:pedidos_tests;DB_CLOSE_DELAY=-1", "spring.jpa.open-in-view=false"})
class PedidoIntegracaoTests {
    @Autowired PedidoService service;
    @Autowired PedidoRepository pedidos;
    @Autowired ItemPedidoRepository itensSalvos;
    @Autowired CotacaoPedidoRepository cotacoes;
    @Autowired ProdutoRepository produtos;
    @Autowired CategoriaRepository categorias;
    @Autowired WebApplicationContext context;
    @Autowired Filter springSecurityFilterChain;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc; MockHttpSession session; CsrfToken csrf;
    Produto cafe, comida; Categoria categoria;
    final tools.jackson.databind.json.JsonMapper json=new tools.jackson.databind.json.JsonMapper();
    @BeforeEach void preparar() throws Exception {
        jdbc.update("delete from historico_status_pedido");pedidos.deleteAll();cotacoes.deleteAll();produtos.deleteAll();categorias.deleteAll();
        categoria=categorias.save(new Categoria("Cafés",null,true,TipoCardapio.CAFE,0));
        cafe=produtos.save(new Produto("Espresso", "Descrição",new BigDecimal("10.00"),"/img/cupEsquerdaCima.jpg",categoria,true,true));
        comida=produtos.save(new Produto("Croissant",null,new BigDecimal("15.50"),"/img/1320.jpg",categoria,true,true));
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        var page=mvc.perform(get("/finalizar-pedido")).andExpect(status().isOk()).andReturn();
        csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        csrf.getToken();session=(MockHttpSession)page.getRequest().getSession(false);
    }
    List<ItemPedidoRequest> linhas(int qtd) {return List.of(new ItemPedidoRequest(cafe.getId(),qtd));}
    PedidoRequest request(TipoAtendimento tipo,List<ItemPedidoRequest> itens) {
        var cotacao=service.cotar(new CotacaoRequest(tipo,itens));
        return new PedidoRequest("Cliente Exemplo","11999999999","cliente@example.com",tipo,FormaPagamento.DINHEIRO,
            tipo==TipoAtendimento.ENTREGA?new EnderecoRequest("01310000","Av Paulista","100","","Bela Vista"):null,
            "Sem açúcar",itens,cotacao.cotacao());
    }
    String chave(){return UUID.randomUUID().toString();}
    String serializar(Object o){return json.writeValueAsString(o);}
    MockHttpServletRequestBuilder postJson(String path,String body) {
        return post(path).session(session).header(csrf.getHeaderName(),csrf.getToken()).contentType("application/json").content(body);
    }
    @Test void entregaComMultiplosProdutosTotaisEHistorico() {
        var req=request(TipoAtendimento.ENTREGA,List.of(new ItemPedidoRequest(cafe.getId(),2),new ItemPedidoRequest(comida.getId(),1)));
        var resposta=service.criar(req,chave());
        assertEquals(new BigDecimal("35.50"),resposta.subtotal());assertEquals(new BigDecimal("7.00"),resposta.taxaEntrega());
        assertEquals(new BigDecimal("42.50"),resposta.total());assertEquals(StatusPedido.RECEBIDO,resposta.status());
        assertEquals(StatusPagamento.PENDENTE,resposta.statusPagamento());assertEquals(2,itensSalvos.count());
        assertEquals(1,pedidos.count());assertNotNull(resposta.criadoEm());
        cafe.atualizar("Nome futuro",null,new BigDecimal("12.00"),null,categoria,true,true);produtos.save(cafe);
        var salvo=service.consultar(resposta.referencia());
        assertEquals("Espresso",salvo.itens().stream().filter(i->i.produtoId().equals(cafe.getId())).findFirst().orElseThrow().nomeProduto());
        assertEquals(new BigDecimal("10.00"),salvo.itens().stream().filter(i->i.produtoId().equals(cafe.getId())).findFirst().orElseThrow().precoUnitario());
        assertEquals(resposta.total(),salvo.total());
    }
    @Test void retiradaUmItemSemEnderecoETaxa() {
        var p=service.criar(request(TipoAtendimento.RETIRADA,linhas(1)),chave());
        assertEquals(new BigDecimal("0.00"),p.taxaEntrega());assertEquals(new BigDecimal("10.00"),p.total());assertNull(p.enderecoEntrega());
    }
    @ParameterizedTest @ValueSource(ints={0,-1,100})
    void rejeitaQuantidadesInvalidas(int qtd) {
        assertThrows(EntradaInvalidaException.class,()->service.cotar(new CotacaoRequest(TipoAtendimento.RETIRADA,linhas(qtd))));
        assertEquals(0,pedidos.count());
    }
    @Test void aceitaQuantidade99() {assertEquals(new BigDecimal("990.00"),service.criar(request(TipoAtendimento.RETIRADA,linhas(99)),chave()).total());}
    @Test void rejeitaSemItens() {assertThrows(EntradaInvalidaException.class,()->service.cotar(new CotacaoRequest(TipoAtendimento.RETIRADA,List.of())));}
    @Test void rejeitaIdsDuplicados() {assertThrows(EntradaInvalidaException.class,()->service.cotar(new CotacaoRequest(TipoAtendimento.RETIRADA,List.of(new ItemPedidoRequest(cafe.getId(),1),new ItemPedidoRequest(cafe.getId(),2)))));}
    @Test void rejeitaLimiteTotalDeUnidades() {
        var terceiro=produtos.save(new Produto("Outro",null,BigDecimal.ONE,null,categoria,true,true));
        assertThrows(EntradaInvalidaException.class,()->service.cotar(new CotacaoRequest(TipoAtendimento.RETIRADA,List.of(new ItemPedidoRequest(cafe.getId(),99),new ItemPedidoRequest(comida.getId(),99),new ItemPedidoRequest(terceiro.getId(),3)))));
    }
    @Test void produtoInexistente() {assertThrows(RecursoNaoEncontradoException.class,()->service.cotar(new CotacaoRequest(TipoAtendimento.RETIRADA,List.of(new ItemPedidoRequest(Long.MAX_VALUE,1)))));}
    @Test void produtoInativo() {cafe.desativar();produtos.save(cafe);assertThrows(RegraNegocioException.class,()->request(TipoAtendimento.RETIRADA,linhas(1)));}
    @Test void indisponibilidadeAposCotacaoNaoPersistePedido() {
        var req=request(TipoAtendimento.RETIRADA,linhas(1));
        cafe.atualizar(cafe.getNome(),null,cafe.getPreco(),null,categoria,true,false);produtos.save(cafe);
        assertThrows(RegraNegocioException.class,()->service.criar(req,chave()));assertEquals(0,pedidos.count());assertEquals(0,itensSalvos.count());
    }
    @Test void categoriaInativa() {categoria.desativar();categorias.save(categoria);assertThrows(RegraNegocioException.class,()->request(TipoAtendimento.RETIRADA,linhas(1)));}
    @Test void precoAlteradoExigeRevisao() {
        var req=request(TipoAtendimento.RETIRADA,linhas(1));
        cafe.atualizar(cafe.getNome(),null,new BigDecimal("12.00"),null,categoria,true,true);produtos.save(cafe);
        assertThrows(RegraNegocioException.class,()->service.criar(req,chave()));assertEquals(0,pedidos.count());
        assertEquals(new BigDecimal("12.00"),service.criar(request(TipoAtendimento.RETIRADA,linhas(1)),chave()).total());
    }
    @Test void rejeitaTrocaDeItensOuTipoAposCotacao() {
        var r=request(TipoAtendimento.RETIRADA,linhas(1));
        var alterado=new PedidoRequest(r.nomeCliente(),r.telefoneCliente(),r.emailCliente(),r.tipoAtendimento(),r.formaPagamento(),null,r.observacoes(),linhas(2),r.cotacao());
        assertThrows(RegraNegocioException.class,()->service.criar(alterado,chave()));
    }
    @Test void cotacaoExpirada() {
        var req=request(TipoAtendimento.RETIRADA,linhas(1));jdbc.update("update cotacoes_pedido set expira_em = ?",java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(60)));
        assertThrows(RegraNegocioException.class,()->service.criar(req,chave()));
    }
    @Test void rollbackQuandoInsercaoDeItemFalha() {
        var req=request(TipoAtendimento.RETIRADA,linhas(13));
        jdbc.execute("alter table itens_pedido add constraint falha_teste check (quantidade <> 13)");
        try {assertThrows(PedidoPersistenciaException.class,()->service.criar(req,chave()));assertEquals(0,pedidos.count());assertEquals(0,itensSalvos.count());}
        finally {jdbc.execute("alter table itens_pedido drop constraint falha_teste");}
        assertNotNull(service.criar(req,chave()).referencia());
    }
    @Test void reenvioMesmoPayloadRetornaMesmoPedidoMesmoSeProdutoMudou() {
        var req=request(TipoAtendimento.RETIRADA,linhas(2));String key=chave();var p=service.criar(req,key);
        cafe.desativar();produtos.save(cafe);assertEquals(p,service.criar(req,key));assertEquals(1,pedidos.count());
    }
    @Test void mesmaChaveComDadosDiferentesEOutraChaveComMesmaCotacaoConflitam() {
        var req=request(TipoAtendimento.RETIRADA,linhas(1));String key=chave();service.criar(req,key);
        var outro=new PedidoRequest("Outro cliente",req.telefoneCliente(),req.emailCliente(),req.tipoAtendimento(),req.formaPagamento(),null,null,req.itens(),req.cotacao());
        assertThrows(RegraNegocioException.class,()->service.criar(outro,key));
        assertThrows(RegraNegocioException.class,()->service.criar(req,chave()));assertEquals(1,pedidos.count());
    }
    @Test void reenviosConcorrentesGravamUmaVez() throws Exception {
        var req=request(TipoAtendimento.RETIRADA,linhas(1));String key=chave();var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<PedidoResponse> criar=()->{start.await();return service.criar(req,key);};
            var a=pool.submit(criar);var b=pool.submit(criar);start.countDown();
            assertEquals(a.get(15,TimeUnit.SECONDS).id(),b.get(15,TimeUnit.SECONDS).id());
        }
        assertEquals(1,pedidos.count());assertEquals(1,itensSalvos.count());
    }
    @Test void httpCriacaoConsultaSemExporContatoEComNoStore() throws Exception {
        mvc.perform(get("/sacola-pedido")).andExpect(status().isOk())
            .andExpect(content().string(containsString("data-delivery-cents=\"700\"")));
        var req=request(TipoAtendimento.ENTREGA,linhas(2));String key=chave();
        var result=mvc.perform(postJson("/api/pedidos",serializar(req)).header("Idempotency-Key",key))
            .andExpect(status().isCreated()).andExpect(header().string("Cache-Control",containsString("no-store")))
            .andExpect(content().string(not(containsString("cliente@example.com")))).andReturn();
        var p=json.readTree(result.getResponse().getContentAsString());String token=p.get("referencia").asText();
        mvc.perform(get("/api/pedidos/consulta/"+token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control",containsString("no-store")));
        mvc.perform(get("/api/pedidos/consulta/invalido")).andExpect(status().isNotFound());
        mvc.perform(get("/api/pedidos/consulta/"+"a".repeat(43))).andExpect(status().isNotFound());
        mvc.perform(get("/api/pedidos/"+p.get("id").asLong())).andExpect(status().isNotFound());
        mvc.perform(postJson("/api/pedidos",serializar(req)).header("Idempotency-Key",key)).andExpect(status().isCreated());
        assertEquals(1,pedidos.count());
        mvc.perform(postJson("/api/pedidos/"+p.get("id").asLong()+"/status","{\"status\":\"CONCLUIDO\"}")).andExpect(status().isNotFound());
        assertEquals(StatusPedido.RECEBIDO,service.consultar(token).status());
    }
    @Test void httpCsrfObrigatorioSemAfetarExcecaoAdministrativa() throws Exception {
        var req=request(TipoAtendimento.RETIRADA,linhas(1));
        mvc.perform(post("/api/pedidos").contentType("application/json").content(serializar(req)).header("Idempotency-Key",chave())).andExpect(status().isForbidden());
        mvc.perform(post("/api/pedidos/cotacao").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/produtos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
    }
    @Test void httpRejeitaJSONInvalidoEPropriedadesOficiais() throws Exception {
        mvc.perform(postJson("/api/pedidos","{")).andExpect(status().isBadRequest());
        mvc.perform(postJson("/api/pedidos","{}")).andExpect(status().isBadRequest());
        var req=request(TipoAtendimento.RETIRADA,linhas(1));String body=serializar(req);
        for(String campo:List.of("total","status","statusPagamento","precoUnitario"))
            mvc.perform(postJson("/api/pedidos",body.substring(0,body.length()-1)+",\""+campo+"\":1}").header("Idempotency-Key",chave())).andExpect(status().isBadRequest());
        mvc.perform(postJson("/api/pedidos",body).header("Idempotency-Key","curta")).andExpect(status().isBadRequest());
        assertEquals(0,pedidos.count());
    }
    @Test void httpDisponibilidadeRetorna409() throws Exception {
        var req=request(TipoAtendimento.RETIRADA,linhas(1));cafe.desativar();produtos.save(cafe);
        mvc.perform(postJson("/api/pedidos",serializar(req)).header("Idempotency-Key",chave())).andExpect(status().isConflict());
    }
    @Test void camposDeContatoEEnderecoValidados() throws Exception {
        var r=request(TipoAtendimento.ENTREGA,linhas(1));
        var invalido=new PedidoRequest("", "123", "email-invalido", r.tipoAtendimento(),r.formaPagamento(),r.endereco(),null,r.itens(),r.cotacao());
        mvc.perform(postJson("/api/pedidos",serializar(invalido)).header("Idempotency-Key",chave())).andExpect(status().isBadRequest());
        var semEndereco=new PedidoRequest(r.nomeCliente(),r.telefoneCliente(),r.emailCliente(),r.tipoAtendimento(),r.formaPagamento(),null,null,r.itens(),r.cotacao());
        assertThrows(EntradaInvalidaException.class,()->service.criar(semEndereco,chave()));
    }
    @Test void httpFalhaPersistenciaNaoExpoeSQLNemSalvaParcialmente() throws Exception {
        var req=request(TipoAtendimento.RETIRADA,linhas(13));
        jdbc.execute("alter table itens_pedido add constraint falha_http check (quantidade <> 13)");
        try {
            mvc.perform(postJson("/api/pedidos",serializar(req)).header("Idempotency-Key",chave()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("insert into"))))
                .andExpect(content().string(not(containsString("cliente@example.com"))))
                .andExpect(content().string(not(containsString("JdbcSQL"))));
            assertEquals(0,pedidos.count());assertEquals(0,itensSalvos.count());
        } finally {jdbc.execute("alter table itens_pedido drop constraint falha_http");}
    }
    @Test void referenciasDiferentesNaoPermitemConsultarPorIdDeOutroPedido() throws Exception {
        var a=service.criar(request(TipoAtendimento.RETIRADA,linhas(1)),chave());
        var b=service.criar(request(TipoAtendimento.RETIRADA,linhas(2)),chave());
        assertNotEquals(a.referencia(),b.referencia());
        mvc.perform(get("/api/pedidos/consulta/"+a.referencia())).andExpect(status().isOk())
            .andExpect(content().string(containsString("\"quantidade\":1")))
            .andExpect(content().string(not(containsString("\"quantidade\":2"))));
        mvc.perform(get("/api/pedidos/consulta/"+b.id())).andExpect(status().isNotFound());
    }

    @Test void cotacaoHttpUsaValoresOficiaisERejeitaPrecoNoItem() throws Exception {
        var req=new CotacaoRequest(TipoAtendimento.ENTREGA,linhas(2));
        mvc.perform(postJson("/api/pedidos/cotacao",serializar(req)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value(20))
            .andExpect(jsonPath("$.taxaEntrega").value(7)).andExpect(jsonPath("$.total").value(27))
            .andExpect(header().string("Cache-Control",containsString("no-store")));
        String manipulado="{\"tipoAtendimento\":\"RETIRADA\",\"itens\":[{\"produtoId\":"+cafe.getId()+",\"quantidade\":1,\"preco\":0.01}]}";
        mvc.perform(postJson("/api/pedidos/cotacao",manipulado)).andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"nomeCliente","telefoneCliente","emailCliente"})
    void validaCadaContatoIndividualmente(String campo) throws Exception {
        var body=json.readTree(serializar(request(TipoAtendimento.RETIRADA,linhas(1))));
        ((tools.jackson.databind.node.ObjectNode)body).put(campo,campo.equals("nomeCliente")?" ":"invalido");
        mvc.perform(postJson("/api/pedidos",body.toString()).header("Idempotency-Key",chave())).andExpect(status().isBadRequest());
        assertEquals(0,pedidos.count());
    }
    @Test void cotacaoInexistenteEChaveAusenteNaoGravam() throws Exception {
        var req=request(TipoAtendimento.RETIRADA,linhas(1));
        mvc.perform(postJson("/api/pedidos",serializar(req))).andExpect(status().isBadRequest());
        var invalido=new PedidoRequest(req.nomeCliente(),req.telefoneCliente(),req.emailCliente(),req.tipoAtendimento(),req.formaPagamento(),null,null,req.itens(),"x".repeat(43));
        mvc.perform(postJson("/api/pedidos",serializar(invalido)).header("Idempotency-Key",chave())).andExpect(status().isConflict());
        assertEquals(0,pedidos.count());assertEquals(0,itensSalvos.count());
    }
    @Test void limiteMonetarioNaoGravaCotacao() {
        cafe.atualizar(cafe.getNome(),null,new BigDecimal("99999999.99"),null,categoria,true,true);produtos.save(cafe);
        assertThrows(EntradaInvalidaException.class,()->service.cotar(new CotacaoRequest(TipoAtendimento.RETIRADA,linhas(2))));
        assertEquals(0,cotacoes.count());
    }
    @Test void perfilNormalNaoCarregaDadosDevEBancoEhMemoria() throws Exception {
        assertFalse(context.containsBean("cargaCardapioDev"));
        try(var c=Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            assertTrue(c.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
    }

}
