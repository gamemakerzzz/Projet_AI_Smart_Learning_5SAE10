package com.smartlearn.dto;

import java.math.BigDecimal;

public class RecommendationResponse {

    private Long formationId;
    private String titre;
    private Double score;
    private String description;
    private String niveau;
    private String categorieNom;
    private BigDecimal prix;

    public RecommendationResponse() {
    }

    public RecommendationResponse(Long formationId, String titre, Double score, String description, String niveau, String categorieNom, BigDecimal prix) {
        this.formationId = formationId;
        this.titre = titre;
        this.score = score;
        this.description = description;
        this.niveau = niveau;
        this.categorieNom = categorieNom;
        this.prix = prix;
    }

    public Long getFormationId() {
        return formationId;
    }

    public void setFormationId(Long formationId) {
        this.formationId = formationId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getCategorieNom() {
        return categorieNom;
    }

    public void setCategorieNom(String categorieNom) {
        this.categorieNom = categorieNom;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }
}
