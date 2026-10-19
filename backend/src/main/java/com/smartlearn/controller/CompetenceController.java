package com.smartlearn.controller;

import com.smartlearn.dto.CompetenceRequest;
import com.smartlearn.dto.CompetenceResponse;
import com.smartlearn.service.ApprenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @ownership.competence(authentication, #id)")
@RestController
@RequestMapping("/api/competences")
public class CompetenceController {

    private final ApprenantService apprenantService;

    public CompetenceController(ApprenantService apprenantService) {
        this.apprenantService = apprenantService;
    }

    @PutMapping("/{id}")
    public CompetenceResponse update(@PathVariable Long id, @Valid @RequestBody CompetenceRequest request) {
        return apprenantService.updateCompetence(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        apprenantService.deleteCompetence(id);
    }
}
