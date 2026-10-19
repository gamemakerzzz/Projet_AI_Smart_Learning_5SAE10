package com.smartlearn.dto;

import java.time.LocalDateTime;

public class PlanificationResponse {

    private Long id;
    private String titre;
    private LocalDateTime dateHeureDebut;
    private LocalDateTime dateHeureFin;
    private String statut;
    private Long formationId;
    private String formationTitre;

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

    public LocalDateTime getDateHeureDebut() {
        return dateHeureDebut;
    }

    public void setDateHeureDebut(LocalDateTime dateHeureDebut) {
        this.dateHeureDebut = dateHeureDebut;
    }

    public LocalDateTime getDateHeureFin() {
        return dateHeureFin;
    }

    public void setDateHeureFin(LocalDateTime dateHeureFin) {
        this.dateHeureFin = dateHeureFin;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Long getFormationId() {
        return formationId;
    }

    public void setFormationId(Long formationId) {
        this.formationId = formationId;
    }

    public String getFormationTitre() {
        return formationTitre;
    }

    public void setFormationTitre(String formationTitre) {
        this.formationTitre = formationTitre;
    }
}