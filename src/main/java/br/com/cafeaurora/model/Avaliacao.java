package br.com.cafeaurora.model;

import br.com.cafeaurora.enums.StatusModeracao;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="avaliacoes", uniqueConstraints=@UniqueConstraint(name="uk_avaliacao_pedido",columnNames="pedido_id"))
public class Avaliacao {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="pedido_id",nullable=false) private Pedido pedido;
    @Column(nullable=false,length=100) private String nomeAutor;
    @Column(nullable=false) private Integer nota;
    @Column(nullable=false,length=1000) private String comentario;
    @Column(nullable=false,updatable=false) private Instant criadaEm;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20)
    private StatusModeracao statusModeracao=StatusModeracao.PENDENTE;
    private Instant moderadaEm;
    @Column(nullable=false) private boolean consentimentoPublicacao;
    protected Avaliacao() {}
    public Avaliacao(Pedido pedido,String nome,Integer nota,String comentario) {
        this.pedido=pedido;this.nomeAutor=nome;this.nota=nota;this.comentario=comentario;
        this.consentimentoPublicacao=true;this.criadaEm=Instant.now();
    }
    public void moderar(StatusModeracao status) {
        if(status==null||status==StatusModeracao.PENDENTE)
            throw new br.com.cafeaurora.exception.EntradaInvalidaException("Escolha aprovar ou rejeitar");
        statusModeracao=status;moderadaEm=Instant.now();
    }
    public Long getId(){return id;}
    public Pedido getPedido(){return pedido;}
    public String getNomeAutor(){return nomeAutor;}
    public Integer getNota(){return nota;}
    public String getComentario(){return comentario;}
    public Instant getCriadaEm(){return criadaEm;}
    public StatusModeracao getStatusModeracao(){return statusModeracao;}
    public Instant getModeradaEm(){return moderadaEm;}
    public boolean isConsentimentoPublicacao(){return consentimentoPublicacao;}
}
