package br.com.cafeaurora.service;

import br.com.cafeaurora.enums.*;
import br.com.cafeaurora.exception.RecursoNaoEncontradoException;
import br.com.cafeaurora.model.Pedido;
import br.com.cafeaurora.repository.PedidoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminPedidoService {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
        .withZone(ZoneId.of("America/Bahia"));
    private final PedidoRepository repositorio;

    public AdminPedidoService(PedidoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public record Resumo(Long id, String cliente, String recebidoEm, String situacao,
                         StatusPedido status, TipoAtendimento atendimento, BigDecimal total) {}

    public record Item(String nome, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {}

    public record Detalhe(Long id, String cliente, String telefone, String email,
                          String endereco, String observacoes, String recebidoEm,
                          TipoAtendimento atendimento, FormaPagamento pagamento,
                          StatusPagamento statusPagamento, StatusPedido status, String situacao,
                          BigDecimal subtotal, BigDecimal taxaEntrega, BigDecimal total,
                          List<Item> itens, List<StatusPedido> proximosStatus) {}

    @Transactional(readOnly = true)
    public Page<Resumo> listar(StatusPedido status, int pagina, int tamanho) {
        Pageable paginaSolicitada = PageRequest.of(Math.max(0, pagina), tamanho,
            Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id")));
        Page<Pedido> resultado = status == null
            ? repositorio.findAll(paginaSolicitada)
            : repositorio.findByStatus(status, paginaSolicitada);
        return resultado.map(p -> new Resumo(p.getId(), p.getNomeCliente(),
            formatar(p.getCriadoEm()), rotulo(p.getStatus()), p.getStatus(),
            p.getTipoAtendimento(), p.getTotal()));
    }

    @Transactional(readOnly = true)
    public Detalhe detalhar(Long id) {
        Pedido p = repositorio.buscarComItens(id).orElseThrow(() ->
            new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
        List<Item> itens = p.getItens().stream().map(i ->
            new Item(i.getNomeProduto(), i.getQuantidade(), i.getPrecoUnitario(), i.getSubtotal())).toList();
        return new Detalhe(p.getId(), p.getNomeCliente(), p.getTelefoneCliente(),
            p.getEmailCliente(), p.getEnderecoEntrega(), p.getObservacoes(),
            formatar(p.getCriadoEm()), p.getTipoAtendimento(), p.getFormaPagamento(),
            p.getStatusPagamento(), p.getStatus(), rotulo(p.getStatus()),
            p.getSubtotal(), p.getTaxaEntrega(), p.getTotal(), itens, p.proximosStatus());
    }

    @Transactional
    public void atualizarStatus(Long id, StatusPedido novoStatus) {
        Pedido pedido = repositorio.findById(id).orElseThrow(() ->
            new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
        pedido.alterarStatus(novoStatus);
    }

    public long contar() { return repositorio.count(); }
    public long contar(StatusPedido status) { return repositorio.countByStatus(status); }

    public static String rotulo(StatusPedido status) {
        return switch (status) {
            case RECEBIDO -> "Recebido";
            case EM_PREPARO -> "Em preparo";
            case PRONTO -> "Pronto";
            case CONCLUIDO -> "Concluído";
            case CANCELADO -> "Cancelado";
        };
    }

    private static String formatar(Instant quando) {
        return quando == null ? "Sem registro" : DATA.format(quando);
    }
}
