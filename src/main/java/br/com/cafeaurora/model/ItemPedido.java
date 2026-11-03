package br.com.cafeaurora.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "itens_pedido")
public class ItemPedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "nome_produto", nullable = false, length = 120)
    private String nomeProduto;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    ItemPedido(Pedido pedido, Produto produto, int quantidade) {
        if (pedido == null || produto == null || quantidade < 1 || quantidade > 99
            || !produto.isAtivo() || !produto.isDisponivel() || !produto.getCategoria().isAtiva()
            || produto.getPreco() == null || produto.getPreco().signum() <= 0)
            throw new IllegalArgumentException("Item inválido");
        this.pedido=pedido; this.produto=produto; this.quantidade=quantidade;
        this.nomeProduto=produto.getNome(); this.precoUnitario=produto.getPreco();
        this.imagemUrl=produto.getImagemUrl();
    }
    @Column(length=500)
    private String imagemUrl;
    public String getImagemUrl() { return imagemUrl; }
    protected ItemPedido() {}

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public Produto getProduto() { return produto; }
    public String getNomeProduto() { return nomeProduto; }
    public Integer getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
