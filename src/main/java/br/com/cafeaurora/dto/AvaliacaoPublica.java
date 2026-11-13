package br.com.cafeaurora.dto;
import java.time.Instant;
public record AvaliacaoPublica(Long id,String nomeAutor,Integer nota,String comentario,Instant criadaEm,String imagem) {}
