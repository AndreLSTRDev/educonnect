package br.ucsal.educonnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/login", "/recuperar-senha", "/redefinir-senha", "/css/**", "/js/**", "/img/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/professor/**").hasRole("PROFESSOR")
                .requestMatchers("/aluno/**").hasRole("ALUNO")
                .requestMatchers("/gestor/**").hasRole("GESTOR_DENUNCIAS")
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login")
                .defaultSuccessUrl("/painel", true)
                .failureUrl("/login?erro")
                .permitAll())
            .logout(l -> l
                .logoutSuccessUrl("/login?saiu")
                .invalidateHttpSession(true)
                .permitAll());
        return http.build();
    }
}
