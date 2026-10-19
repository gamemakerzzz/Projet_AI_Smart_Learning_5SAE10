package com.smartlearn.controller;

import com.smartlearn.dto.PlanificationIARequest;
import com.smartlearn.dto.PlanificationIAResponse;
import com.smartlearn.service.PlanificationIAService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/planification-ia")
public class PlanificationIAController {

    private final PlanificationIAService planificationIAService;

    public PlanificationIAController(PlanificationIAService planificationIAService) {
        this.planificationIAService = planificationIAService;
    }

    @PostMapping("/generer")
    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #req.apprenantId)")
    public ResponseEntity<PlanificationIAResponse> generer(@P("req") @RequestBody PlanificationIARequest req) {
        if (req.getApprenantId() == null || req.getDateDebut() == null || req.getDateFin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "apprenantId, dateDebut et dateFin sont obligatoires");
        }
        if (req.getDateFin().isBefore(req.getDateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dateFin doit être après dateDebut");
        }
        List<Long> formationIds = req.getFormationIds();   // null = toutes les formations, comme avant
        PlanificationIAResponse response = planificationIAService.genererPlanification(
                req.getApprenantId(), req.getDateDebut(), req.getDateFin(), formationIds);
        return ResponseEntity.ok(response);
    }
}