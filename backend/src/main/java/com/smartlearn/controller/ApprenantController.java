package com.smartlearn.controller;

import com.smartlearn.dto.ApprenantRequest;
import com.smartlearn.dto.ApprenantResponse;
import com.smartlearn.dto.ApprenantUpdateRequest;
import com.smartlearn.dto.CompetenceRequest;
import com.smartlearn.dto.CompetenceResponse;
import com.smartlearn.dto.ProfilRequest;
import com.smartlearn.dto.ProfilResponse;
import com.smartlearn.service.ApprenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/apprenants")
public class ApprenantController {

    private final ApprenantService apprenantService;

    public ApprenantController(ApprenantService apprenantService) {
        this.apprenantService = apprenantService;
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<ApprenantResponse> findAll() {
        return apprenantService.findAll();
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #id)")
    @GetMapping("/{id}")
    public ApprenantResponse findById(@PathVariable Long id) {
        return apprenantService.findById(id);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApprenantResponse create(@Valid @RequestBody ApprenantRequest request) {
        return apprenantService.create(request);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApprenantResponse update(@PathVariable Long id, @Valid @RequestBody ApprenantUpdateRequest request) {
        return apprenantService.update(id, request);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        apprenantService.delete(id);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #id)")
    @GetMapping("/{id}/profil")
    public ProfilResponse getProfil(@PathVariable Long id) {
        return apprenantService.getProfil(id);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #id)")
    @PutMapping("/{id}/profil")
    public ProfilResponse upsertProfil(@PathVariable Long id, @Valid @RequestBody ProfilRequest request) {
        return apprenantService.upsertProfil(id, request);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #id)")
    @GetMapping("/{id}/competences")
    public List<CompetenceResponse> findCompetences(@PathVariable Long id) {
        return apprenantService.findCompetences(id);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #id)")
    @PostMapping("/{id}/competences")
    @ResponseStatus(HttpStatus.CREATED)
    public CompetenceResponse addCompetence(@PathVariable Long id, @Valid @RequestBody CompetenceRequest request) {
        return apprenantService.addCompetence(id, request);
    }
}
