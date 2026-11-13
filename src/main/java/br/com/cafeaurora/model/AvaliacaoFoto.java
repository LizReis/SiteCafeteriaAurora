package br.com.cafeaurora.model;
import jakarta.persistence.*;
@Entity @Table(name="avaliacoes_fotos")
public class AvaliacaoFoto {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="avaliacao_id",nullable=false,unique=true) private Avaliacao avaliacao;
    @Column(nullable=false,length=30) private String tipoMime;
    @Lob @Column(nullable=false) private byte[] conteudo;
    protected AvaliacaoFoto() {}
    public AvaliacaoFoto(Avaliacao avaliacao,String tipoMime,byte[] conteudo) {
        this.avaliacao=avaliacao;this.tipoMime=tipoMime;this.conteudo=conteudo.clone();
    }
    public String getTipoMime(){return tipoMime;}
    public byte[] getConteudo(){return conteudo.clone();}
}
