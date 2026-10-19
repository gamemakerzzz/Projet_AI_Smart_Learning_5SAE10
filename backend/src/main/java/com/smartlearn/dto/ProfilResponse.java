package com.smartlearn.dto;

public class ProfilResponse {

    private Long id;
    private String niveau;
    private String objectif;
    private String domaineInteret;
    private Long apprenantId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getObjectif() {
        return objectif;
    }

    public void setObjectif(String objectif) {
        this.objectif = objectif;
    }

    public String getDomaineInteret() {
        return domaineInteret;
    }

    public void setDomaineInteret(String domaineInteret) {
        this.domaineInteret = domaineInteret;
    }

    public Long getApprenantId() {
        return apprenantId;
    }

    public void setApprenantId(Long apprenantId) {
        this.apprenantId = apprenantId;
    }
}
