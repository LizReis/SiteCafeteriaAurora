package br.com.cafeaurora.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import br.com.cafeaurora.service.CardapioService;
import br.com.cafeaurora.enums.TipoCardapio;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaController {
    private final CardapioService cardapio;
    public PaginaController(CardapioService cardapio) { this.cardapio = cardapio; }


    @GetMapping({"/", "/index", "/index.html"})
    public String inicio() {
        return "index";
    }

    @GetMapping({"/cardapio", "/cardapio.html"})
    public String cardapio() {
        return "cardapio";
    }

    @GetMapping({"/cardapio-cafe", "/cardapio-cafe.html"})
    public String cardapioCafe(Model model) {
        model.addAttribute("secoes", cardapio.listarPorTipo(TipoCardapio.CAFE));
        return "cardapio-cafe";
    }

    @GetMapping({"/cardapio-comidas", "/cardapio-comidas.html"})
    public String cardapioComidas(Model model) {
        model.addAttribute("secoes", cardapio.listarPorTipo(TipoCardapio.COMIDAS));
        return "cardapio-comidas";
    }

    @GetMapping({"/cardapio-drinks", "/cardapio-drinks.html"})
    public String cardapioDrinks(Model model) {
        model.addAttribute("secoes", cardapio.listarPorTipo(TipoCardapio.DRINKS));
        return "cardapio-drinks";
    }

    @GetMapping({"/localizacao", "/localizacao.html"})
    public String localizacao() {
        return "localizacao";
    }

    @GetMapping({"/finalizar-pedido", "/finalizar-pedido.html"})
    public String finalizarPedido() {
        return "finalizar-pedido";
    }

    @GetMapping({"/pedido-confirmado", "/pedido-confirmado.html"})
    public String pedidoConfirmado() {
        return "pedido-confirmado";
    }

    @GetMapping({"/sacola-pedido", "/sacola-pedido.html"})
    public String sacolaPedido() {
        return "sacola-pedido";
    }
}
