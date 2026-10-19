package com.smartlearn.service;

import com.smartlearn.dto.StatistiquesResponse;
import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Competence;
import com.smartlearn.entity.Chapitre;
import com.smartlearn.entity.Formation;
import com.smartlearn.exception.ResourceNotFoundException;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.CompetenceRepository;
import com.smartlearn.repository.ChapitreRepository;
import com.smartlearn.repository.ExamenRepository;
import com.smartlearn.repository.SeanceRepository;
import com.smartlearn.repository.NotificationRepository;
import com.smartlearn.repository.FormationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatistiquesService {

    private final ApprenantRepository apprenantRepository;
    private final CompetenceRepository competenceRepository;
    private final ChapitreRepository chapitreRepository;
    private final ExamenRepository examenRepository;
    private final SeanceRepository seanceRepository;
    private final NotificationRepository notificationRepository;
    private final FormationRepository formationRepository;

    public StatistiquesService(ApprenantRepository apprenantRepository,
                                 CompetenceRepository competenceRepository,
                                 ChapitreRepository chapitreRepository,
                                 ExamenRepository examenRepository,
                                 SeanceRepository seanceRepository,
                                 NotificationRepository notificationRepository,
                                 FormationRepository formationRepository) {
        this.apprenantRepository = apprenantRepository;
        this.competenceRepository = competenceRepository;
        this.chapitreRepository = chapitreRepository;
        this.examenRepository = examenRepository;
        this.seanceRepository = seanceRepository;
        this.notificationRepository = notificationRepository;
        this.formationRepository = formationRepository;
    }

    public StatistiquesResponse getStatistiques(Long apprenantId) {
        Apprenant apprenant = apprenantRepository.findById(apprenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant introuvable : " + apprenantId));

        List<Competence> competences = competenceRepository.findByApprenantId(apprenantId);
        List<Formation> formations = formationRepository.findAll();

        StatistiquesResponse stats = new StatistiquesResponse();
        stats.setApprenantId(apprenantId);
        stats.setApprenantNom(apprenant.getNom() + " " + apprenant.getPrenom());
        stats.setTotalExamens((int) examenRepository.countByApprenantId(apprenantId));
        stats.setTotalSeances((int) seanceRepository.countByApprenantId(apprenantId));
        stats.setTotalNotifications((int) notificationRepository.countByApprenantId(apprenantId));

        // Compétences par niveau
        Map<String, Integer> compByLevel = new HashMap<>();
        compByLevel.put("faible", 0);
        compByLevel.put("moyen", 0);
        compByLevel.put("bon", 0);
        compByLevel.put("avance", 0);
        for (Competence c : competences) {
            if (c.getNiveau() < 50) compByLevel.put("faible", compByLevel.get("faible") + 1);
            else if (c.getNiveau() < 75) compByLevel.put("moyen", compByLevel.get("moyen") + 1);
            else if (c.getNiveau() < 90) compByLevel.put("bon", compByLevel.get("bon") + 1);
            else compByLevel.put("avance", compByLevel.get("avance") + 1);
        }
        stats.setCompetencesByLevel(compByLevel);

        // Niveau moyen des compétences
        double avg = competences.stream().mapToInt(Competence::getNiveau).average().orElse(0.0);
        stats.setAverageCompetenceLevel(avg);

        // Progression par formation
        List<StatistiquesResponse.FormationProgress> formationProgress = new ArrayList<>();
        int totalChapitres = 0;
        int chapitresARevoir = 0;

        for (Formation f : formations) {
            List<Chapitre> chapitres = chapitreRepository.findByFormationIdOrderByOrdreAsc(f.getId());
            totalChapitres += chapitres.size();

            int faibles = 0;
            for (Chapitre ch : chapitres) {
                if (isChapitreFaible(ch, competences)) {
                    faibles++;
                    chapitresARevoir++;
                }
            }
            formationProgress.add(new StatistiquesResponse.FormationProgress(
                    f.getId(), f.getTitre(), chapitres.size(), faibles
            ));
        }

        stats.setTotalChapitres(totalChapitres);
        stats.setChapitresARevoir(chapitresARevoir);
        stats.setFormationProgress(formationProgress);

        return stats;
    }

    private boolean isChapitreFaible(Chapitre chapitre, List<Competence> competences) {
        String contenu = (chapitre.getContenu() != null ? chapitre.getContenu() : "").toLowerCase();
        String titre = chapitre.getTitre().toLowerCase();
        String text = titre + " " + contenu;

        for (Competence c : competences) {
            if (text.contains(c.getNom().toLowerCase()) && c.getNiveau() < 50) {
                return true;
            }
        }
        return false;
    }
}