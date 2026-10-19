package com.smartlearn.service;

import com.smartlearn.dto.PlanificationIARequest;
import com.smartlearn.dto.PlanificationIAResponse;
import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Competence;
import com.smartlearn.entity.Examen;
import com.smartlearn.entity.Formation;
import com.smartlearn.exception.ResourceNotFoundException;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.CompetenceRepository;
import com.smartlearn.repository.ExamenRepository;
import com.smartlearn.repository.FormationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PlanificationIAService {

    private static final Logger log = LoggerFactory.getLogger(PlanificationIAService.class);

    private static final int JOURS_REVISION = 5;
    private static final String[][] CRENEAUX = {
            {"09:00", "11:00"}, {"14:00", "16:00"}, {"16:30", "18:30"}
    };

    private final String mlaServiceUrl;
    private final ApprenantRepository apprenantRepository;
    private final CompetenceRepository competenceRepository;
    private final ExamenRepository examenRepository;
    private final FormationRepository formationRepository;
    private final RestTemplate restTemplate;

    public PlanificationIAService(ApprenantRepository apprenantRepository,
                                  CompetenceRepository competenceRepository,
                                  ExamenRepository examenRepository,
                                  FormationRepository formationRepository,
                                  // CORRECTION : l'URL était codée en dur sur localhost:8000, ce qui
                                  // ignore MLA_SERVICE_URL=http://mla-service:8000 sous Docker.
                                  @Value("${IA_SERVICE_URL:http://127.0.0.1:8001}") String mlaServiceUrl) {
        this.apprenantRepository = apprenantRepository;
        this.competenceRepository = competenceRepository;
        this.examenRepository = examenRepository;
        this.formationRepository = formationRepository;
        this.mlaServiceUrl = mlaServiceUrl;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    public PlanificationIAResponse genererPlanification(Long apprenantId, LocalDate dateDebut, LocalDate dateFin, List<Long> formationIds) {
        Apprenant apprenant = apprenantRepository.findById(apprenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant introuvable : " + apprenantId));

        List<Examen> examens = examenRepository.findByApprenantId(apprenantId);
        List<Competence> competences = competenceRepository.findByApprenantId(apprenantId);
        List<Formation> formations;
        if (formationIds != null && !formationIds.isEmpty()) {
            formations = new ArrayList<>();
            for (Long id : formationIds) {
                Formation f = formationRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable : " + id));
                formations.add(f);
            }
        } else {
            formations = formationRepository.findAll();
        }

        // Construire le payload pour le service MLA
        Map<String, Object> profilMap = new HashMap<>();
        profilMap.put("apprenantId", apprenantId);
        profilMap.put("dateDebut", dateDebut != null ? dateDebut.format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
        profilMap.put("dateFin", dateFin != null ? dateFin.format(DateTimeFormatter.ISO_LOCAL_DATE) : "");

        List<Map<String, Object>> examensList = new ArrayList<>();
        for (Examen e : examens) {
            Map<String, Object> em = new HashMap<>();
            em.put("id", e.getId());
            em.put("titre", e.getTitre());
            em.put("dateExamen", e.getDateExamen() != null ? e.getDateExamen().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
            em.put("coefficient", e.getCoefficient() != null ? e.getCoefficient() : 1);
            em.put("formationId", e.getFormation() != null ? e.getFormation().getId() : null);
            em.put("formationTitre", e.getFormation() != null ? e.getFormation().getTitre() : "");
            examensList.add(em);
        }

        List<Map<String, Object>> competencesList = new ArrayList<>();
        for (Competence c : competences) {
            Map<String, Object> cm = new HashMap<>();
            cm.put("nom", c.getNom());
            cm.put("niveau", c.getNiveau());
            competencesList.add(cm);
        }

        List<Map<String, Object>> formationsList = new ArrayList<>();
        for (Formation f : formations) {
            Map<String, Object> fm = new HashMap<>();
            fm.put("id", f.getId());
            fm.put("titre", f.getTitre());
            formationsList.add(fm);
        }

        profilMap.put("examens", examensList);
        profilMap.put("competences", competencesList);
        profilMap.put("formations", formationsList);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(profilMap, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    mlaServiceUrl + "/planification", entity, Map.class);
            if (response == null) {
                throw new IllegalStateException("Réponse vide du service IA");
            }
            return mapToResponse(apprenantId, dateDebut, dateFin, response);
        } catch (Exception e) {
            // CORRECTION : l'erreur était avalée sans trace (d'où le faux « Aucune séance »).
            log.error("Service IA indisponible ou route introuvable ({}/planification) : {}",
                    mlaServiceUrl, e.getMessage());
            return fallbackPlanification(apprenantId, dateDebut, dateFin, formations, examens);
        }
    }

    @SuppressWarnings("unchecked")
    private PlanificationIAResponse mapToResponse(Long apprenantId, LocalDate dateDebut, LocalDate dateFin, Map<String, Object> response) {
        PlanificationIAResponse result = new PlanificationIAResponse();
        result.setApprenantId(apprenantId);
        result.setDateDebut(dateDebut);
        result.setDateFin(dateFin);
        result.setTotalSeances(((Number) response.getOrDefault("totalSeances", 0)).intValue());
        result.setPeriode((String) response.getOrDefault("periode", ""));

        List<Map<String, Object>> seancesList = (List<Map<String, Object>>) response.getOrDefault("seances", new ArrayList<>());
        List<PlanificationIAResponse.SeanceIAResponse> seances = new ArrayList<>();
        for (Map<String, Object> s : seancesList) {
            PlanificationIAResponse.SeanceIAResponse seance = new PlanificationIAResponse.SeanceIAResponse();
            seance.setDate((String) s.getOrDefault("date", ""));
            seance.setHeureDebut((String) s.getOrDefault("heureDebut", ""));
            seance.setHeureFin((String) s.getOrDefault("heureFin", ""));
            seance.setTitre((String) s.getOrDefault("titre", ""));
            seance.setDescription((String) s.getOrDefault("description", ""));
            Object fid = s.get("formationId");
            seance.setFormationId(fid != null ? Long.valueOf(fid.toString()) : null);
            seance.setFormationTitre((String) s.getOrDefault("formationTitre", ""));
            Object prio = s.getOrDefault("priorite", 0);
            seance.setPriorite(prio != null ? ((Number) prio).intValue() : 0);
            seance.setMotif((String) s.getOrDefault("motif", ""));
            seances.add(seance);
        }
        result.setSeances(seances);
        return result;
    }

    /**
     * Plan local de secours quand le service IA ne répond pas : jusqu'à 5 jours de révision
     * avant chaque examen à venir (même logique que la route Python /planification).
     */
    private PlanificationIAResponse fallbackPlanification(Long apprenantId, LocalDate dateDebut, LocalDate dateFin,
                                                          List<Formation> formations, List<Examen> examens) {
        PlanificationIAResponse result = new PlanificationIAResponse();
        result.setApprenantId(apprenantId);
        result.setDateDebut(dateDebut);
        result.setDateFin(dateFin);
        result.setPeriode(dateDebut + " au " + dateFin);

        List<PlanificationIAResponse.SeanceIAResponse> seances = new ArrayList<>();
        if (dateDebut == null || dateFin == null) {
            result.setTotalSeances(0);
            result.setSeances(seances);
            return result;
        }

        Set<Long> formationIds = new HashSet<>();
        for (Formation f : formations) {
            formationIds.add(f.getId());
        }

        // clé "date|formationId" -> une seule séance par jour et par formation
        Map<String, PlanificationIAResponse.SeanceIAResponse> parCle = new LinkedHashMap<>();
        for (Examen e : examens) {
            if (e.getDateExamen() == null || e.getFormation() == null) continue;
            if (!formationIds.contains(e.getFormation().getId())) continue;

            LocalDate dExam = e.getDateExamen().toLocalDate();
            if (!dExam.isAfter(dateDebut)) continue; // examen passé ou le jour même

            LocalDate minPremier = dExam.minusDays(JOURS_REVISION);
            LocalDate premier = dateDebut.isAfter(minPremier) ? dateDebut : minPremier;
            LocalDate veille = dExam.minusDays(1);
            LocalDate dernier = dateFin.isBefore(veille) ? dateFin : veille;

            double coef = e.getCoefficient() != null ? e.getCoefficient().doubleValue() : 1.0;
            for (LocalDate d = premier; !d.isAfter(dernier); d = d.plusDays(1)) {
                long joursRestants = java.time.temporal.ChronoUnit.DAYS.between(d, dExam);
                int priorite = (int) Math.round(coef * 2) + (int) Math.max(0, 3 - joursRestants);
                String cle = d + "|" + e.getFormation().getId();

                PlanificationIAResponse.SeanceIAResponse existante = parCle.get(cle);
                if (existante != null && existante.getPriorite() >= priorite) continue;

                PlanificationIAResponse.SeanceIAResponse s = new PlanificationIAResponse.SeanceIAResponse();
                s.setDate(d.toString());
                s.setTitre("Révision - " + e.getFormation().getTitre());
                s.setDescription("Séance de révision planifiée pour la formation : " + e.getFormation().getTitre());
                s.setFormationId(e.getFormation().getId());
                s.setFormationTitre(e.getFormation().getTitre());
                s.setPriorite(priorite);
                s.setMotif("Examen le " + dExam + " (plan local, service IA indisponible)");
                parCle.put(cle, s);
            }
        }

        seances.addAll(parCle.values());
        seances.sort(Comparator.comparing(PlanificationIAResponse.SeanceIAResponse::getDate)
                .thenComparing(PlanificationIAResponse.SeanceIAResponse::getPriorite, Comparator.reverseOrder()));

        // Un créneau différent par séance d'une même journée
        Map<String, Integer> parJour = new HashMap<>();
        for (PlanificationIAResponse.SeanceIAResponse s : seances) {
            int i = parJour.merge(s.getDate(), 1, Integer::sum) - 1;
            String[] c = CRENEAUX[i % CRENEAUX.length];
            s.setHeureDebut(c[0]);
            s.setHeureFin(c[1]);
        }

        result.setTotalSeances(seances.size());
        result.setSeances(seances);
        return result;
    }
}