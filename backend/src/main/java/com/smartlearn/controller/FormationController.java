package com.smartlearn.controller;

import com.smartlearn.dto.ChapitreRequest;
import com.smartlearn.dto.ChapitreResponse;
import com.smartlearn.dto.FormationRequest;
import com.smartlearn.dto.FormationResponse;
import com.smartlearn.service.FormationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/formations")
public class FormationController {

    private final FormationService formationService;

    public FormationController(FormationService formationService) {
        this.formationService = formationService;
    }

    @GetMapping
    public List<FormationResponse> findAll(@RequestParam(required = false) String niveau) {
        return formationService.findAll(niveau);
    }

    @GetMapping("/search")
    public List<FormationResponse> search(@RequestParam String keyword) {
        return formationService.searchByTitre(keyword);
    }

    @GetMapping("/categorie/{categorieId}")
    public List<FormationResponse> findByCategorie(@PathVariable Long categorieId) {
        return formationService.findByCategorie(categorieId);
    }

    @GetMapping("/{id}")
    public FormationResponse findById(@PathVariable Long id) {
        return formationService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FormationResponse create(@Valid @RequestBody FormationRequest request) {
        return formationService.create(request);
    }

    @PutMapping("/{id}")
    public FormationResponse update(@PathVariable Long id, @Valid @RequestBody FormationRequest request) {
        return formationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        formationService.delete(id);
    }

    @GetMapping("/{id}/chapitres")
    public List<ChapitreResponse> findChapitres(@PathVariable Long id) {
        return formationService.findChapitres(id);
    }

    @PostMapping("/{id}/chapitres")
    @ResponseStatus(HttpStatus.CREATED)
    public ChapitreResponse addChapitre(@PathVariable Long id, @Valid @RequestBody ChapitreRequest request) {
        return formationService.addChapitre(id, request);
    }
}
