package com.smartlearn.service;

import com.smartlearn.dto.PlanificationRequest;
import com.smartlearn.dto.PlanificationResponse;
import com.smartlearn.dto.NotificationRequest;
import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Examen;
import com.smartlearn.entity.Formation;
import com.smartlearn.entity.Seance;
import com.smartlearn.exception.BadRequestException;
import com.smartlearn.exception.ResourceNotFoundException;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.ExamenRepository;
import com.smartlearn.repository.FormationRepository;
import com.smartlearn.repository.SeanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlanificationService {

    private static final Logger log = LoggerFactory.getLogger(PlanificationService.class);

    private final SeanceRepository seanceRepository;
    private final ApprenantRepository apprenantRepository;
    private final ExamenRepository examenRepository;
    private final FormationRepository formationRepository;
    private final NotificationService notificationService;

    public PlanificationService(SeanceRepository seanceRepository,
                                ApprenantRepository apprenantRepository,
                                ExamenRepository examenRepository,
                                FormationRepository formationRepository,
                                NotificationService notificationService) {
        this.seanceRepository = seanceRepository;
        this.apprenantRepository = apprenantRepository;
        this.examenRepository = examenRepository;
        this.formationRepository = formationRepository;
        this.notificationService = notificationService;
    }

    // Pas de readOnly : cette méthode enregistre des séances.
    public List<PlanificationResponse> generate(PlanificationRequest request) {
        if (request.getApprenantId() == null) {
            throw new BadRequestException("L'ID de l'apprenant est requis");
        }
        if (request.getDateDebut() == null || request.getDateFin() == null) {
            throw new BadRequestException("Les dates de début et de fin sont requises");
        }
        if (request.getDateFin().isBefore(request.getDateDebut())) {
            throw new BadRequestException("La date de fin doit être égale ou postérieure à la date de début");
        }

        Apprenant apprenant = apprenantRepository.findById(request.getApprenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant introuvable : " + request.getApprenantId()));

        List<Formation> formations = new ArrayList<>();
        if (request.getFormationIds() != null && !request.getFormationIds().isEmpty()) {
            for (Long formationId : request.getFormationIds()) {
                Formation formation = formationRepository.findById(formationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable : " + formationId));
                formations.add(formation);
            }
        } else {
            // Aucune formation choisie : on planifie toutes les formations
            formations.addAll(formationRepository.findAll());
        }
        if (formations.isEmpty()) {
            throw new BadRequestException("Aucune formation disponible à planifier");
        }

        LocalDateTime debutPeriode = request.getDateDebut().atStartOfDay();
        LocalDateTime finPeriode = request.getDateFin().plusDays(1).atStartOfDay();

        // Examens de la période (disponibles pour une logique de priorisation ultérieure)
        List<Examen> examens = examenRepository.findByApprenantIdAndDateExamenBetween(
                request.getApprenantId(), debutPeriode, finPeriode);
        log.debug("{} examen(s) dans la période pour apprenantId={}", examens.size(), request.getApprenantId());

        List<PlanificationResponse> result = new ArrayList<>();
        LocalDate current = request.getDateDebut();
        LocalDate end = request.getDateFin();

        while (!current.isAfter(end)) {
            for (Formation formation : formations) {
                Seance seance = new Seance();
                seance.setTitre("Révision - " + formation.getTitre());
                seance.setDescription("Séance de révision planifiée pour la formation : " + formation.getTitre());
                seance.setDateHeureDebut(current.atTime(9, 0));
                seance.setDateHeureFin(current.atTime(11, 0));
                seance.setStatut("PLANIFIE");
                seance.setApprenant(apprenant);
                seance.setFormation(formation);

                Seance saved = seanceRepository.save(seance);
                result.add(toResponse(saved));
            }
            current = current.plusDays(1);
        }

        log.debug("Planification générée: {} séances pour apprenantId={}", result.size(), request.getApprenantId());

        // Notification de planification
        try {
            NotificationRequest notifReq = new NotificationRequest();
            notifReq.setMessage("Plan de révision généré : " + result.size() + " séance(s) planifiée(s) du " +
                request.getDateDebut() + " au " + request.getDateFin() + ".");
            notifReq.setType("PLANIFICATION");
            notificationService.create(request.getApprenantId(), notifReq);
        } catch (Exception e) {
            log.warn("Notification planification échouée: {}", e.getMessage());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<PlanificationResponse> getAllByApprenant(Long apprenantId) {
        return seanceRepository.findByApprenantIdOrderByIdAsc(apprenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PlanificationResponse toResponse(Seance seance) {
        PlanificationResponse response = new PlanificationResponse();
        response.setId(seance.getId());
        response.setTitre(seance.getTitre());
        response.setDateHeureDebut(seance.getDateHeureDebut());
        response.setDateHeureFin(seance.getDateHeureFin());
        response.setStatut(seance.getStatut());
        if (seance.getFormation() != null) {
            response.setFormationId(seance.getFormation().getId());
            response.setFormationTitre(seance.getFormation().getTitre());
        }
        return response;
    }
}