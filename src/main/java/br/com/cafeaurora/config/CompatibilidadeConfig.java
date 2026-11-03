package br.com.cafeaurora.config;
import br.com.cafeaurora.service.CompatibilidadeCategorias;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
@Configuration
public class CompatibilidadeConfig {
    @Bean @Order(0)
    CommandLineRunner migrarCategoriasLegadas(CompatibilidadeCategorias service) {
        return args -> service.atualizarLegadas();
    }
}
