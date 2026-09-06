package com.smartlearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ChapitreRequest {

    @NotBlank(message = "Le titre du chapitre est obligatoire")
    private String titre;

    private String contenu;

    @NotNull(message = "L'ordre du chapitre est obligatoire")
    private Integer ordre;

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Integer getOrdre() {
        return ordre;
    }

    public void setOrdre(Integer ordre) {
        this.ordre = ordre;
    }
}
