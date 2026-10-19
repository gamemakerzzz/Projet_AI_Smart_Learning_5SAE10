package com.smartlearn.service;

import com.smartlearn.dto.ChapitreRequest;
import com.smartlearn.dto.ChapitreResponse;
import com.smartlearn.dto.EntityMapper;
import com.smartlearn.dto.FormationRequest;
import com.smartlearn.dto.FormationResponse;
import com.smartlearn.entity.Categorie;
import com.smartlearn.entity.Chapitre;
import com.smartlearn.entity.Formation;
import com.smartlearn.exception.ResourceNotFoundException;
import com.smartlearn.repository.ChapitreRepository;
import com.smartlearn.repository.FormationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FormationService {

    private final FormationRepository formationRepository;
    private final ChapitreRepository chapitreRepository;
    private final CategorieService categorieService;

    public FormationService(FormationRepository formationRepository,
                            ChapitreRepository chapitreRepository,
                            CategorieService categorieService) {
        this.formationRepository = formationRepository;
        this.chapitreRepository = chapitreRepository;
        this.categorieService = categorieService;
    }

    @Transactional(readOnly = true)
    public List<FormationResponse> findAll(String niveau) {
        List<Formation> formations = (niveau == null || niveau.isBlank())
                ? formationRepository.findAllWithCategorie()
                : formationRepository.findByNiveauIgnoreCase(niveau);
        return formations.stream().map(EntityMapper::toFormationResponse).toList();
    }

    @Transactional(readOnly = true)
    public FormationResponse findById(Long id) {
        return EntityMapper.toFormationResponse(getFormation(id));
    }

    @Transactional(readOnly = true)
    public List<FormationResponse> searchByTitre(String keyword) {
        return formationRepository.searchByTitre(keyword == null ? "" : keyword).stream()
                .map(EntityMapper::toFormationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FormationResponse> findByCategorie(Long categorieId) {
        categorieService.getCategorie(categorieId);
        return formationRepository.findByCategorieId(categorieId).stream()
                .map(EntityMapper::toFormationResponse)
                .toList();
    }

    public FormationResponse create(FormationRequest request) {
        Formation formation = new Formation();
        apply(formation, request);
        return EntityMapper.toFormationResponse(formationRepository.save(formation));
    }

    public FormationResponse update(Long id, FormationRequest request) {
        Formation formation = getFormation(id);
        apply(formation, request);
        return EntityMapper.toFormationResponse(formationRepository.save(formation));
    }

    public void delete(Long id) {
        Formation formation = getFormation(id);
        formationRepository.delete(formation);
    }

    @Transactional(readOnly = true)
    public List<ChapitreResponse> findChapitres(Long formationId) {
        getFormation(formationId);
        return chapitreRepository.findByFormationIdOrderByOrdreAsc(formationId).stream()
                .map(EntityMapper::toChapitreResponse)
                .toList();
    }

    public ChapitreResponse addChapitre(Long formationId, ChapitreRequest request) {
        Formation formation = getFormation(formationId);
        Chapitre chapitre = new Chapitre();
        chapitre.setTitre(request.getTitre());
        chapitre.setContenu(request.getContenu());
        chapitre.setOrdre(request.getOrdre());
        chapitre.setFormation(formation);
        return EntityMapper.toChapitreResponse(chapitreRepository.save(chapitre));
    }

    public ChapitreResponse updateChapitre(Long chapitreId, ChapitreRequest request) {
        Chapitre chapitre = chapitreRepository.findById(chapitreId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapitre introuvable : " + chapitreId));
        chapitre.setTitre(request.getTitre());
        chapitre.setContenu(request.getContenu());
        chapitre.setOrdre(request.getOrdre());
        return EntityMapper.toChapitreResponse(chapitreRepository.save(chapitre));
    }

    public void deleteChapitre(Long chapitreId) {
        Chapitre chapitre = chapitreRepository.findById(chapitreId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapitre introuvable : " + chapitreId));
        chapitreRepository.delete(chapitre);
    }

    public Formation getFormation(Long id) {
        return formationRepository.findByIdWithCategorie(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable : " + id));
    }

    private void apply(Formation formation, FormationRequest request) {
        Categorie categorie = categorieService.getCategorie(request.getCategorieId());
        formation.setTitre(request.getTitre());
        formation.setDescription(request.getDescription());
        formation.setPrix(request.getPrix());
        formation.setNiveau(request.getNiveau());
        formation.setMotsCles(request.getMotsCles());
        formation.setCategorie(categorie);
    }
}
