package com.smartlearn.controller;

import com.smartlearn.dto.ChapitreRequest;
import com.smartlearn.dto.ChapitreResponse;
import com.smartlearn.service.FormationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chapitres")
public class ChapitreController {

    private final FormationService formationService;

    public ChapitreController(FormationService formationService) {
        this.formationService = formationService;
    }

    @PutMapping("/{id}")
    public ChapitreResponse update(@PathVariable Long id, @Valid @RequestBody ChapitreRequest request) {
        return formationService.updateChapitre(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        formationService.deleteChapitre(id);
    }
}
