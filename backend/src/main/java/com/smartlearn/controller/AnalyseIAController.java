package com.smartlearn.controller;

import com.smartlearn.dto.AnalyseIAResponse;
import com.smartlearn.service.AnalyseIAService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyse-ia")
public class AnalyseIAController {

    private final AnalyseIAService analyseIAService;

    public AnalyseIAController(AnalyseIAService analyseIAService) {
        this.analyseIAService = analyseIAService;
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}")
    public ResponseEntity<AnalyseIAResponse> analyserApprenant(
            @PathVariable("apprenantId") Long apprenantId) {
        return ResponseEntity.ok(analyseIAService.analyserApprenant(apprenantId));
    }
}