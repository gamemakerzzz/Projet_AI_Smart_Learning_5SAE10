package com.smartlearn.dto;

import java.math.BigDecimal;

public class FormationResponse {

    private Long id;
    private String titre;
    private String description;
    private BigDecimal prix;
    private String niveau;
    private String motsCles;
    private CategorieResponse categorie;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getMotsCles() {
        return motsCles;
    }

    public void setMotsCles(String motsCles) {
        this.motsCles = motsCles;
    }

    public CategorieResponse getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieResponse categorie) {
        this.categorie = categorie;
    }
}
