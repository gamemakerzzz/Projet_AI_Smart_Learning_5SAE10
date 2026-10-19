package com.smartlearn.dto;

public class ProfilRequest {

    private String niveau;
    private String objectif;
    private String domaineInteret;

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
}
