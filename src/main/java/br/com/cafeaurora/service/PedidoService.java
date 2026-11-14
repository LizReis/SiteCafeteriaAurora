package br.com.cafeaurora.service;

import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.enums.TipoAtendimento;
import br.com.cafeaurora.exception.*;
import br.com.cafeaurora.model.*;
import br.com.cafeaurora.repository.*;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final HistoricoStatusPedidoService historico;
    private final ProdutoRepository produtos;
    private final CotacaoPedidoRepository cotacoes;
    private final Validator validator;
    private final TransactionTemplate transacao;
    private final BigDecimal taxaEntrega;
    private final SecureRandom random = new SecureRandom();
    public PedidoService(PedidoRepository pedidos, ProdutoRepository produtos, CotacaoPedidoRepository cotacoes,
        HistoricoStatusPedidoService historico, Validator validator, PlatformTransactionManager manager,
        @Value("${cafe.pedido.taxa-entrega:7.00}") BigDecimal taxaEntrega) {
        this.historico=historico; this.pedidos=pedidos; this.produtos=produtos; this.cotacoes=cotacoes; this.validator=validator;
        this.transacao=new TransactionTemplate(manager);
        if(taxaEntrega.signum()<0 || taxaEntrega.scale()>2 || taxaEntrega.compareTo(new BigDecimal("999999.99"))>0)
            throw new IllegalArgumentException("Taxa de entrega inválida");
        this.taxaEntrega=taxaEntrega.setScale(2);
    }
    public CotacaoResponse cotar(CotacaoRequest request) {
        validar(request);
        return transacao.execute(tx -> {
            var itens=consultarItens(request.itens()).respostas();
            BigDecimal subtotal=subtotal(itens), taxa=taxa(request.tipoAtendimento());
            validarTotal(subtotal.add(taxa));
            String token=token(); Instant expira=Instant.now().plusSeconds(900);
            cotacoes.save(new CotacaoPedido(hash(token), resumo(request.tipoAtendimento(),itens,taxa),expira));
            return new CotacaoResponse(token,expira,itens,subtotal,taxa,subtotal.add(taxa));
        });
    }
    public PedidoResponse criar(PedidoRequest request, String chave) {
        validar(request);
        if(chave==null || !chave.matches("[a-zA-Z0-9_-]{32,100}"))
            throw new EntradaInvalidaException("Idempotency-Key obrigatório, entre 32 e 100 caracteres seguros");
        if(request.tipoAtendimento()==TipoAtendimento.ENTREGA && request.endereco()==null)
            throw new EntradaInvalidaException("Informe o endereço de entrega");
        String idem=hash(chave), requisicao=hash(request.toString());
        try {
            return transacao.execute(tx -> {
                var anterior=pedidos.findByIdempotenciaHash(idem);
                if(anterior.isPresent()) return repetido(anterior.get(),requisicao);
                var cotacao=cotacoes.findById(hash(request.cotacao())).orElseThrow(() ->
                    new RegraNegocioException("Cotação inválida. Atualize o resumo e revise os valores."));
                var validados=consultarItens(request.itens());
                var itens=validados.respostas();
                BigDecimal taxa=taxa(request.tipoAtendimento());
                if(cotacao.getExpiraEm().isBefore(Instant.now()) ||
                    !cotacao.getResumoHash().equals(resumo(request.tipoAtendimento(),itens,taxa)))
                    throw new RegraNegocioException("Preço ou condições alterados. Atualize o resumo e revise os novos valores antes de confirmar.");
                Pedido pedido=new Pedido(request.nomeCliente(), request.telefoneCliente(), request.emailCliente(),
                    request.endereco()==null ? null : request.endereco().formatado(), request.observacoes(),
                    request.tipoAtendimento(),request.formaPagamento(),taxa,token(),idem,requisicao,hash(request.cotacao()));
                for(var item:itens) pedido.adicionarItem(validados.produtos().get(item.produtoId()),item.quantidade());
                pedidos.saveAndFlush(pedido);
                historico.registrar(pedido,null,"Sistema",br.com.cafeaurora.enums.OrigemStatusPedido.SISTEMA);
                return PedidoResponse.de(pedido);
            });
        } catch(DataIntegrityViolationException ex) {
            // A restrição única é a proteção final contra concorrência. A transação
            // anterior já terminou com rollback; a leitura ocorre em outra transação.
            return transacao.execute(tx -> pedidos.findByIdempotenciaHash(idem)
                .map(p -> repetido(p,requisicao)).orElseGet(() -> {
                    if(pedidos.existsByCotacaoHash(hash(request.cotacao())))
                        throw new RegraNegocioException("Esta cotação já foi utilizada. Consulte o pedido registrado ou gere uma nova cotação.");
                    throw new PedidoPersistenciaException();
                }));
        }
    }
    @Transactional(readOnly=true)
    public PedidoResponse consultar(String referencia) {
        if(referencia==null || !referencia.matches("[A-Za-z0-9_-]{43}"))
            throw new RecursoNaoEncontradoException("Pedido não encontrado");
        return pedidos.findByReferenciaPublica(referencia).map(PedidoResponse::de)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }
    private PedidoResponse repetido(Pedido pedido,String requisicao) {
        if(!pedido.getRequisicaoHash().equals(requisicao))
            throw new RegraNegocioException("Chave de idempotência já utilizada com outros dados");
        return PedidoResponse.de(pedido);
    }
    private void validar(Object request) {
        if(request==null || !validator.validate(request).isEmpty())
            throw new EntradaInvalidaException("Dados de entrada inválidos");
    }
    private record ItensValidados(Map<Long,Produto> produtos,List<ItemPedidoResponse> respostas) {}
    private ItensValidados consultarItens(List<ItemPedidoRequest> itens) {
        Set<Long> ids=new HashSet<>(); int quantidade=0;
        for(var item:itens) {
            if(!ids.add(item.produtoId())) throw new EntradaInvalidaException("Produtos duplicados no pedido");
            quantidade+=item.quantidade();
        }
        if(quantidade>200) throw new EntradaInvalidaException("Limite de 200 unidades por pedido");
        var porId=produtos.buscarParaPedido(ids).stream().collect(Collectors.toMap(Produto::getId,Function.identity()));
        var respostas=itens.stream().sorted(Comparator.comparing(ItemPedidoRequest::produtoId)).map(item -> {
            Produto p=porId.get(item.produtoId());
            if(p==null) throw new RecursoNaoEncontradoException("Produto não encontrado: "+item.produtoId());
            if(!p.isAtivo() || !p.isDisponivel() || !p.getCategoria().isAtiva())
                throw new RegraNegocioException("Produto indisponível: "+p.getNome());
            return new ItemPedidoResponse(p.getId(),p.getNome(),item.quantidade(),p.getPreco(),
                p.getPreco().multiply(BigDecimal.valueOf(item.quantidade())), CardapioProduto.imagemSegura(p.getImagemUrl()));
        }).toList();
        return new ItensValidados(porId,respostas);
    }
    private BigDecimal subtotal(List<ItemPedidoResponse> itens) {
        return itens.stream().map(ItemPedidoResponse::subtotal).reduce(BigDecimal.ZERO.setScale(2),BigDecimal::add);
    }
    private BigDecimal taxa(TipoAtendimento tipo) { return tipo==TipoAtendimento.ENTREGA?taxaEntrega:BigDecimal.ZERO.setScale(2); }
    private String resumo(TipoAtendimento tipo,List<ItemPedidoResponse> itens,BigDecimal taxa) {
        return hash(tipo+"|"+taxa+"|"+itens.toString());
    }
    private void validarTotal(BigDecimal total) {
        if(total.compareTo(new BigDecimal("99999999.99"))>0) throw new EntradaInvalidaException("Valor do pedido excede o limite");
    }
    private String token() { byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String texto) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 indisponível",ex); }
    }
}
