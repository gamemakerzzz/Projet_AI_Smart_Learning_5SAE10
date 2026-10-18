package com.smartlearn.config;

import com.smartlearn.repository.ApprenantRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    UserDetailsService users(ApprenantRepository repository) {
        return email -> repository.findByEmailIgnoreCase(email.trim())
                .map(a -> User.withUsername(a.getEmail()).password(a.getPassword())
                        .roles(a.getRole().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf", "/api/auth/login", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/flashcards/openai/callback").permitAll()
                .requestMatchers("/api/flashcards/**").authenticated()
                .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/formations/**", "/api/categories/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/apprenants").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/apprenants").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/apprenants/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/apprenants/*").hasRole("ADMIN")
                .requestMatchers("/api/apprenants/**", "/api/competences/**", "/api/recommendations/**").authenticated()
                .requestMatchers("/api/**").hasRole("ADMIN")
                .anyRequest().denyAll())
            .formLogin(form -> form.loginProcessingUrl("/api/auth/login").usernameParameter("email")
                .successHandler((req, res, auth) -> res.setStatus(204))
                .failureHandler((req, res, ex) -> res.setStatus(401)))
            .logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, err) -> res.setStatus(401))
                .accessDeniedHandler((req, res, err) -> res.setStatus(403)))
            .requestCache(cache -> cache.disable());
        return http.build();
    }
}
