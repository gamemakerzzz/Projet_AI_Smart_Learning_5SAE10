package com.smartlearn.controller;

import com.smartlearn.dto.CategorieRequest;
import com.smartlearn.dto.CategorieResponse;
import com.smartlearn.service.CategorieService;
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
@RequestMapping("/api/categories")
public class CategorieController {

    private final CategorieService categorieService;

    public CategorieController(CategorieService categorieService) {
        this.categorieService = categorieService;
    }

    @GetMapping
    public List<CategorieResponse> findAll() {
        return categorieService.findAll();
    }

    @GetMapping("/{id}")
    public CategorieResponse findById(@PathVariable Long id) {
        return categorieService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategorieResponse create(@Valid @RequestBody CategorieRequest request) {
        return categorieService.create(request);
    }

    @PutMapping("/{id}")
    public CategorieResponse update(@PathVariable Long id, @Valid @RequestBody CategorieRequest request) {
        return categorieService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categorieService.delete(id);
    }
}
