package br.com.cafeaurora.dto;
import br.com.cafeaurora.enums.StatusModeracao;
import java.time.Instant;
public record AvaliacaoAdmin(Long id,Long pedidoId,String nomeAutor,Integer nota,String comentario,
    Instant criadaEm,StatusModeracao statusModeracao,Instant moderadaEm,boolean temFoto) {}
