package br.com.cafeaurora.model;
import br.com.cafeaurora.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class PedidoTest {
    Pedido pedido(){return new Pedido("Cliente","11999999999","email@example.com",null,null,TipoAtendimento.RETIRADA,FormaPagamento.PIX,BigDecimal.ZERO,"referencia","idem","request","quote");}
    Produto produto(){var p=new Produto("Café",null,new BigDecimal("10.35"),null,new Categoria("Café",null,true),true,true);ReflectionTestUtils.setField(p,"id",1L);return p;}
    @Test void associaAmbosOsLadosECalculaValoresExatos(){var p=pedido();p.adicionarItem(produto(),2);assertSame(p,p.getItens().getFirst().getPedido());assertEquals(new BigDecimal("20.70"),p.getTotal());assertThrows(UnsupportedOperationException.class,()->p.getItens().clear());}
    @Test void naoPermiteEstadoInvalido(){assertThrows(IllegalArgumentException.class,()->pedido().adicionarItem(produto(),0));assertThrows(IllegalArgumentException.class,()->pedido().adicionarItem(produto(),100));assertThrows(IllegalStateException.class,()->pedido().antesDeSalvar());}
    @Test void naoPermiteProdutoDuplicado(){var p=pedido();var produto=produto();p.adicionarItem(produto,1);assertThrows(IllegalArgumentException.class,()->p.adicionarItem(produto,2));}
    @Test void historicoNaoDependeDePrecoOuNomeAtual(){var p=pedido();var produto=produto();p.adicionarItem(produto,1);produto.atualizar("Outro",null,new BigDecimal("15.00"),null,produto.getCategoria(),true,true);assertEquals("Café",p.getItens().getFirst().getNomeProduto());assertEquals(new BigDecimal("10.35"),p.getItens().getFirst().getPrecoUnitario());}
}
