package br.com.cafeaurora.controller;
import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidos;
    public PedidoController(PedidoService pedidos) { this.pedidos=pedidos; }
    @PostMapping("/cotacao")
    public ResponseEntity<CotacaoResponse> cotar(@Valid @RequestBody CotacaoRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pedidos.cotar(request));
    }
    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest request,
        @RequestHeader(name="Idempotency-Key",required=false) String chave) {
        var resposta=pedidos.criar(request,chave);
        return ResponseEntity.created(URI.create("/api/pedidos/consulta/"+resposta.referencia()))
            .cacheControl(CacheControl.noStore()).body(resposta);
    }
    @GetMapping("/consulta/{referencia}")
    public ResponseEntity<PedidoResponse> consultar(@PathVariable String referencia) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pedidos.consultar(referencia));
    }
}
