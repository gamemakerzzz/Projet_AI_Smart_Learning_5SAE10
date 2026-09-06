package com.smartlearn.service;

import com.smartlearn.dto.RecommendationResponse;
import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Competence;
import com.smartlearn.entity.Formation;

import com.smartlearn.entity.Profil;
import com.smartlearn.repository.CompetenceRepository;
import com.smartlearn.repository.FormationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final ApprenantService apprenantService;
    private final FormationRepository formationRepository;
    private final CompetenceRepository competenceRepository;
    private final RestTemplate restTemplate;

    @Value("${mla.service.url:http://localhost:8000}")
    private String mlaServiceUrl;

    public RecommendationService(ApprenantService apprenantService,
                                 FormationRepository formationRepository,
                                 CompetenceRepository competenceRepository) {
        this.apprenantService = apprenantService;
        this.formationRepository = formationRepository;
        this.competenceRepository = competenceRepository;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    public List<RecommendationResponse> getRecommendations(Long apprenantId) {
        Apprenant apprenant = apprenantService.getApprenant(apprenantId);
        List<Competence> competences = competenceRepository.findByApprenantId(apprenantId);
        List<Formation> formations = formationRepository.findAllWithCategorie();

        try {
            return callMlaService(apprenant, competences, formations);
        } catch (Exception e) {
            // Spring fallback algorithm if FastAPI microservice is offline
            return computeFallbackRecommendations(apprenant, competences, formations);
        }
    }

    @SuppressWarnings("unchecked")
    private List<RecommendationResponse> callMlaService(Apprenant apprenant, List<Competence> competences, List<Formation> formations) {
        Map<String, Object> requestBody = new HashMap<>();

        Profil profil = apprenant.getProfil();
        Map<String, Object> profilMap = new HashMap<>();
        profilMap.put("niveau", profil != null && profil.getNiveau() != null ? profil.getNiveau() : "");
        profilMap.put("objectif", profil != null && profil.getObjectif() != null ? profil.getObjectif() : "");
        profilMap.put("domaineInteret", profil != null && profil.getDomaineInteret() != null ? profil.getDomaineInteret() : "");
        profilMap.put("competences", competences.stream().map(Competence::getNom).toList());

        List<Map<String, Object>> formationsList = new ArrayList<>();
        for (Formation f : formations) {
            Map<String, Object> fMap = new HashMap<>();
            fMap.put("id", f.getId());
            fMap.put("titre", f.getTitre());
            fMap.put("description", f.getDescription() != null ? f.getDescription() : "");
            fMap.put("niveau", f.getNiveau());
            fMap.put("motsCles", f.getMotsCles() != null ? f.getMotsCles() : "");
            fMap.put("categorie", f.getCategorie() != null ? f.getCategorie().getNom() : "");
            formationsList.add(fMap);
        }

        requestBody.put("profil", profilMap);
        requestBody.put("formations", formationsList);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        List<Map<String, Object>> mlaResponse = restTemplate.postForObject(mlaServiceUrl + "/recommend", entity, List.class);

        List<RecommendationResponse> result = new ArrayList<>();
        if (mlaResponse != null) {
            Map<Long, Formation> formationMap = new HashMap<>();
            formations.forEach(f -> formationMap.put(f.getId(), f));

            for (Map<String, Object> item : mlaResponse) {
                Long fId = ((Number) item.get("formationId")).longValue();
                Double score = ((Number) item.get("score")).doubleValue();
                Formation f = formationMap.get(fId);

                if (f != null) {
                    result.add(new RecommendationResponse(
                            f.getId(),
                            f.getTitre(),
                            score,
                            f.getDescription(),
                            f.getNiveau(),
                            f.getCategorie() != null ? f.getCategorie().getNom() : null,
                            f.getPrix()
                    ));
                }
            }
        }
        return result;
    }

    private List<RecommendationResponse> computeFallbackRecommendations(Apprenant apprenant, List<Competence> competences, List<Formation> formations) {
        Profil profil = apprenant.getProfil();
        String userText = (
                (profil != null && profil.getObjectif() != null ? profil.getObjectif() : "") + " " +
                (profil != null && profil.getDomaineInteret() != null ? profil.getDomaineInteret() : "") + " " +
                competences.stream().map(Competence::getNom).reduce("", (a, b) -> a + " " + b) + " " +
                (profil != null && profil.getNiveau() != null ? profil.getNiveau() : "")
        ).toLowerCase();

        List<RecommendationResponse> result = new ArrayList<>();
        for (Formation f : formations) {
            String formText = (
                    f.getTitre() + " " +
                    (f.getDescription() != null ? f.getDescription() : "") + " " +
                    (f.getMotsCles() != null ? f.getMotsCles() : "") + " " +
                    (f.getCategorie() != null ? f.getCategorie().getNom() : "")
            ).toLowerCase();

            var words = java.util.Arrays.stream(userText.split("\\s+"))
                    .filter(word -> word.length() > 2).distinct().toList();
            long matches = words.stream().filter(formText::contains).count();
            double score = words.isEmpty() ? 0.0 : (double) matches / words.size();

            result.add(new RecommendationResponse(
                    f.getId(),
                    f.getTitre(),
                    score,
                    f.getDescription(),
                    f.getNiveau(),
                    f.getCategorie() != null ? f.getCategorie().getNom() : null,
                    f.getPrix()
            ));
        }

        result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return result;
    }
}
