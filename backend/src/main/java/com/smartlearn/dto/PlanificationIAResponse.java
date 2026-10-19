package com.smartlearn.dto;

import java.time.LocalDate;
import java.util.List;

public class PlanificationIAResponse {

    private Long apprenantId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer totalSeances;
    private String periode;
    private List<SeanceIAResponse> seances;

    public Long getApprenantId() { return apprenantId; }
    public void setApprenantId(Long apprenantId) { this.apprenantId = apprenantId; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public Integer getTotalSeances() { return totalSeances; }
    public void setTotalSeances(Integer totalSeances) { this.totalSeances = totalSeances; }
    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }
    public List<SeanceIAResponse> getSeances() { return seances; }
    public void setSeances(List<SeanceIAResponse> seances) { this.seances = seances; }

    public static class SeanceIAResponse {
        private String date;
        private String heureDebut;
        private String heureFin;
        private String titre;
        private String description;
        private Long formationId;
        private String formationTitre;
        private Integer priorite;
        private String motif;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getHeureDebut() { return heureDebut; }
        public void setHeureDebut(String heureDebut) { this.heureDebut = heureDebut; }
        public String getHeureFin() { return heureFin; }
        public void setHeureFin(String heureFin) { this.heureFin = heureFin; }
        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getFormationId() { return formationId; }
        public void setFormationId(Long formationId) { this.formationId = formationId; }
        public String getFormationTitre() { return formationTitre; }
        public void setFormationTitre(String formationTitre) { this.formationTitre = formationTitre; }
        public Integer getPriorite() { return priorite; }
        public void setPriorite(Integer priorite) { this.priorite = priorite; }
        public String getMotif() { return motif; }
        public void setMotif(String motif) { this.motif = motif; }
    }
}
