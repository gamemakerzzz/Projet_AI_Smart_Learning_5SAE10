package com.smartlearn.dto;

public class ChapitreRevoirResponse {

    private Long chapitreId;
    private String titre;
    private Long formationId;
    private String formationTitre;
    private Integer ordre;
    private String raison;
    private Integer priorite;

    public ChapitreRevoirResponse() {}

    public ChapitreRevoirResponse(Long chapitreId, String titre, Long formationId,
                                   String formationTitre, Integer ordre, String raison, Integer priorite) {
        this.chapitreId = chapitreId;
        this.titre = titre;
        this.formationId = formationId;
        this.formationTitre = formationTitre;
        this.ordre = ordre;
        this.raison = raison;
        this.priorite = priorite;
    }

    public Long getChapitreId() { return chapitreId; }
    public void setChapitreId(Long chapitreId) { this.chapitreId = chapitreId; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Long getFormationId() { return formationId; }
    public void setFormationId(Long formationId) { this.formationId = formationId; }

    public String getFormationTitre() { return formationTitre; }
    public void setFormationTitre(String formationTitre) { this.formationTitre = formationTitre; }

    public Integer getOrdre() { return ordre; }
    public void setOrdre(Integer ordre) { this.ordre = ordre; }

    public String getRaison() { return raison; }
    public void setRaison(String raison) { this.raison = raison; }

    public Integer getPriorite() { return priorite; }
    public void setPriorite(Integer priorite) { this.priorite = priorite; }
}