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

    protected Pedido() {}

    @PrePersist
    void antesDeSalvar() {
        if (criadoEm == null) criadoEm = Instant.now();
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
    public List<ItemPedido> getItens() { return itens; }
}
