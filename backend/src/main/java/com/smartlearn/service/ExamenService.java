package com.smartlearn.service;

import com.smartlearn.dto.ExamenRequest;
import com.smartlearn.dto.ExamenResponse;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExamenService {

    private static final Logger log = LoggerFactory.getLogger(ExamenService.class);

    private final ExamenRepository examenRepository;
    private final ApprenantRepository apprenantRepository;
    private final FormationRepository formationRepository;

    public ExamenService(ExamenRepository examenRepository,
                         ApprenantRepository apprenantRepository,
                         FormationRepository formationRepository) {
        this.examenRepository = examenRepository;
        this.apprenantRepository = apprenantRepository;
        this.formationRepository = formationRepository;
    }

    public List<ExamenResponse> getAllByApprenant(Long apprenantId) {
        return examenRepository.findByApprenantIdOrderByIdAsc(apprenantId).stream()
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
        return toResponse(saved);
    }

    @Transactional
    public ExamenResponse update(Long id, ExamenRequest request) {
        Examen examen = examenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen introuvable : " + id));

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
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!examenRepository.existsById(id)) {
            throw new ResourceNotFoundException("Examen introuvable : " + id);
        }
              examenRepository.deleteById(id);        log.debug("Examen supprimé: id={}", id);
    }

    private ExamenResponse toResponse(Examen examen) {
        ExamenResponse response = new ExamenResponse();
        response.setId(examen.getId());
        response.setTitre(examen.getTitre());
        response.setDescription(examen.getDescription());
        response.setDateExamen(examen.getDateExamen());
        response.setDureeMinutes(examen.getDureeMinutes());
        response.setCoefficient(examen.getCoefficient());
        if (examen.getFormation() != null) {
            response.setFormationId(examen.getFormation().getId());
        }
        return response;
    }
}