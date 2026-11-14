package br.com.cafeaurora.service;
import br.com.cafeaurora.enums.*;
import br.com.cafeaurora.model.*;
import br.com.cafeaurora.repository.HistoricoStatusPedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
@Service
public class HistoricoStatusPedidoService {
    private static final DateTimeFormatter DATA=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("America/Bahia"));
    private final HistoricoStatusPedidoRepository repositorio;
    public HistoricoStatusPedidoService(HistoricoStatusPedidoRepository repositorio){this.repositorio=repositorio;}
    public record Evento(Long id,String horario,String anterior,String novo,String responsavel,OrigemStatusPedido origem) {}
    @Transactional(propagation=Propagation.MANDATORY)
    public void registrar(Pedido pedido,StatusPedido anterior,String responsavel,OrigemStatusPedido origem) {
        repositorio.save(new HistoricoStatusPedido(pedido,anterior,pedido.getStatus(),
            anterior==null?pedido.getCriadoEm():Instant.now(),responsavel,origem));
    }
    @Transactional(readOnly=true)
    public List<Evento> listar(Long id){
        return repositorio.findByPedidoIdOrderByAlteradoEmAscIdAsc(id).stream().map(e ->
            new Evento(e.getId(),DATA.format(e.getAlteradoEm()),e.getStatusAnterior()==null?null:AdminPedidoService.rotulo(e.getStatusAnterior()),
                AdminPedidoService.rotulo(e.getStatusNovo()),e.getResponsavel(),e.getOrigem())).toList();
    }
}
