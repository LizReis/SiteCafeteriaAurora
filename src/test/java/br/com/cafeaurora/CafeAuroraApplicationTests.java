package br.com.cafeaurora;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:contexto_test;DB_CLOSE_DELAY=-1")
class CafeAuroraApplicationTests {

    @Test
    void contextLoads() {
    }
}
