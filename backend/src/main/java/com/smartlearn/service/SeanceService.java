package com.smartlearn.service;

import com.smartlearn.dto.SeanceRequest;
import com.smartlearn.dto.SeanceResponse;
import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Formation;
import com.smartlearn.entity.Seance;
import com.smartlearn.exception.BadRequestException;
import com.smartlearn.exception.DuplicateResourceException;
import com.smartlearn.exception.ResourceNotFoundException;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.FormationRepository;
import com.smartlearn.repository.SeanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SeanceService {

    private static final Logger log = LoggerFactory.getLogger(SeanceService.class);

    private final SeanceRepository seanceRepository;
    private final ApprenantRepository apprenantRepository;
    private final FormationRepository formationRepository;

    public SeanceService(SeanceRepository seanceRepository,
                         ApprenantRepository apprenantRepository,
                         FormationRepository formationRepository) {
        this.seanceRepository = seanceRepository;
        this.apprenantRepository = apprenantRepository;
        this.formationRepository = formationRepository;
    }

    @Transactional(readOnly = true)
    public List<SeanceResponse> getAllByApprenant(Long apprenantId) {
        return seanceRepository.findByApprenantIdOrderByIdAsc(apprenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SeanceResponse> getAllByApprenantAndFormation(Long apprenantId, Long formationId) {
        return seanceRepository.findByApprenantIdAndFormationId(apprenantId, formationId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SeanceResponse> getAllByApprenantAndPeriod(Long apprenantId,
                                                           java.time.LocalDateTime debut,
                                                           java.time.LocalDateTime fin) {
        return seanceRepository.findByApprenantIdAndDateHeureDebutBetween(apprenantId, debut, fin).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SeanceResponse getById(Long id) {
        Seance seance = getSeance(id);
        return toResponse(seance);
    }

    public SeanceResponse create(Long apprenantId, SeanceRequest request) {
        Apprenant apprenant = apprenantRepository.findById(apprenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant introuvable : " + apprenantId));

        validateSeance(request);

        Seance seance = new Seance();
        seance.setTitre(request.getTitre());
        seance.setDescription(request.getDescription());
        seance.setDateHeureDebut(request.getDateHeureDebut());
        seance.setDateHeureFin(request.getDateHeureFin());
        seance.setStatut(request.getStatut() != null ? request.getStatut() : "PLANIFIE");
        seance.setApprenant(apprenant);

        if (request.getFormationId() != null) {
            Formation formation = formationRepository.findById(request.getFormationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable : " + request.getFormationId()));
            seance.setFormation(formation);
        }

        Seance saved = seanceRepository.save(seance);
        log.debug("Séance créée: id={}, apprenantId={}", saved.getId(), apprenantId);
        return toResponse(saved);
    }

    public SeanceResponse update(Long id, SeanceRequest request) {
        Seance seance = getSeance(id);

        validateSeance(request);

        seance.setTitre(request.getTitre());
        seance.setDescription(request.getDescription());
        seance.setDateHeureDebut(request.getDateHeureDebut());
        seance.setDateHeureFin(request.getDateHeureFin());
        seance.setStatut(request.getStatut() != null ? request.getStatut() : seance.getStatut());

        if (request.getFormationId() != null) {
            Formation formation = formationRepository.findById(request.getFormationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable : " + request.getFormationId()));
            seance.setFormation(formation);
        } else {
            seance.setFormation(null);
        }

        Seance saved = seanceRepository.save(seance);
        log.debug("Séance mise à jour: id={}", saved.getId());
        return toResponse(saved);
    }

    public void delete(Long id) {
        Seance seance = getSeance(id);
        seanceRepository.delete(seance);
        log.debug("Séance supprimée: id={}", id);
    }

    private void validateSeance(SeanceRequest request) {
        if (request.getDateHeureDebut() == null) {
            throw new BadRequestException("La date de début est requise");
        }
        if (request.getDateHeureFin() == null) {
            throw new BadRequestException("La date de fin est requise");
        }
        if (!request.getDateHeureFin().isAfter(request.getDateHeureDebut())) {
            throw new BadRequestException("La date de fin doit être après la date de début");
        }
    }

    private Seance getSeance(Long id) {
        return seanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable : " + id));
    }

    private SeanceResponse toResponse(Seance seance) {
        SeanceResponse response = new SeanceResponse();
        response.setId(seance.getId());
        response.setTitre(seance.getTitre());
        response.setDescription(seance.getDescription());
        response.setDateHeureDebut(seance.getDateHeureDebut());
        response.setDateHeureFin(seance.getDateHeureFin());
        response.setStatut(seance.getStatut());
        if (seance.getFormation() != null) {
            response.setFormationId(seance.getFormation().getId());
        }
        return response;
    }
}