package com.smartlearn.controller;

import com.smartlearn.repository.ApprenantRepository;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final ApprenantRepository users;
    public AuthController(ApprenantRepository users) { this.users = users; }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        var user = users.findByEmailIgnoreCase(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return Map.of("id", user.getId(), "email", user.getEmail(),
            "prenom", user.getPrenom(), "nom", user.getNom(), "role", user.getRole().name());
    }
}
