package com.smartlearn.dto;

import java.time.LocalDate;
import java.util.List;

public class PlanificationIARequest {

    private Long apprenantId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private List<Long> formationIds;

    public Long getApprenantId() { return apprenantId; }
    public void setApprenantId(Long apprenantId) { this.apprenantId = apprenantId; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public List<Long> getFormationIds() { return formationIds; }
    public void setFormationIds(List<Long> formationIds) { this.formationIds = formationIds; }
}
