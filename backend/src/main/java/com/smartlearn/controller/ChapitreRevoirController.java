package com.smartlearn.controller;

import com.smartlearn.dto.ChapitreRevoirResponse;
import com.smartlearn.service.ChapitreRevoirService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chapitres-revoir")
public class ChapitreRevoirController {

    private final ChapitreRevoirService chapitreRevoirService;

    public ChapitreRevoirController(ChapitreRevoirService chapitreRevoirService) {
        this.chapitreRevoirService = chapitreRevoirService;
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}")
    public ResponseEntity<List<ChapitreRevoirResponse>> detectChapitresARevoir(
            @PathVariable("apprenantId") Long apprenantId) {
        return ResponseEntity.ok(chapitreRevoirService.detectChapitresARevoir(apprenantId));
    }
}