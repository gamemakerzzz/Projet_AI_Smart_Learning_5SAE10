package com.smartlearn.service;

import com.smartlearn.dto.ExamenRequest;
import com.smartlearn.dto.ExamenResponse;
import com.smartlearn.dto.NotificationRequest;
import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Examen;
import com.smartlearn.entity.Formation;
import com.smartlearn.exception.ResourceNotFoundException;
import com.smartlearn.repository.ExamenRepository;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.FormationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExamenService {

    private static final Logger log = LoggerFactory.getLogger(ExamenService.class);

    private final ExamenRepository examenRepository;
    private final ApprenantRepository apprenantRepository;
    private final FormationRepository formationRepository;
    private final NotificationService notificationService;
    private final NotificationRappelService rappelService;

    public ExamenService(ExamenRepository examenRepository,
                         ApprenantRepository apprenantRepository,
                         FormationRepository formationRepository,
                         NotificationService notificationService,
                         NotificationRappelService rappelService) {
        this.examenRepository = examenRepository;
        this.apprenantRepository = apprenantRepository;
        this.formationRepository = formationRepository;
        this.notificationService = notificationService;
        this.rappelService = rappelService;
    }

    public List<ExamenResponse> getAllByApprenant(Long apprenantId) {
        return examenRepository.findByApprenantIdOrderByIdAsc(apprenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ExamenResponse> getAll() {
        return examenRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ExamenResponse> getAllByApprenantAndFormation(Long apprenantId, Long formationId) {
        return examenRepository.findByApprenantIdAndFormationId(apprenantId, formationId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ExamenResponse> getAllByApprenantAndUpcoming(Long apprenantId) {
        return examenRepository.findByApprenantIdAndDateExamenAfter(apprenantId, java.time.LocalDateTime.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ExamenResponse getById(Long id) {
        Examen examen = examenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen introuvable : " + id));
        return toResponse(examen);
    }

    @Transactional
    public ExamenResponse create(Long apprenantId, ExamenRequest request) {
        Apprenant apprenant = apprenantRepository.findById(apprenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant introuvable : " + apprenantId));

        Examen examen = new Examen();
        examen.setTitre(request.getTitre());
        examen.setDescription(request.getDescription());
        examen.setDateExamen(request.getDateExamen());
        examen.setDureeMinutes(request.getDureeMinutes());
        examen.setCoefficient(request.getCoefficient() != null ? request.getCoefficient() : 1);
        examen.setApprenant(apprenant);

        if (request.getFormationId() != null) {
            Formation formation = formationRepository.findById(request.getFormationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable : " + request.getFormationId()));
            examen.setFormation(formation);
        }

        Examen saved = examenRepository.save(examen);
        log.debug("Examen créé: id={}, apprenantId={}", saved.getId(), apprenantId);

        // Notification création examen
        try {
            NotificationRequest notifReq = new NotificationRequest();
            notifReq.setMessage("Nouvel examen planifié : \"" + saved.getTitre() + "\" le " + 
                saved.getDateExamen().toLocalDate() + " à " + saved.getDateExamen().toLocalTime());
            notifReq.setType("EXAMEN");
            notificationService.create(apprenantId, notifReq);
        } catch (Exception e) {
            log.warn("Notification création examen échouée: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public ExamenResponse update(Long id, ExamenRequest request) {
        Examen examen = examenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen introuvable : " + id));

        boolean dateChanged = !request.getDateExamen().equals(examen.getDateExamen());
        boolean titreChanged = !request.getTitre().equals(examen.getTitre());

        examen.setTitre(request.getTitre());
        examen.setDescription(request.getDescription());
        examen.setDateExamen(request.getDateExamen());
        examen.setDureeMinutes(request.getDureeMinutes());
        examen.setCoefficient(request.getCoefficient() != null ? request.getCoefficient() : examen.getCoefficient());

        if (request.getFormationId() != null) {
            Formation formation = formationRepository.findById(request.getFormationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable : " + request.getFormationId()));
            examen.setFormation(formation);
        } else {
            examen.setFormation(null);
        }

        Examen saved = examenRepository.save(examen);
        log.debug("Examen mis à jour: id={}", saved.getId());

        // Notification modification si changement important
        if (dateChanged || titreChanged) {
            try {
                NotificationRequest notifReq = new NotificationRequest();
                StringBuilder msg = new StringBuilder("Examen modifié : \"" + saved.getTitre() + "\"");
                if (dateChanged) {
                    msg.append(" - nouvelle date : ").append(saved.getDateExamen().toLocalDate())
                       .append(" à ").append(saved.getDateExamen().toLocalTime());
                }
                notifReq.setMessage(msg.toString());
                notifReq.setType("EXAMEN");
                notificationService.create(saved.getApprenant().getId(), notifReq);
            } catch (Exception e) {
                log.warn("Notification modification examen échouée: {}", e.getMessage());
            }
        }

        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Examen examen = examenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen introuvable : " + id));
        
        Long apprenantId = examen.getApprenant().getId();
        
        // Nettoyer les rappels associés avant suppression
        rappelService.supprimerRappelsParCible("EXAMEN", id);
        
        examenRepository.delete(examen);
        log.debug("Examen supprimé: id={}", id);
    }

    private ExamenResponse toResponse(Examen examen) {
        ExamenResponse response = new ExamenResponse();
        response.setId(examen.getId());
        response.setTitre(examen.getTitre());
        response.setDescription(examen.getDescription());
        response.setDateExamen(examen.getDateExamen());
        response.setDureeMinutes(examen.getDureeMinutes());
        response.setCoefficient(examen.getCoefficient());
        response.setApprenantId(examen.getApprenant().getId());
        if (examen.getFormation() != null) {
            response.setFormationId(examen.getFormation().getId());
        }
        return response;
    }
}