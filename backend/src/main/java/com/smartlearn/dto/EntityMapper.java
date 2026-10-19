package com.smartlearn.dto;

import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Categorie;
import com.smartlearn.entity.Chapitre;
import com.smartlearn.entity.Competence;
import com.smartlearn.entity.Formation;
import com.smartlearn.entity.Profil;

public final class EntityMapper {

    private EntityMapper() {
    }

    public static CategorieResponse toCategorieResponse(Categorie categorie) {
        return new CategorieResponse(categorie.getId(), categorie.getNom(), categorie.getDescription());
    }

    public static FormationResponse toFormationResponse(Formation formation) {
        FormationResponse response = new FormationResponse();
        response.setId(formation.getId());
        response.setTitre(formation.getTitre());
        response.setDescription(formation.getDescription());
        response.setPrix(formation.getPrix());
        response.setNiveau(formation.getNiveau());
        response.setMotsCles(formation.getMotsCles());
        if (formation.getCategorie() != null) {
            response.setCategorie(toCategorieResponse(formation.getCategorie()));
        }
        return response;
    }

    public static ChapitreResponse toChapitreResponse(Chapitre chapitre) {
        ChapitreResponse response = new ChapitreResponse();
        response.setId(chapitre.getId());
        response.setTitre(chapitre.getTitre());
        response.setContenu(chapitre.getContenu());
        response.setOrdre(chapitre.getOrdre());
        if (chapitre.getFormation() != null) {
            response.setFormationId(chapitre.getFormation().getId());
        }
        return response;
    }

    public static ProfilResponse toProfilResponse(Profil profil) {
        if (profil == null) {
            return null;
        }
        ProfilResponse response = new ProfilResponse();
        response.setId(profil.getId());
        response.setNiveau(profil.getNiveau());
        response.setObjectif(profil.getObjectif());
        response.setDomaineInteret(profil.getDomaineInteret());
        if (profil.getApprenant() != null) {
            response.setApprenantId(profil.getApprenant().getId());
        }
        return response;
    }

    public static CompetenceResponse toCompetenceResponse(Competence competence) {
        CompetenceResponse response = new CompetenceResponse();
        response.setId(competence.getId());
        response.setNom(competence.getNom());
        response.setNiveau(competence.getNiveau());
        if (competence.getApprenant() != null) {
            response.setApprenantId(competence.getApprenant().getId());
        }
        return response;
    }

    public static ApprenantResponse toApprenantResponse(Apprenant apprenant) {
        ApprenantResponse response = new ApprenantResponse();
        response.setId(apprenant.getId());
        response.setNom(apprenant.getNom());
        response.setPrenom(apprenant.getPrenom());
        response.setEmail(apprenant.getEmail());
        response.setProfil(toProfilResponse(apprenant.getProfil()));
        return response;
    }
}
