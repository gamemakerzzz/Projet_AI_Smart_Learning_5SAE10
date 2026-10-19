package com.smartlearn.controller;

import com.smartlearn.dto.StatistiquesResponse;
import com.smartlearn.service.StatistiquesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistiques")
public class StatistiquesController {

    private final StatistiquesService statistiquesService;

    public StatistiquesController(StatistiquesService statistiquesService) {
        this.statistiquesService = statistiquesService;
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}")
    public ResponseEntity<StatistiquesResponse> getStatistiques(
            @PathVariable("apprenantId") Long apprenantId) {
        return ResponseEntity.ok(statistiquesService.getStatistiques(apprenantId));
    }
}