package com.smartlearn.controller;

import com.smartlearn.dto.PlanificationRequest;
import com.smartlearn.dto.PlanificationResponse;
import com.smartlearn.service.PlanificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/planification")
public class PlanificationController {

    private final PlanificationService planificationService;

    public PlanificationController(PlanificationService planificationService) {
        this.planificationService = planificationService;
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #request.apprenantId)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<PlanificationResponse> generate(@Valid @RequestBody PlanificationRequest request) {
        return planificationService.generate(request);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}")
    public List<PlanificationResponse> getAllByApprenant(
            @PathVariable("apprenantId") Long apprenantId) {
        return planificationService.getAllByApprenant(apprenantId);
    }
}