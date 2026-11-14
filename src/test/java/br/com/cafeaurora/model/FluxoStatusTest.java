package br.com.cafeaurora.model;
import br.com.cafeaurora.enums.*;
import br.com.cafeaurora.exception.RegraNegocioException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
class FluxoStatusTest {
    static Stream<Arguments> pares(){return Arrays.stream(TipoAtendimento.values()).flatMap(t -> Arrays.stream(StatusPedido.values()).flatMap(a -> Arrays.stream(StatusPedido.values()).map(n -> Arguments.of(t,a,n))));}
    @ParameterizedTest @MethodSource("pares")
    void matrizCompleta(TipoAtendimento tipo,StatusPedido atual,StatusPedido novo){
        Pedido p=new Pedido("Cliente","11999999999","a@example.com","Rua",null,tipo,FormaPagamento.DINHEIRO,BigDecimal.ZERO,"ref","idem","req","cot");
        ReflectionTestUtils.setField(p,"status",atual);
        boolean permitido=switch(atual){
            case RECEBIDO -> novo==StatusPedido.EM_PREPARO||novo==StatusPedido.CANCELADO;
            case EM_PREPARO -> novo==StatusPedido.PRONTO||novo==StatusPedido.CANCELADO;
            case PRONTO -> novo==(tipo==TipoAtendimento.ENTREGA?StatusPedido.EM_ENTREGA:StatusPedido.CONCLUIDO)||novo==StatusPedido.CANCELADO;
            case EM_ENTREGA -> tipo==TipoAtendimento.ENTREGA&&novo==StatusPedido.CONCLUIDO;
            default -> false;
        };
        assertEquals(permitido,p.proximosStatus().contains(novo));
        assertThrows(RegraNegocioException.class,()->p.alterarStatus(null));
        if(permitido){p.alterarStatus(novo);assertEquals(novo,p.getStatus());}
        else {assertThrows(RegraNegocioException.class,()->p.alterarStatus(novo));assertEquals(atual,p.getStatus());}
    }
}
