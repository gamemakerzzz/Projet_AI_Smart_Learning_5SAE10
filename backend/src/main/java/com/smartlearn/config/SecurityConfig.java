package com.smartlearn.config;

import com.smartlearn.repository.ApprenantRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
        http.cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/api/auth/csrf", "/api/auth/login", "/error").permitAll()
                .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()

                // Lecture du catalogue
                .requestMatchers(HttpMethod.GET, "/api/formations/**", "/api/categories/**").authenticated()

                // Gestion des apprenants : ADMIN uniquement
                .requestMatchers(HttpMethod.GET, "/api/apprenants").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/apprenants").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/apprenants/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/apprenants/*").hasRole("ADMIN")

                // Liste de TOUS les examens : ADMIN uniquement (avant la règle générale)
                .requestMatchers(HttpMethod.GET, "/api/examens").hasRole("ADMIN")

                // Modules accessibles à USER et ADMIN (l'ownership est vérifié dans les contrôleurs)
                .requestMatchers(
                    "/api/apprenants/**", "/api/competences/**", "/api/recommendations/**",
                    "/api/examens/**", "/api/seances/**", "/api/notifications/**",
                    "/api/planification/**", "/api/chapitres-revoir/**", "/api/statistiques/**",
                    "/api/analyse-ia/**", "/api/planification-ia/**", "/api/seance/**"
                ).authenticated()

                // Tout le reste de /api : ADMIN
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