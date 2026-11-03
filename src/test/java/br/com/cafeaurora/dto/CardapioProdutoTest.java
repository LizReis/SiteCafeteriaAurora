package br.com.cafeaurora.dto;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class CardapioProdutoTest {
    CardapioProduto produto(String imagem) {
        return new CardapioProduto(1L,"Nome","Descrição",new BigDecimal("1234.56"),imagem,true,2L,"Seção");
    }
    @Test void moedaExataEFormatacaoBrasileira() {
        assertEquals(123456L,produto(null).getPrecoCentavos());
        assertEquals("R$ 1.234,56",produto(null).getPrecoFormatado().replace('\u00a0',' '));
    }
    @Test void imagemAlternativaNaoRepresentaOutroProduto() {
        for(String imagem : new String[]{null,"", "javascript:alert(1)","//outro-host/img.jpg"})
            assertEquals("/img/produto-sem-imagem.svg",produto(imagem).getImagem());
        assertEquals("/img/1050.jpg",produto("/img/1050.jpg").getImagem());
        assertEquals("https://example.com/cafe.jpg",produto("https://example.com/cafe.jpg").getImagem());
    }
}
