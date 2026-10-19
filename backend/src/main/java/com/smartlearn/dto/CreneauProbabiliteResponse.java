package com.smartlearn.dto;

import java.util.List;

public class CreneauProbabiliteResponse {

    private Integer heure;
    private Integer jourSemaine;
    private Integer dureeMin;
    private Double probabilite;
    private String source;

    public Integer getHeure() { return heure; }
    public void setHeure(Integer heure) { this.heure = heure; }
    public Integer getJourSemaine() { return jourSemaine; }
    public void setJourSemaine(Integer jourSemaine) { this.jourSemaine = jourSemaine; }
    public Integer getDureeMin() { return dureeMin; }
    public void setDureeMin(Integer dureeMin) { this.dureeMin = dureeMin; }
    public Double getProbabilite() { return probabilite; }
    public void setProbabilite(Double probabilite) { this.probabilite = probabilite; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
