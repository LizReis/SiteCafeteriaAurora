package br.com.cafeaurora;
import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.enums.*;
import br.com.cafeaurora.model.*;
import br.com.cafeaurora.repository.*;
import br.com.cafeaurora.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:historico_etapa6;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class HistoricoStatusTests {
    @Autowired PedidoService checkout;
    @Autowired AdminPedidoService admin;
    @Autowired PedidoRepository pedidos;
    @Autowired ProdutoRepository produtos;
    @Autowired CategoriaRepository categorias;
    @Autowired HistoricoStatusPedidoService historico;
    @MockitoSpyBean HistoricoStatusPedidoRepository eventos;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    Produto produto;
    @BeforeEach void preparar(){
        var c=categorias.save(new Categoria(UUID.randomUUID().toString(),null,true,TipoCardapio.CAFE,0));
        produto=produtos.save(new Produto("Teste",null,new BigDecimal("10.00"),null,c,true,true));
    }
    PedidoRequest request(){
        var itens=List.of(new ItemPedidoRequest(produto.getId(),1));
        var cot=checkout.cotar(new CotacaoRequest(TipoAtendimento.ENTREGA,itens));
        return new PedidoRequest("Cliente","11999999999","c@example.com",TipoAtendimento.ENTREGA,FormaPagamento.DINHEIRO,
            new EnderecoRequest("01310000","Rua","1",null,"Bairro"),null,itens,cot.cotacao());
    }
    PedidoResponse novo(){return checkout.criar(request(),UUID.randomUUID().toString());}
    @AfterEach void limparIdentidade(){SecurityContextHolder.clearContext();}
    @Test void criacaoIdempotenteEEventosOrdenados(){
        Instant antes=Instant.now().minusSeconds(1);
        var req=request();var chave=UUID.randomUUID().toString();var p=checkout.criar(req,chave);
        assertEquals(p.id(),checkout.criar(req,chave).id());
        var inicial=eventos.findByPedidoIdOrderByAlteradoEmAscIdAsc(p.id());assertEquals(1,inicial.size());
        assertNull(inicial.getFirst().getStatusAnterior());assertEquals(OrigemStatusPedido.SISTEMA,inicial.getFirst().getOrigem());
        assertEquals(p.criadoEm(),inicial.getFirst().getAlteradoEm());assertTrue(inicial.getFirst().getAlteradoEm().isAfter(antes));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("operador",null,List.of()));
        for(var s:List.of(StatusPedido.EM_PREPARO,StatusPedido.PRONTO,StatusPedido.EM_ENTREGA,StatusPedido.CONCLUIDO))admin.atualizarStatus(p.id(),s);
        var lista=eventos.findByPedidoIdOrderByAlteradoEmAscIdAsc(p.id());assertEquals(5,lista.size());
        for(int i=1;i<lista.size();i++){
            assertEquals(lista.get(i-1).getStatusNovo(),lista.get(i).getStatusAnterior());
            assertEquals("operador",lista.get(i).getResponsavel());assertEquals(OrigemStatusPedido.ADMINISTRADOR,lista.get(i).getOrigem());
            assertFalse(lista.get(i).getAlteradoEm().isBefore(lista.get(i-1).getAlteradoEm()));
        }
        assertEquals(historico.listar(p.id()),historico.listar(p.id()));
        assertThrows(RuntimeException.class,()->admin.atualizarStatus(p.id(),StatusPedido.CANCELADO));
        assertEquals(5,eventos.findByPedidoIdOrderByAlteradoEmAscIdAsc(p.id()).size());
    }
    @Test void falhaNoHistoricoReverteStatus(){
        var p=novo();doThrow(new IllegalStateException("Falha simulada")).when(eventos).save(any());
        assertThrows(IllegalStateException.class,()->admin.atualizarStatus(p.id(),StatusPedido.EM_PREPARO));
        assertEquals(StatusPedido.RECEBIDO,pedidos.findById(p.id()).orElseThrow().getStatus());
        assertEquals(1,eventos.findByPedidoIdOrderByAlteradoEmAscIdAsc(p.id()).size());
    }
    @Test void falhaNoHistoricoReverteCriacao(){
        long antes=pedidos.count();doThrow(new IllegalStateException("Falha simulada")).when(eventos).save(any());
        assertThrows(IllegalStateException.class,()->novo());assertEquals(antes,pedidos.count());
    }
    @Test void legadoNaoInventaEventos(){
        var p=novo();jdbc.update("delete from historico_status_pedido where pedido_id=?",p.id());
        assertTrue(historico.listar(p.id()).isEmpty());
        admin.atualizarStatus(p.id(),StatusPedido.EM_PREPARO);
        assertEquals(1,historico.listar(p.id()).size());
    }
    @Test void administradoresConcorrentesNaoSobrescrevemEstado() throws Exception {
        var p=novo();admin.atualizarStatus(p.id(),StatusPedido.EM_PREPARO);admin.atualizarStatus(p.id(),StatusPedido.PRONTO);
        try(var executor=Executors.newFixedThreadPool(2)){
            var barreira=new CyclicBarrier(2);
            var tarefas=List.of(StatusPedido.EM_ENTREGA,StatusPedido.CANCELADO).stream().map(s->executor.submit(()->{
                barreira.await(10,TimeUnit.SECONDS);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin-"+s,null,List.of()));
                try{admin.atualizarStatus(p.id(),s);return true;}catch(br.com.cafeaurora.exception.RegraNegocioException e){return false;}
                finally{SecurityContextHolder.clearContext();}
            })).toList();
            int sucesso=0;for(var tarefa:tarefas)if(tarefa.get(15,TimeUnit.SECONDS))sucesso++;
            assertEquals(1,sucesso);
        }
        var lista=eventos.findByPedidoIdOrderByAlteradoEmAscIdAsc(p.id());assertEquals(4,lista.size());
        assertEquals(StatusPedido.PRONTO,lista.getLast().getStatusAnterior());
        assertEquals("admin-"+lista.getLast().getStatusNovo(),lista.getLast().getResponsavel());
        assertEquals(pedidos.findById(p.id()).orElseThrow().getStatus(),lista.getLast().getStatusNovo());
    }
}
