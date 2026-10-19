package com.smartlearn.service;

import com.smartlearn.dto.AnalyseIAResponse;
import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Competence;
import com.smartlearn.entity.Profil;
import com.smartlearn.exception.ResourceNotFoundException;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.CompetenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class AnalyseIAService {

    private static final Logger log = LoggerFactory.getLogger(AnalyseIAService.class);

    private final ApprenantRepository apprenantRepository;
    private final CompetenceRepository competenceRepository;
    private final RestTemplate restTemplate;

    // CORRECTION : URL configurable (Docker : MLA_SERVICE_URL=http://mla-service:8000).
    // La valeur par défaut reste utilisable si la classe est créée à la main (tests).
    @Value("${IA_SERVICE_URL:http://127.0.0.1:8001}")
    private String iaServiceUrl = "http://127.0.0.1:8001";

    public AnalyseIAService(ApprenantRepository apprenantRepository,
                             CompetenceRepository competenceRepository) {
        this.apprenantRepository = apprenantRepository;
        this.competenceRepository = competenceRepository;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    public AnalyseIAResponse analyserApprenant(Long apprenantId) {
        Apprenant apprenant = apprenantRepository.findById(apprenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant introuvable : " + apprenantId));

        List<Competence> competences = competenceRepository.findByApprenantId(apprenantId);
        Profil profil = apprenant.getProfil();

        // Tentative d'appel au service IA
        try {
            return callIAService(apprenant, competences, profil);
        } catch (Exception e) {
            // CORRECTION : l'erreur était avalée sans trace
            log.warn("Service IA indisponible ({}/analyze), analyse locale utilisée : {}",
                    iaServiceUrl, e.getMessage());
            return computeFallbackAnalysis(apprenant, competences, profil);
        }
    }

    @SuppressWarnings("unchecked")
    private AnalyseIAResponse callIAService(Apprenant apprenant, List<Competence> competences, Profil profil) {
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> profilMap = new HashMap<>();
        profilMap.put("niveau", profil != null && profil.getNiveau() != null ? profil.getNiveau() : "");
        profilMap.put("objectif", profil != null && profil.getObjectif() != null ? profil.getObjectif() : "");
        profilMap.put("domaineInteret", profil != null && profil.getDomaineInteret() != null ? profil.getDomaineInteret() : "");

        List<Map<String, Object>> compList = new ArrayList<>();
        for (Competence c : competences) {
            Map<String, Object> compMap = new HashMap<>();
            compMap.put("nom", c.getNom());
            compMap.put("niveau", c.getNiveau());
            compList.add(compMap);
        }
        profilMap.put("competences", compList);

        requestBody.put("profil", profilMap);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        Map<String, Object> response = restTemplate.postForObject(
                iaServiceUrl + "/analyze", entity, Map.class);

        // CORRECTION : réponse vide ou champ absent -> plus de NullPointerException
        if (response == null) {
            throw new IllegalStateException("Réponse vide du service IA");
        }

        AnalyseIAResponse result = new AnalyseIAResponse();
        result.setStyleApprentissage((String) response.get("styleApprentissage"));
        result.setForces(toStringList(response.get("forces")));
        result.setFaiblesses(toStringList(response.get("faiblesses")));
        result.setRecommandationsPersonnalisees(toStringList(response.get("recommandationsPersonnalisees")));
        Object score = response.get("scoreProgression");
        result.setScoreProgression(score instanceof Number ? ((Number) score).intValue() : 0);
        result.setNiveauGlobal((String) response.get("niveauGlobal"));

        // CORRECTION : le cast direct en Map<String, Double> échouait si Jackson renvoyait des Integer
        Map<String, Double> compEvol = new HashMap<>();
        Object evol = response.get("competencesEvolution");
        if (evol instanceof Map) {
            for (Map.Entry<?, ?> en : ((Map<?, ?>) evol).entrySet()) {
                if (en.getValue() instanceof Number) {
                    compEvol.put(String.valueOf(en.getKey()), ((Number) en.getValue()).doubleValue());
                }
            }
        }
        result.setCompetencesEvolution(compEvol);

        return result;
    }

    private static List<String> toStringList(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof List) {
            for (Object o : (List<?>) value) {
                if (o != null) out.add(o.toString());
            }
        }
        return out;
    }

    private AnalyseIAResponse computeFallbackAnalysis(Apprenant apprenant, List<Competence> competences, Profil profil) {
        AnalyseIAResponse result = new AnalyseIAResponse();

        // Déterminer le style d'apprentissage basé sur les compétences
        if (competences.isEmpty()) {
            result.setStyleApprentissage("Débutant - Explorer les bases");
        } else {
            double avgLevel = competences.stream().mapToInt(Competence::getNiveau).average().orElse(0.0);
            if (avgLevel < 50) {
                result.setStyleApprentissage("Débutant - Renforcement des bases");
            } else if (avgLevel < 75) {
                result.setStyleApprentissage("Intermédiaire - Approfondissement");
            } else {
                result.setStyleApprentissage("Avancé - Perfectionnement");
            }
        }

        // Forces et faiblesses
        List<String> forces = new ArrayList<>();
        List<String> faiblesses = new ArrayList<>();
        Map<String, Double> compEvol = new HashMap<>();

        for (Competence c : competences) {
            if (c.getNiveau() >= 75) {
                forces.add(c.getNom() + " (" + c.getNiveau() + "%)");
            } else if (c.getNiveau() < 50) {
                faiblesses.add(c.getNom() + " (" + c.getNiveau() + "%)");
            }
            compEvol.put(c.getNom(), (double) c.getNiveau());
        }

        result.setForces(forces);
        result.setFaiblesses(faiblesses);
        result.setCompetencesEvolution(compEvol);

        double avgLevel = competences.stream().mapToInt(Competence::getNiveau).average().orElse(0.0);

        // Recommandations (mêmes règles que le service Python)
        List<String> recommandations = new ArrayList<>();
        if (!faiblesses.isEmpty()) {
            recommandations.add("Consacrez des séances de révision aux compétences faibles : " +
                    String.join(", ", faiblesses));
        }
        // CORRECTION : un objectif vide ("") déclenchait une recommandation vide de sens
        if (profil != null && profil.getObjectif() != null && !profil.getObjectif().isBlank()) {
            recommandations.add("Alignez vos formations sur votre objectif : " + profil.getObjectif());
        }
        if (competences.size() < 3) {
            recommandations.add("Développez au moins 3 compétences pour une base solide");
        }
        if (avgLevel >= 75) {
            recommandations.add("Maintenez votre niveau avec des défis avancés");
        }
        result.setRecommandationsPersonnalisees(recommandations);

        // Score de progression
        result.setScoreProgression((int) avgLevel);

        if (avgLevel < 50) {
            result.setNiveauGlobal("Débutant");
        } else if (avgLevel < 75) {
            result.setNiveauGlobal("Intermédiaire");
        } else {
            result.setNiveauGlobal("Avancé");
        }

        return result;
    }
}