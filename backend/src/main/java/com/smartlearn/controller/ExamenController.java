
package com.smartlearn.controller;

import com.smartlearn.dto.ExamenRequest;
import com.smartlearn.dto.ExamenResponse;
import com.smartlearn.service.ExamenService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/examens")
public class ExamenController {

    private final ExamenService examenService;

    public ExamenController(ExamenService examenService) {
        this.examenService = examenService;
    }

    // Récupérer tous les examens d'un apprenant
    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}")
    public List<ExamenResponse> getAllByApprenant(
            @PathVariable("apprenantId") Long apprenantId) {

        return examenService.getAllByApprenant(apprenantId);
    }

    // Récupérer les examens d'un apprenant par formation
    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}/formation/{formationId}")
    public List<ExamenResponse> getAllByApprenantAndFormation(
            @PathVariable("apprenantId") Long apprenantId,
            @PathVariable("formationId") Long formationId) {

        return examenService.getAllByApprenantAndFormation(
                apprenantId, formationId);
    }

    // Récupérer les examens à venir
    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}/a-venir")
    public List<ExamenResponse> getAllByApprenantAndUpcoming(
            @PathVariable("apprenantId") Long apprenantId) {

        return examenService.getAllByApprenantAndUpcoming(apprenantId);
    }

    // Créer un examen
    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @PostMapping("/apprenant/{apprenantId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamenResponse create(
            @PathVariable("apprenantId") Long apprenantId,
            @Valid @RequestBody ExamenRequest request) {

        return examenService.create(apprenantId, request);
    }

    // Modifier un examen
    @PreAuthorize("hasRole('ADMIN') or @ownership.examen(authentication, #id)")
    @PutMapping("/{id}")
    public ExamenResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody ExamenRequest request) {

        return examenService.update(id, request);
    }

    // Supprimer un examen
    @PreAuthorize("hasRole('ADMIN') or @ownership.examen(authentication, #id)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        examenService.delete(id);
    }
}
