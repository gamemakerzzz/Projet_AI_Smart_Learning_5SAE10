package com.smartlearn.controller;

import com.smartlearn.dto.SeanceRequest;
import com.smartlearn.dto.SeanceResponse;
import com.smartlearn.service.SeanceService;
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
@RequestMapping("/api/seances")
public class SeanceController {

    private final SeanceService seanceService;

    public SeanceController(SeanceService seanceService) {
        this.seanceService = seanceService;
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}")
    public List<SeanceResponse> getAllByApprenant(
            @PathVariable("apprenantId") Long apprenantId) {
        return seanceService.getAllByApprenant(apprenantId);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}/formation/{formationId}")
    public List<SeanceResponse> getAllByApprenantAndFormation(
            @PathVariable("apprenantId") Long apprenantId,
            @PathVariable("formationId") Long formationId) {
        return seanceService.getAllByApprenantAndFormation(apprenantId, formationId);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @PostMapping("/apprenant/{apprenantId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SeanceResponse create(
            @PathVariable("apprenantId") Long apprenantId,
            @Valid @RequestBody SeanceRequest request) {
        return seanceService.create(apprenantId, request);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.seance(authentication, #id)")
    @PutMapping("/{id}")
    public SeanceResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody SeanceRequest request) {
        return seanceService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.seance(authentication, #id)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        seanceService.delete(id);
    }
}