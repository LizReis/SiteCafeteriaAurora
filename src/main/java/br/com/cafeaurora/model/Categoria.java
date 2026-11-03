package br.com.cafeaurora.model;

import jakarta.persistence.*;
import br.com.cafeaurora.enums.TipoCardapio;

@Entity
@Table(name = "categorias", uniqueConstraints = @UniqueConstraint(columnNames = "nome"))
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false)
    private boolean ativa = true;

    // Nullable para atualização segura dos bancos anteriores. Categorias não
    // reconhecidas permanecem sem classificação até decisão administrativa.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoCardapio tipoCardapio;
    private Integer ordemExibicao;

    public TipoCardapio getTipoCardapio() { return tipoCardapio; }
    public Integer getOrdemExibicao() { return ordemExibicao; }
    public void classificar(TipoCardapio tipo, Integer ordem) {
        this.tipoCardapio = tipo;
        this.ordemExibicao = ordem;
    }
    public Categoria(String nome, String descricao, boolean ativa, TipoCardapio tipo, Integer ordem) {
        this(nome, descricao, ativa);
        classificar(tipo, ordem);
    }
    protected Categoria() {}

    public Categoria(String nome, String descricao, boolean ativa) {
        this.nome = nome;
        this.descricao = descricao;
        this.ativa = ativa;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public boolean isAtiva() { return ativa; }

    public void atualizar(String nome, String descricao, boolean ativa) {
        this.nome = nome;
        this.descricao = descricao;
        this.ativa = ativa;
    }

    public void desativar() { this.ativa = false; }
}
