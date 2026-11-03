package br.com.cafeaurora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService adminUser(@Value("${cafe.admin.username:admin}") String usuario,
                                 @Value("${cafe.admin.password:}") String senha,
                                 PasswordEncoder encoder) {
        if (senha.isBlank()) {
            throw new IllegalStateException("Defina CAFE_ADMIN_PASSWORD para habilitar a administração");
        }
        return new InMemoryUserDetailsManager(User.withUsername(usuario)
                .password(encoder.encode(senha)).roles("ADMIN").build());
    }

    // A API antiga continua aceitando exclusivamente HTTP Basic, sem sessões/cookies.
    // Separar os filtros impede que um login via formulário autentique POSTs sem CSRF na API.
    @Bean
    @Order(1)
    SecurityFilterChain apiAdmin(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/admin/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .httpBasic(Customizer.withDefaults())
            .formLogin(form -> form.disable());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain paginas(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/login").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/h2-console/**").denyAll()
                .anyRequest().permitAll())
            // CSRF permanece ativo para todos os POSTs HTML e para o checkout público.
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin", true)
                .failureUrl("/admin/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout")
                .permitAll());
        return http.build();
    }
}
