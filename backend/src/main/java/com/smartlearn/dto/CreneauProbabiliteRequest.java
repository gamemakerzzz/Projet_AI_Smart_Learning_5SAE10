package com.smartlearn.dto;

import java.util.List;

public class CreneauProbabiliteRequest {

    private Integer heure;
    private Integer jourSemaine;
    private Integer dureeMin;
    private Integer joursAvantExamen;
    private Integer seancesMemeJour;

    public Integer getHeure() { return heure; }
    public void setHeure(Integer heure) { this.heure = heure; }
    public Integer getJourSemaine() { return jourSemaine; }
    public void setJourSemaine(Integer jourSemaine) { this.jourSemaine = jourSemaine; }
    public Integer getDureeMin() { return dureeMin; }
    public void setDureeMin(Integer dureeMin) { this.dureeMin = dureeMin; }
    public Integer getJoursAvantExamen() { return joursAvantExamen; }
    public void setJoursAvantExamen(Integer joursAvantExamen) { this.joursAvantExamen = joursAvantExamen; }
    public Integer getSeancesMemeJour() { return seancesMemeJour; }
    public void setSeancesMemeJour(Integer seancesMemeJour) { this.seancesMemeJour = seancesMemeJour; }
}
