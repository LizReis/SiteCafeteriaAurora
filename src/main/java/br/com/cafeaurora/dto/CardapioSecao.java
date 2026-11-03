package br.com.cafeaurora.dto;
import java.util.List;
public record CardapioSecao(Long id, String nome, List<CardapioProduto> produtos) {}
