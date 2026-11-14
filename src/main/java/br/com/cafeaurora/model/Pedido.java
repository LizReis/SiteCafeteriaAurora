package br.com.cafeaurora.model;

import jakarta.persistence.*;
import br.com.cafeaurora.enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {
    @OneToOne(mappedBy="pedido",fetch=FetchType.LAZY)
    private Avaliacao avaliacao;
    public boolean isJaAvaliado(){return avaliacao!=null;}
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_cliente", nullable = false, length = 120)
    private String nomeCliente;

    @Column(name = "telefone_cliente", length = 30)
    private String telefoneCliente;

    @Column(name = "endereco_entrega", length = 500)
    private String enderecoEntrega;

    @Column(length = 1000)
    private String observacoes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoAtendimento tipoAtendimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusPedido status = StatusPedido.RECEBIDO;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 30)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    // Colunas novas anuláveis permitem abrir tabelas de pedidos legadas.
    @Column(length=150)
    private String emailCliente;
    @Column(precision=10, scale=2)
    private BigDecimal subtotal;
    @Column(precision=10, scale=2)
    private BigDecimal taxaEntrega;
    @Column(length=43, unique=true)
    private String referenciaPublica;
    @Column(length=64, unique=true)
    private String idempotenciaHash;
    @Column(length=64)
    private String requisicaoHash;
    @Column(length=64, unique=true)
    private String cotacaoHash;

    public String getEmailCliente() { return emailCliente; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxaEntrega() { return taxaEntrega; }
    public String getReferenciaPublica() { return referenciaPublica; }
    public String getRequisicaoHash() { return requisicaoHash; }

    public Pedido(String nome, String telefone, String email, String endereco, String observacoes,
                  TipoAtendimento tipo, FormaPagamento pagamento, BigDecimal taxa,
                  String referencia, String idempotencia, String requisicao, String cotacao) {
        if (nome == null || nome.isBlank() || telefone == null || telefone.isBlank() || email == null || email.isBlank()
            || tipo == null || pagamento == null || taxa == null || taxa.signum() < 0
            || referencia == null || idempotencia == null || requisicao == null || cotacao == null)
            throw new IllegalArgumentException("Dados obrigatórios do pedido");
        if (tipo == TipoAtendimento.ENTREGA && (endereco == null || endereco.isBlank()))
            throw new IllegalArgumentException("Entrega exige endereço");
        if (tipo == TipoAtendimento.RETIRADA && taxa.signum() != 0)
            throw new IllegalArgumentException("Retirada não tem taxa");
        this.nomeCliente=nome.trim(); this.telefoneCliente=telefone; this.emailCliente=email;
        this.enderecoEntrega=tipo == TipoAtendimento.ENTREGA ? endereco : null;
        this.observacoes=observacoes; this.tipoAtendimento=tipo; this.formaPagamento=pagamento;
        this.taxaEntrega=taxa; this.subtotal=BigDecimal.ZERO.setScale(2); this.total=taxa;
        this.referenciaPublica=referencia; this.idempotenciaHash=idempotencia;
        this.requisicaoHash=requisicao; this.cotacaoHash=cotacao;
    }
    public void adicionarItem(Produto produto, int quantidade) {
        if (itens.stream().anyMatch(i -> i.getProduto().getId().equals(produto.getId())))
            throw new IllegalArgumentException("Produto duplicado");
        ItemPedido item = new ItemPedido(this, produto, quantidade);
        BigDecimal novoSubtotal=subtotal.add(item.getSubtotal());
        BigDecimal novoTotal=novoSubtotal.add(taxaEntrega);
        if (novoTotal.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new br.com.cafeaurora.exception.EntradaInvalidaException("Valor do pedido excede o limite");
        itens.add(item); subtotal=novoSubtotal; total=novoTotal;
    }
    protected Pedido() {}

    @PrePersist
    void antesDeSalvar() {
        if (itens.isEmpty()) throw new IllegalStateException("Pedido sem itens");
        if (criadoEm == null) criadoEm = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public String getNomeCliente() { return nomeCliente; }
    public String getTelefoneCliente() { return telefoneCliente; }
    public String getEnderecoEntrega() { return enderecoEntrega; }
    public String getObservacoes() { return observacoes; }
    public TipoAtendimento getTipoAtendimento() { return tipoAtendimento; }
    public StatusPedido getStatus() { return status; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public StatusPagamento getStatusPagamento() { return statusPagamento; }
    public BigDecimal getTotal() { return total; }
    public Instant getCriadoEm() { return criadoEm; }
    public List<ItemPedido> getItens() { return List.copyOf(itens); }
    public List<StatusPedido> proximosStatus() {
        return switch (status) {
            case RECEBIDO -> List.of(StatusPedido.EM_PREPARO, StatusPedido.CANCELADO);
            case EM_PREPARO -> List.of(StatusPedido.PRONTO, StatusPedido.CANCELADO);
            case PRONTO -> List.of(tipoAtendimento == TipoAtendimento.ENTREGA ? StatusPedido.EM_ENTREGA : StatusPedido.CONCLUIDO, StatusPedido.CANCELADO);
            case EM_ENTREGA -> tipoAtendimento == TipoAtendimento.ENTREGA ? List.of(StatusPedido.CONCLUIDO) : List.of();
            case CONCLUIDO, CANCELADO -> List.of();
        };
    }

    public void alterarStatus(StatusPedido novo) {
        if (novo == null || !proximosStatus().contains(novo))
            throw new br.com.cafeaurora.exception.RegraNegocioException(
                "Transição de status não permitida para este pedido");
        this.status = novo;
    }
}
