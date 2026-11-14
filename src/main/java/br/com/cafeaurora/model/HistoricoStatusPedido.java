package br.com.cafeaurora.model;

import br.com.cafeaurora.enums.*;
import jakarta.persistence.*;
import java.time.Instant;

/** Evento imutável: sem setters, edição ou exclusão na aplicação. */
@Entity
@Table(name="historico_status_pedido", indexes=@Index(name="idx_historico_pedido", columnList="pedido_id,alterado_em,id"))
public class HistoricoStatusPedido {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="pedido_id",nullable=false,updatable=false)
    private Pedido pedido;
    @Enumerated(EnumType.STRING) @Column(length=25,updatable=false)
    private StatusPedido statusAnterior;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=25,updatable=false)
    private StatusPedido statusNovo;
    @Column(name="alterado_em",nullable=false,updatable=false)
    private Instant alteradoEm;
    @Column(nullable=false,length=120,updatable=false)
    private String responsavel;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=25,updatable=false)
    private OrigemStatusPedido origem;
    protected HistoricoStatusPedido() {}
    public HistoricoStatusPedido(Pedido pedido, StatusPedido anterior, StatusPedido novo,
                                 Instant quando, String responsavel, OrigemStatusPedido origem) {
        this.pedido=java.util.Objects.requireNonNull(pedido);
        this.statusAnterior=anterior; this.statusNovo=java.util.Objects.requireNonNull(novo);
        this.alteradoEm=java.util.Objects.requireNonNull(quando);
        if(responsavel==null || responsavel.isBlank() || responsavel.length()>120)
            throw new IllegalArgumentException("Responsável inválido");
        this.responsavel=responsavel; this.origem=java.util.Objects.requireNonNull(origem);
    }
    public Long getId(){return id;}
    public StatusPedido getStatusAnterior(){return statusAnterior;}
    public StatusPedido getStatusNovo(){return statusNovo;}
    public Instant getAlteradoEm(){return alteradoEm;}
    public String getResponsavel(){return responsavel;}
    public OrigemStatusPedido getOrigem(){return origem;}
}
