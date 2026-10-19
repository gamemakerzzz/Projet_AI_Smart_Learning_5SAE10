package com.smartlearn.controller;

import com.smartlearn.dto.CreneauProbabiliteRequest;
import com.smartlearn.dto.CreneauProbabiliteResponse;
import com.smartlearn.service.SeanceProbabiliteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seance")
public class SeanceProbabiliteController {

    private final SeanceProbabiliteService seanceProbabiliteService;

    public SeanceProbabiliteController(SeanceProbabiliteService seanceProbabiliteService) {
        this.seanceProbabiliteService = seanceProbabiliteService;
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @PostMapping("/probabilite/{apprenantId}")
    public ResponseEntity<List<CreneauProbabiliteResponse>> calculerProbabilite(
            @PathVariable("apprenantId") Long apprenantId,
            @RequestBody List<CreneauProbabiliteRequest> creneaux) {
        List<CreneauProbabiliteResponse> result = seanceProbabiliteService.calculerProbabilites(creneaux);
        return ResponseEntity.ok(result);
    }
}
