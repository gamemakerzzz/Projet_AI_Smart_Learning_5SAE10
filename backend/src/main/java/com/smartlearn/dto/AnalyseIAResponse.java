package com.smartlearn.dto;

import java.util.List;
import java.util.Map;

public class AnalyseIAResponse {

    private String styleApprentissage;
    private List<String> forces;
    private List<String> faiblesses;
    private List<String> recommandationsPersonnalisees;
    private Integer scoreProgression;
    private Map<String, Double> competencesEvolution;
    private String niveauGlobal;

    public AnalyseIAResponse() {}

    public String getStyleApprentissage() { return styleApprentissage; }
    public void setStyleApprentissage(String styleApprentissage) { this.styleApprentissage = styleApprentissage; }

    public List<String> getForces() { return forces; }
    public void setForces(List<String> forces) { this.forces = forces; }

    public List<String> getFaiblesses() { return faiblesses; }
    public void setFaiblesses(List<String> faiblesses) { this.faiblesses = faiblesses; }

    public List<String> getRecommandationsPersonnalisees() { return recommandationsPersonnalisees; }
    public void setRecommandationsPersonnalisees(List<String> recommandationsPersonnalisees) { this.recommandationsPersonnalisees = recommandationsPersonnalisees; }

    public Integer getScoreProgression() { return scoreProgression; }
    public void setScoreProgression(Integer scoreProgression) { this.scoreProgression = scoreProgression; }

    public Map<String, Double> getCompetencesEvolution() { return competencesEvolution; }
    public void setCompetencesEvolution(Map<String, Double> competencesEvolution) { this.competencesEvolution = competencesEvolution; }

    public String getNiveauGlobal() { return niveauGlobal; }
    public void setNiveauGlobal(String niveauGlobal) { this.niveauGlobal = niveauGlobal; }
}