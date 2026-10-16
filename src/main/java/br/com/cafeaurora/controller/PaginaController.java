package br.com.cafeaurora.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaController {

    @GetMapping({"/", "/index", "/index.html"})
    public String inicio() {
        return "index";
    }

    @GetMapping({"/cardapio", "/cardapio.html"})
    public String cardapio() {
        return "cardapio";
    }

    @GetMapping({"/cardapio-cafe", "/cardapio-cafe.html"})
    public String cardapioCafe() {
        return "cardapio-cafe";
    }

    @GetMapping({"/cardapio-comidas", "/cardapio-comidas.html"})
    public String cardapioComidas() {
        return "cardapio-comidas";
    }

    @GetMapping({"/cardapio-drinks", "/cardapio-drinks.html"})
    public String cardapioDrinks() {
        return "cardapio-drinks";
    }

    @GetMapping({"/sacola-pedido", "/sacola-pedido.html"})
    public String sacolaPedido() {
        return "sacola-pedido";
    }
}
