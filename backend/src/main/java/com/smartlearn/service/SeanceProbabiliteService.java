package com.smartlearn.service;

import com.smartlearn.dto.CreneauProbabiliteRequest;
import com.smartlearn.dto.CreneauProbabiliteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SeanceProbabiliteService {

    private static final Logger log = LoggerFactory.getLogger(SeanceProbabiliteService.class);

    // CORRECTION : URL configurable (sous Docker : MLA_SERVICE_URL=http://mla-service:8000)
    private final String mlaServiceUrl;

    private final RestTemplate restTemplate;

    public SeanceProbabiliteService(@Value("${mla.service.url:http://localhost:8000}") String mlaServiceUrl) {
        this.mlaServiceUrl = mlaServiceUrl;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    public List<CreneauProbabiliteResponse> calculerProbabilites(List<CreneauProbabiliteRequest> creneaux) {
        if (creneaux == null || creneaux.isEmpty()) {
            return new ArrayList<>();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<CreneauProbabiliteRequest>> entity = new HttpEntity<>(creneaux, headers);

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> response = restTemplate.postForObject(
                    mlaServiceUrl + "/seance/probabilite", entity, List.class);

            // CORRECTION : réponse nulle ou de taille différente -> le front lit par index,
            // un décalage donnerait la mauvaise probabilité à une séance.
            if (response == null || response.size() != creneaux.size()) {
                throw new IllegalStateException("Réponse invalide du service IA : "
                        + (response == null ? "vide" : response.size() + " éléments pour " + creneaux.size()));
            }

            List<CreneauProbabiliteResponse> result = new ArrayList<>();
            for (int i = 0; i < response.size(); i++) {
                Map<String, Object> m = response.get(i);
                CreneauProbabiliteRequest src = creneaux.get(i);
                CreneauProbabiliteResponse r = new CreneauProbabiliteResponse();
                // CORRECTION : champs absents -> valeur du créneau envoyé au lieu d'un NullPointerException
                r.setHeure(toInt(m.get("heure"), src.getHeure()));
                r.setJourSemaine(toInt(m.get("jourSemaine"), src.getJourSemaine()));
                r.setDureeMin(toInt(m.get("dureeMin"), src.getDureeMin()));
                r.setProbabilite(m.get("probabilite") != null
                        ? Double.valueOf(m.get("probabilite").toString())
                        : regleSimple(src));
                r.setSource(m.get("source") != null ? m.get("source").toString() : "SCORE");
                result.add(r);
            }
            return result;
        } catch (Exception e) {
            // CORRECTION : l'erreur n'était pas tracée
            log.warn("Service IA indisponible ({}/seance/probabilite), règles locales utilisées : {}",
                    mlaServiceUrl, e.getMessage());
            List<CreneauProbabiliteResponse> result = new ArrayList<>();
            for (CreneauProbabiliteRequest c : creneaux) {
                CreneauProbabiliteResponse r = new CreneauProbabiliteResponse();
                r.setHeure(c.getHeure());
                r.setJourSemaine(c.getJourSemaine());
                r.setDureeMin(c.getDureeMin());
                double p = regleSimple(c);
                r.setProbabilite(Math.round(p * 1000.0) / 1000.0);
                r.setSource("REGLES");
                result.add(r);
            }
            return result;
        }
    }

    private static Integer toInt(Object value, Integer defaut) {
        return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : defaut;
    }

    private double regleSimple(CreneauProbabiliteRequest c) {
        double p = 0.7;
        Integer h = c.getHeure() != null ? c.getHeure() : 12;
        Integer d = c.getDureeMin() != null ? c.getDureeMin() : 60;
        Integer m = c.getSeancesMemeJour() != null ? c.getSeancesMemeJour() : 0;
        if (h >= 21 || h <= 7) p -= 0.3;
        if (d > 90) p -= 0.2;
        p -= 0.1 * m;
        return Math.max(0.05, Math.min(0.95, p));
    }
}