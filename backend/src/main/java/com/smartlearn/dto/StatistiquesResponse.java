package com.smartlearn.dto;

import java.util.List;
import java.util.Map;

public class StatistiquesResponse {

    private Long apprenantId;
    private String apprenantNom;
    private Integer totalExamens;
    private Integer totalSeances;
    private Integer totalNotifications;
    private Integer totalChapitres;
    private Integer chapitresARevoir;
    private Map<String, Integer> competencesByLevel;
    private List<FormationProgress> formationProgress;
    private Double averageCompetenceLevel;

    public StatistiquesResponse() {}

    public Long getApprenantId() { return apprenantId; }
    public void setApprenantId(Long apprenantId) { this.apprenantId = apprenantId; }

    public String getApprenantNom() { return apprenantNom; }
    public void setApprenantNom(String apprenantNom) { this.apprenantNom = apprenantNom; }

    public Integer getTotalExamens() { return totalExamens; }
    public void setTotalExamens(Integer totalExamens) { this.totalExamens = totalExamens; }

    public Integer getTotalSeances() { return totalSeances; }
    public void setTotalSeances(Integer totalSeances) { this.totalSeances = totalSeances; }

    public Integer getTotalNotifications() { return totalNotifications; }
    public void setTotalNotifications(Integer totalNotifications) { this.totalNotifications = totalNotifications; }

    public Integer getTotalChapitres() { return totalChapitres; }
    public void setTotalChapitres(Integer totalChapitres) { this.totalChapitres = totalChapitres; }

    public Integer getChapitresARevoir() { return chapitresARevoir; }
    public void setChapitresARevoir(Integer chapitresARevoir) { this.chapitresARevoir = chapitresARevoir; }

    public Map<String, Integer> getCompetencesByLevel() { return competencesByLevel; }
    public void setCompetencesByLevel(Map<String, Integer> competencesByLevel) { this.competencesByLevel = competencesByLevel; }

    public List<FormationProgress> getFormationProgress() { return formationProgress; }
    public void setFormationProgress(List<FormationProgress> formationProgress) { this.formationProgress = formationProgress; }

    public Double getAverageCompetenceLevel() { return averageCompetenceLevel; }
    public void setAverageCompetenceLevel(Double averageCompetenceLevel) { this.averageCompetenceLevel = averageCompetenceLevel; }

    public static class FormationProgress {
        private Long formationId;
        private String titre;
        private Integer totalChapitres;
        private Integer chapitresFaibles;

        public FormationProgress() {}

        public FormationProgress(Long formationId, String titre, Integer totalChapitres, Integer chapitresFaibles) {
            this.formationId = formationId;
            this.titre = titre;
            this.totalChapitres = totalChapitres;
            this.chapitresFaibles = chapitresFaibles;
        }

        public Long getFormationId() { return formationId; }
        public void setFormationId(Long formationId) { this.formationId = formationId; }

        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }

        public Integer getTotalChapitres() { return totalChapitres; }
        public void setTotalChapitres(Integer totalChapitres) { this.totalChapitres = totalChapitres; }

        public Integer getChapitresFaibles() { return chapitresFaibles; }
        public void setChapitresFaibles(Integer chapitresFaibles) { this.chapitresFaibles = chapitresFaibles; }
    }
}