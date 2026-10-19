package com.smartlearn.dto;

import java.time.LocalDateTime;

public class ExamenRequest {

    private String titre;
    private String description;
    private LocalDateTime dateExamen;
    private Integer dureeMinutes;
    private Long formationId;
    private Integer coefficient = 1;

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateExamen() {
        return dateExamen;
    }

    public void setDateExamen(LocalDateTime dateExamen) {
        this.dateExamen = dateExamen;
    }

    public Integer getDureeMinutes() {
        return dureeMinutes;
    }

    public void setDureeMinutes(Integer dureeMinutes) {
        this.dureeMinutes = dureeMinutes;
    }

    public Long getFormationId() {
        return formationId;
    }

    public void setFormationId(Long formationId) {
        this.formationId = formationId;
    }

    public Integer getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(Integer coefficient) {
        this.coefficient = coefficient;
    }
}