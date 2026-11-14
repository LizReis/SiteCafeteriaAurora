package br.com.cafeaurora;

import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.enums.*;
import br.com.cafeaurora.exception.*;
import br.com.cafeaurora.model.*;
import br.com.cafeaurora.repository.*;
import br.com.cafeaurora.service.*;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:avaliacoes_test;DB_CLOSE_DELAY=-1","spring.jpa.open-in-view=false",
    "cafe.admin.username=admin","cafe.admin.password=senha-exclusiva-para-testes"})
class AvaliacaoIntegracaoTests {
    @Autowired AvaliacaoService service;
    @Autowired AvaliacaoRepository avaliacoes;
    @Autowired AvaliacaoFotoRepository fotos;
    @Autowired PedidoRepository pedidos;
    @Autowired ProdutoRepository produtos;
    @Autowired CategoriaRepository categorias;
    @Autowired CotacaoPedidoRepository cotacoes;
    @Autowired PedidoService checkout;
    @Autowired AdminPedidoService admin;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    @Autowired Filter springSecurityFilterChain;
    MockMvc mvc; MockHttpSession session; CsrfToken csrf;
    Produto produto; PedidoResponse pedido;
    @BeforeEach void preparar() throws Exception {
        fotos.deleteAll();avaliacoes.deleteAll();jdbc.update("delete from historico_status_pedido");pedidos.deleteAll();cotacoes.deleteAll();produtos.deleteAll();categorias.deleteAll();
        var c=categorias.save(new Categoria("Café",null,true,TipoCardapio.CAFE,0));
        produto=produtos.save(new Produto("Produto histórico",null,new BigDecimal("10.00"),"/img/1320.jpg",c,true,true));
        pedido=novoPedido(TipoAtendimento.RETIRADA);
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();token();
    }
    PedidoResponse novoPedido(TipoAtendimento tipo) {
        var itens=List.of(new ItemPedidoRequest(produto.getId(),1));
        var cotacao=checkout.cotar(new CotacaoRequest(tipo,itens));
        return checkout.criar(new PedidoRequest("Comprador privado","11999999999","privado@example.com",tipo,FormaPagamento.DINHEIRO,
            tipo==TipoAtendimento.ENTREGA?new EnderecoRequest("01310000","Rua fictícia","1",null,"Bairro"):null,
            "Privado",itens,cotacao.cotacao()),UUID.randomUUID().toString());
    }
    void concluir(PedidoResponse p) {
        admin.atualizarStatus(p.id(),StatusPedido.EM_PREPARO);admin.atualizarStatus(p.id(),StatusPedido.PRONTO);
        if(p.tipoAtendimento()==TipoAtendimento.ENTREGA)admin.atualizarStatus(p.id(),StatusPedido.EM_ENTREGA);
        admin.atualizarStatus(p.id(),StatusPedido.CONCLUIDO);
    }
    void token() throws Exception {
        var r=mvc.perform(get("/pedido-confirmado").session(session==null?new MockHttpSession():session)).andExpect(status().isOk()).andReturn();
        csrf=(CsrfToken)r.getRequest().getAttribute(CsrfToken.class.getName());csrf.getToken();session=(MockHttpSession)r.getRequest().getSession(false);
    }
    void login() throws Exception {
        var r=mvc.perform(post("/admin/login").session(session).param(csrf.getParameterName(),csrf.getToken())
            .param("username","admin").param("password","senha-exclusiva-para-testes")).andExpect(redirectedUrl("/admin")).andReturn();
        session=(MockHttpSession)r.getRequest().getSession(false);token();
    }
    AvaliacaoRequest req(){return new AvaliacaoRequest(pedido.referencia(),"  Nome escolhido  ",5,"  Experiência muito boa!  ",true);}
    MockMultipartHttpServletRequestBuilder envio() {
        var r=multipart("/api/avaliacoes");
        r.session(session).header(csrf.getHeaderName(),csrf.getToken()).param("referencia",pedido.referencia())
            .param("nomeAutor","Nome escolhido").param("nota","5").param("comentario","Experiência muito boa!").param("consentimentoPublicacao","true");
        return r;
    }
    MockMultipartFile imagem(String formato) throws Exception {
        if(formato.equals("webp"))return new MockMultipartFile("foto","irrelevante.exe","image/webp",Base64.getDecoder().decode("UklGRjYAAABXRUJQVlA4ICoAAADwAQCdASoCAAIAAUAmJaACdLoB+AAGXYAA/si//8wSvyDc6/9uwWcsYAA="));
        var b=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB),formato,b);
        return new MockMultipartFile("foto","../../nome."+formato,"image/"+formato,b.toByteArray());
    }
    Long registrar() {concluir(pedido);service.registrar(req(),null);return avaliacoes.findAll().stream().filter(a->a.getPedido().getId().equals(pedido.id())).findFirst().orElseThrow().getId();}

    MockMultipartFile fotoQueNaoPodeSerProcessada() {
        return new MockMultipartFile("foto","invalida.png","image/png",new byte[]{1,2,3}) {
            @Override public long getSize(){throw new AssertionError("Imagem examinada antes da autorização");}
            @Override public java.io.InputStream getInputStream(){throw new AssertionError("Imagem lida antes da autorização");}
        };
    }
    @Test void referenciaInexistenteRejeitadaAntesDaImagem() throws Exception {
        var referencia="x".repeat(43);
        mvc.perform(envio().file(new MockMultipartFile("foto","invalida.png","image/png",new byte[]{1,2,3}))
            .with(r->{r.setParameter("referencia",referencia);return r;})).andExpect(status().isNotFound());
        assertThrows(RecursoNaoEncontradoException.class,()->service.registrar(
            new AvaliacaoRequest(referencia,"Autor",5,"Muito bom",true),fotoQueNaoPodeSerProcessada()));
        assertEquals(0,avaliacoes.count());assertEquals(0,fotos.count());
    }
    @ParameterizedTest @EnumSource(value=StatusPedido.class,names={"CONCLUIDO"},mode=EnumSource.Mode.EXCLUDE)
    void pedidoInelegivelRejeitadoAntesDaImagem(StatusPedido status) throws Exception {
        jdbc.update("update pedidos set status=? where id=?",status.name(),pedido.id());
        mvc.perform(envio().file(new MockMultipartFile("foto","invalida.png","image/png",new byte[]{1,2,3})))
            .andExpect(status().isConflict());
        assertThrows(RegraNegocioException.class,()->service.registrar(req(),fotoQueNaoPodeSerProcessada()));
        assertEquals(0,avaliacoes.count());assertEquals(0,fotos.count());
    }
    @ParameterizedTest @EnumSource(StatusModeracao.class)
    void pedidoJaAvaliadoRejeitadoAntesDaImagem(StatusModeracao status) throws Exception {
        var id=registrar();if(status!=StatusModeracao.PENDENTE)service.moderar(id,status);
        mvc.perform(envio().file(new MockMultipartFile("foto","invalida.png","image/png",new byte[]{1,2,3})))
            .andExpect(status().isConflict());
        assertThrows(RegraNegocioException.class,()->service.registrar(req(),fotoQueNaoPodeSerProcessada()));
        assertEquals(1,avaliacoes.count());assertEquals(0,fotos.count());
    }
    MockMultipartFile fotoComAlteracaoConcorrente(Runnable alteracao) throws Exception {
        return new MockMultipartFile("foto","valida.png","image/png",imagem("png").getBytes()) {
            @Override public java.io.InputStream getInputStream() throws java.io.IOException {
                assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),
                    "Processamento da imagem deve ficar fora da transação de autorização/gravação");
                var executor=Executors.newSingleThreadExecutor();
                try {
                    executor.submit(alteracao).get(10,TimeUnit.SECONDS);
                } catch(Exception ex) {throw new AssertionError("Alteração concorrente bloqueada durante leitura da foto",ex);}
                finally {executor.shutdownNow();}
                return super.getInputStream();
            }
        };
    }
    @Test void revalidaStatusAlteradoDuranteImagemSemManterLock() throws Exception {
        concluir(pedido);
        var foto=fotoComAlteracaoConcorrente(()->jdbc.update("update pedidos set status='CANCELADO' where id=?",pedido.id()));
        assertThrows(RegraNegocioException.class,()->service.registrar(req(),foto));
        assertEquals(0,avaliacoes.count());assertEquals(0,fotos.count());
    }
    @Test void revalidaDuplicidadeCriadaDuranteImagemSemManterLock() throws Exception {
        concluir(pedido);
        var foto=fotoComAlteracaoConcorrente(()->service.registrar(req(),null));
        assertThrows(RegraNegocioException.class,()->service.registrar(req(),foto));
        assertEquals(1,avaliacoes.count());assertEquals(0,fotos.count());
    }
    @Test void revalidaExistenciaAposImagem() throws Exception {
        concluir(pedido);
        var foto=fotoComAlteracaoConcorrente(()->{jdbc.update("delete from historico_status_pedido where pedido_id=?",pedido.id());pedidos.deleteById(pedido.id());});
        assertThrows(RecursoNaoEncontradoException.class,()->service.registrar(req(),foto));
        assertEquals(0,avaliacoes.count());assertEquals(0,fotos.count());
    }

    @ParameterizedTest @EnumSource(TipoAtendimento.class)
    void avaliacaoValidaPendenteEPedidoIntacto(TipoAtendimento tipo) throws Exception {
        pedido=novoPedido(tipo);concluir(pedido);
        mvc.perform(envio()).andExpect(status().isCreated()).andExpect(jsonPath("$.mensagem").value(containsString("aguardando aprovação")))
            .andExpect(content().string(not(containsString(pedido.referencia())))).andExpect(content().string(not(containsString("privado@example.com"))));
        var a=avaliacoes.findAll().getFirst();assertEquals(pedido.id(),a.getPedido().getId());assertEquals(StatusModeracao.PENDENTE,a.getStatusModeracao());
        assertTrue(a.isConsentimentoPublicacao());assertNotNull(a.getCriadaEm());assertNull(a.getModeradaEm());
        var salvo=checkout.consultar(pedido.referencia());assertTrue(salvo.jaAvaliado());assertEquals(pedido.total(),salvo.total());
        assertEquals(StatusPagamento.PENDENTE,salvo.statusPagamento());assertEquals("Produto histórico",salvo.itens().getFirst().nomeProduto());
        assertTrue(service.publicas().isEmpty());assertEquals(0,fotos.count());
    }
    @ParameterizedTest @EnumSource(value=StatusPedido.class,names={"CONCLUIDO"},mode=EnumSource.Mode.EXCLUDE)
    void rejeitaStatusNaoConcluido(StatusPedido status) throws Exception {
        jdbc.update("update pedidos set status=? where id=?",status.name(),pedido.id());
        mvc.perform(envio()).andExpect(status().isConflict());assertEquals(0,avaliacoes.count());
    }
    static Stream<Arguments> invalidos() {
        return Stream.of(Arguments.of("A",5,"Ótimo café",true),Arguments.of(" ",5,"Ótimo café",true),Arguments.of("a".repeat(101),5,"Ótimo café",true),
            Arguments.of("Autor",0,"Ótimo café",true),Arguments.of("Autor",6,"Ótimo café",true),Arguments.of("Autor",null,"Ótimo café",true),
            Arguments.of("Autor",5," ",true),Arguments.of("Autor",5,"abcd",true),Arguments.of("Autor",5,"c".repeat(1001),true),
            Arguments.of("Autor",5,"Ótimo café",false),Arguments.of("Autor",5,"Ótimo café",null));
    }
    @ParameterizedTest @MethodSource("invalidos")
    void validaEntradaNoService(String nome,Integer nota,String comentario,Boolean consentimento) {
        concluir(pedido);assertThrows(EntradaInvalidaException.class,()->service.registrar(new AvaliacaoRequest(pedido.referencia(),nome,nota,comentario,consentimento),null));
        assertEquals(0,avaliacoes.count());
    }
    @ParameterizedTest @ValueSource(strings={"1","invalida","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void referenciaInvalidaOuInexistente(String referencia) throws Exception {
        concluir(pedido);mvc.perform(envio().with(r->{r.setParameter("referencia",referencia);return r;})).andExpect(status().isNotFound());
        assertThrows(RecursoNaoEncontradoException.class,()->service.registrar(new AvaliacaoRequest(referencia,"Autor",5,"Muito bom",true),null));
    }
    @Test void csrfObrigatorioECamposIndevidos() throws Exception {
        concluir(pedido);
        mvc.perform(multipart("/api/avaliacoes")).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/avaliacoes").session(session).header(csrf.getHeaderName(),"invalido")).andExpect(status().isForbidden());
        for(var campo:List.of("id","pedidoId","statusModeracao","criadaEm","moderadaEm","status"))
            mvc.perform(envio().param(campo,"APROVADA")).andExpect(status().isBadRequest());
        assertEquals(0,avaliacoes.count());
    }
    @Test void rejeitaReferenciaNaQueryENotaFracionaria() throws Exception {
        concluir(pedido);
        mvc.perform(envio().with(r->{r.setQueryString("referencia=nao-utilizar-query");return r;})).andExpect(status().isBadRequest());
        mvc.perform(envio().with(r->{r.setParameter("nota","4.5");return r;})).andExpect(status().isBadRequest());
        assertEquals(0,avaliacoes.count());
    }
    @Test void duplicidadeERejeicaoNaoLiberamNovoEnvio() throws Exception {
        var id=registrar();service.moderar(id,StatusModeracao.REJEITADA);
        mvc.perform(envio()).andExpect(status().isConflict());assertEquals(1,avaliacoes.count());
        mvc.perform(get("/api/pedidos/consulta/"+pedido.referencia())).andExpect(jsonPath("$.jaAvaliado").value(true));
    }
    @Test void enviosConcorrentesUmSucessoUmConflito() throws Exception {
        concluir(pedido);var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<String> job=()->{start.await();try{service.registrar(req(),imagem("png"));return "criado";}catch(RegraNegocioException e){return "conflito";}};
            var a=pool.submit(job);var b=pool.submit(job);start.countDown();
            assertEquals(Set.of("criado","conflito"),Set.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS)));
        }
        assertEquals(1,avaliacoes.count());assertEquals(1,fotos.count());
    }
    @ParameterizedTest @ValueSource(strings={"jpeg","png","webp"})
    void fotoDecodificadaPersistidaENormalizada(String formato) throws Exception {
        concluir(pedido);mvc.perform(envio().file(imagem(formato))).andExpect(status().isCreated());
        var id=avaliacoes.findAll().getFirst().getId();
        var f=fotos.findByAvaliacaoId(id).orElseThrow();assertEquals("image/jpeg",f.getTipoMime());
        assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(f.getConteudo())));
        mvc.perform(get("/api/avaliacoes/"+id+"/foto")).andExpect(status().isNotFound());
        login();mvc.perform(get("/admin/avaliacoes/"+id+"/foto").session(session)).andExpect(status().isOk());
        mvc.perform(post("/admin/avaliacoes/"+id+"/aprovar").session(session).param(csrf.getParameterName(),csrf.getToken())).andExpect(redirectedUrl("/admin/avaliacoes"));
        mvc.perform(get("/api/avaliacoes/"+id+"/foto")).andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"))
            .andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(header().string("Cache-Control",containsString("no-store")));
        service.moderar(id,StatusModeracao.REJEITADA);mvc.perform(get("/api/avaliacoes/"+id+"/foto")).andExpect(status().isNotFound());
    }
    @Test void fotoInvalidaVaziaDisfarcadaCorrompidaEMimeDivergente() throws Exception {
        concluir(pedido);
        for(var f:List.of(new MockMultipartFile("foto","x.png","image/png",new byte[0]),
            new MockMultipartFile("foto","x.jpg","image/jpeg","<svg><script>alert(1)</script></svg>".getBytes()),
            new MockMultipartFile("foto","x.png","image/png",Arrays.copyOf(imagem("png").getBytes(),20)),
            new MockMultipartFile("foto","x.png","image/png",imagem("jpeg").getBytes()))) {
            mvc.perform(envio().file(f)).andExpect(status().isBadRequest());
        }
        assertEquals(0,avaliacoes.count());assertEquals(0,fotos.count());
    }
    @Test void limiteArquivoEMultiplosEDimensoes() throws Exception {
        concluir(pedido);
        mvc.perform(envio().file(new MockMultipartFile("foto","x.png","image/png",new byte[5*1024*1024+1]))).andExpect(status().isPayloadTooLarge());
        mvc.perform(envio().file(imagem("png")).file(imagem("jpeg"))).andExpect(status().isBadRequest());
        var b=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(4097,1,BufferedImage.TYPE_INT_RGB),"png",b);
        mvc.perform(envio().file(new MockMultipartFile("foto","x.png","image/png",b.toByteArray()))).andExpect(status().isBadRequest());
        assertEquals(0,fotos.count());assertEquals(0,avaliacoes.count());
    }
    @Test void rollbackQuandoFotoFalhaSemOrfaos() throws Exception {
        concluir(pedido);jdbc.execute("alter table avaliacoes_fotos add constraint falha_foto check (tipo_mime <> 'image/jpeg')");
        try {
            mvc.perform(envio().file(imagem("png"))).andExpect(status().isInternalServerError()).andExpect(content().string(not(containsString(pedido.referencia()))));
            assertEquals(0,avaliacoes.count());assertEquals(0,fotos.count());assertFalse(checkout.consultar(pedido.referencia()).jaAvaliado());
        } finally {jdbc.execute("alter table avaliacoes_fotos drop constraint falha_foto");}
        mvc.perform(envio().file(imagem("png"))).andExpect(status().isCreated());
    }
    @Test void moderacaoExigeAdminCsrfENaoAlteraOriginal() throws Exception {
        var id=registrar();
        mvc.perform(get("/admin/avaliacoes").accept("text/html")).andExpect(status().isFound());
        mvc.perform(get("/admin/avaliacoes/"+id+"/foto").accept("text/html")).andExpect(status().isFound());
        mvc.perform(post("/admin/avaliacoes/"+id+"/aprovar").session(session).param(csrf.getParameterName(),csrf.getToken()).accept("text/html")).andExpect(status().isFound());
        assertEquals(StatusModeracao.PENDENTE,avaliacoes.findById(id).orElseThrow().getStatusModeracao());
        login();mvc.perform(get("/admin/avaliacoes").session(session)).andExpect(status().isOk()).andExpect(content().string(containsString("Nome escolhido")));
        mvc.perform(post("/admin/avaliacoes/"+id+"/aprovar").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/avaliacoes/999999/aprovar").session(session).param(csrf.getParameterName(),csrf.getToken())).andExpect(status().isNotFound());
        for(var s:List.of(StatusModeracao.APROVADA,StatusModeracao.REJEITADA)) {
            mvc.perform(post("/admin/avaliacoes/"+id+(s==StatusModeracao.APROVADA?"/aprovar":"/rejeitar")).session(session).param(csrf.getParameterName(),csrf.getToken())).andExpect(status().isFound());
            var a=avaliacoes.findById(id).orElseThrow();assertNotNull(a.getModeradaEm());assertEquals("Nome escolhido",a.getNomeAutor());
            assertEquals(s==StatusModeracao.APROVADA?1:0,service.publicas().size());
        }
        assertEquals(StatusPagamento.PENDENTE,checkout.consultar(pedido.referencia()).statusPagamento());
    }
    @ParameterizedTest @ValueSource(ints={0,1,2,3,4,5,7})
    void homeSomenteAprovadasLimiteCincoEOrdem(int quantidade) throws Exception {
        var ids=new ArrayList<Long>();
        for(int i=0;i<quantidade;i++) {pedido=novoPedido(TipoAtendimento.RETIRADA);ids.add(registrar());}
        // Todas pendentes: a home não pode conter nomes nem comentários.
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(not(containsString("Nome escolhido"))));
        for(var id:ids)service.moderar(id,StatusModeracao.APROVADA);
        var publicas=service.publicas();assertEquals(Math.min(quantidade,5),publicas.size());
        if(quantidade>0)assertEquals(ids.getLast(),publicas.getFirst().id());
        var r=mvc.perform(get("/")).andExpect(status().isOk()).andReturn();
        assertEquals(Math.min(quantidade,5),r.getResponse().getContentAsString().split("class=\"polaroid-card",-1).length-1);
        assertFalse(r.getResponse().getContentAsString().contains("privado@example.com"));
    }
    @Test void textoMaliciosoEscapadoEFotoAusenteUsaHistorico() throws Exception {
        concluir(pedido);service.registrar(new AvaliacaoRequest(pedido.referencia(),"<b>Autor</b>",5,"<script>window.injetado=1</script>",true),null);
        var id=avaliacoes.findAll().getFirst().getId();service.moderar(id,StatusModeracao.APROVADA);
        mvc.perform(get("/")).andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>window.injetado")))).andExpect(content().string(containsString("/img/1320.jpg")));
        mvc.perform(get("/api/avaliacoes/"+id+"/foto")).andExpect(status().isNotFound());
    }
    @Test void unicidadeTambemEhImpostaPeloBanco() {
        registrar();
        var pedidoSalvo=pedidos.findById(pedido.id()).orElseThrow();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->
            avaliacoes.saveAndFlush(new Avaliacao(pedidoSalvo,"Outro autor",4,"Outro comentário")));
        assertEquals(1,avaliacoes.count());assertEquals(0,fotos.count());
    }
    @Test void paginacaoModeracaoFiltrosETimestampEmpatado() throws Exception {
        for(int i=0;i<17;i++) {pedido=novoPedido(TipoAtendimento.RETIRADA);registrar();}
        jdbc.update("update avaliacoes set criada_em=?",java.sql.Timestamp.from(java.time.Instant.parse("2026-01-01T00:00:00Z")));
        var maior=avaliacoes.findAll().stream().map(Avaliacao::getId).max(Long::compareTo).orElseThrow();
        service.moderar(maior,StatusModeracao.APROVADA);
        assertEquals(15,service.listar(null,0).getNumberOfElements());assertEquals(2,service.listar(null,1).getNumberOfElements());
        assertEquals(0,service.listar(null,99).getNumberOfElements());assertEquals(16,service.listar(StatusModeracao.PENDENTE,0).getTotalElements());
        assertEquals(maior,service.listar(null,0).getContent().getFirst().id());
        login();mvc.perform(get("/admin/avaliacoes").session(session).param("status","APROVADA"))
            .andExpect(status().isOk()).andExpect(model().attributeExists("pagina"));
        mvc.perform(get("/admin/avaliacoes").session(session).param("status","INVALIDO")).andExpect(status().isBadRequest());
    }
    @Test void imagemLimitadaPorPixelsMesmoComLadosPermitidos() throws Exception {
        concluir(pedido);var b=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2001,2000,BufferedImage.TYPE_INT_RGB),"png",b);
        mvc.perform(envio().file(new MockMultipartFile("foto","x.png","image/png",b.toByteArray()))).andExpect(status().isBadRequest());
        assertEquals(0,avaliacoes.count());assertEquals(0,fotos.count());
    }
}
