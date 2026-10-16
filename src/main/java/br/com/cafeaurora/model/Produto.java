package br.com.cafeaurora.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
public class Produto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "imagem_url", length = 500)
    private String imagemUrl;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private boolean disponivel = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    protected Produto() {}

    public Produto(String nome, String descricao, BigDecimal preco, String imagemUrl,
                   Categoria categoria, boolean ativo, boolean disponivel) {
        atualizar(nome, descricao, preco, imagemUrl, categoria, ativo, disponivel);
    }

    public void atualizar(String nome, String descricao, BigDecimal preco, String imagemUrl,
                          Categoria categoria, boolean ativo, boolean disponivel) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.imagemUrl = imagemUrl;
        this.categoria = categoria;
        this.ativo = ativo;
        this.disponivel = disponivel;
    }

    public void desativar() { this.ativo = false; }
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPreco() { return preco; }
    public String getImagemUrl() { return imagemUrl; }
    public boolean isAtivo() { return ativo; }
    public boolean isDisponivel() { return disponivel; }
    public Categoria getCategoria() { return categoria; }
}
