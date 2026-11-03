package br.com.cafeaurora.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="cotacoes_pedido")
public class CotacaoPedido {
    @Id @Column(length=64) private String tokenHash;
    @Column(nullable=false,length=64) private String resumoHash;
    @Column(nullable=false) private Instant expiraEm;
    protected CotacaoPedido() {}
    public CotacaoPedido(String tokenHash, String resumoHash, Instant expiraEm) {
        this.tokenHash=tokenHash; this.resumoHash=resumoHash; this.expiraEm=expiraEm;
    }
    public String getResumoHash() { return resumoHash; }
    public Instant getExpiraEm() { return expiraEm; }
}
