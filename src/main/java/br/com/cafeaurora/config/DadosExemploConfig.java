package br.com.cafeaurora.config;
import br.com.cafeaurora.service.CargaCardapioDev;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
@Configuration @Profile("dev")
public class DadosExemploConfig {
    @Bean @Order(1)
    CommandLineRunner alimentarCatalogo(CargaCardapioDev carga) { return args -> carga.carregar(); }
}
