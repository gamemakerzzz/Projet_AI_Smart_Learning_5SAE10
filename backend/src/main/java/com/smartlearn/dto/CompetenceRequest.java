package com.smartlearn.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CompetenceRequest {

    @NotBlank(message = "Le nom de la compétence est obligatoire")
    private String nom;

    @NotNull(message = "Le niveau de la compétence est obligatoire")
    @Min(value = 0, message = "Le niveau doit être compris entre 0 et 100")
    @Max(value = 100, message = "Le niveau doit être compris entre 0 et 100")
    private Integer niveau;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Integer getNiveau() {
        return niveau;
    }

    public void setNiveau(Integer niveau) {
        this.niveau = niveau;
    }
}
