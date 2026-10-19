package com.smartlearn.service;

import com.smartlearn.dto.ChapitreRevoirResponse;
import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Competence;
import com.smartlearn.entity.Chapitre;
import com.smartlearn.entity.Formation;
import com.smartlearn.exception.ResourceNotFoundException;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.CompetenceRepository;
import com.smartlearn.repository.ChapitreRepository;
import com.smartlearn.repository.FormationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChapitreRevoirService {

    private final ApprenantRepository apprenantRepository;
    private final CompetenceRepository competenceRepository;
    private final ChapitreRepository chapitreRepository;
    private final FormationRepository formationRepository;

    public ChapitreRevoirService(ApprenantRepository apprenantRepository,
                                  CompetenceRepository competenceRepository,
                                  ChapitreRepository chapitreRepository,
                                  FormationRepository formationRepository) {
        this.apprenantRepository = apprenantRepository;
        this.competenceRepository = competenceRepository;
        this.chapitreRepository = chapitreRepository;
        this.formationRepository = formationRepository;
    }

    public List<ChapitreRevoirResponse> detectChapitresARevoir(Long apprenantId) {
        Apprenant apprenant = apprenantRepository.findById(apprenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant introuvable : " + apprenantId));

        List<Competence> competences = competenceRepository.findByApprenantId(apprenantId);
        List<Formation> formations = formationRepository.findAll();

        Map<String, Integer> competenceLevels = new HashMap<>();
        for (Competence c : competences) {
            competenceLevels.put(c.getNom().toLowerCase(), c.getNiveau());
        }

        List<ChapitreRevoirResponse> result = new ArrayList<>();

        for (Formation formation : formations) {
            List<Chapitre> chapitres = chapitreRepository.findByFormationIdOrderByOrdreAsc(formation.getId());

            for (Chapitre chapitre : chapitres) {
                int priorite = calculatePriority(chapitre, competenceLevels);
                if (priorite > 0) {
                    String raison = buildRaison(chapitre, competenceLevels);
                    result.add(new ChapitreRevoirResponse(
                            chapitre.getId(),
                            chapitre.getTitre(),
                            formation.getId(),
                            formation.getTitre(),
                            chapitre.getOrdre(),
                            raison,
                            priorite
                    ));
                }
            }
        }

        result.sort((a, b) -> Integer.compare(b.getPriorite(), a.getPriorite()));
        return result;
    }

    private int calculatePriority(Chapitre chapitre, Map<String, Integer> competenceLevels) {
        String contenu = (chapitre.getContenu() != null ? chapitre.getContenu() : "").toLowerCase();
        String titre = chapitre.getTitre().toLowerCase();
        String text = titre + " " + contenu;

        int priority = 0;
        for (Map.Entry<String, Integer> entry : competenceLevels.entrySet()) {
            String skill = entry.getKey();
            int level = entry.getValue();
            if (text.contains(skill)) {
                if (level < 50) {
                    priority += 3;
                } else if (level < 75) {
                    priority += 1;
                }
            }
        }
        return priority;
    }

    private String buildRaison(Chapitre chapitre, Map<String, Integer> competenceLevels) {
        String contenu = (chapitre.getContenu() != null ? chapitre.getContenu() : "").toLowerCase();
        String titre = chapitre.getTitre().toLowerCase();
        String text = titre + " " + contenu;

        List<String> faiblesCompetences = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : competenceLevels.entrySet()) {
            String skill = entry.getKey();
            int level = entry.getValue();
            if (text.contains(skill) && level < 50) {
                faiblesCompetences.add(skill + " (" + level + "%)");
            }
        }

        if (faiblesCompetences.isEmpty()) {
            return "Compétences moyennes à renforcer";
        }
        return "Compétences faibles détectées : " + String.join(", ", faiblesCompetences);
    }
}