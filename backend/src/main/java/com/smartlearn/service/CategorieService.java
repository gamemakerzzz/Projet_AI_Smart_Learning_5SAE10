package com.smartlearn.service;

import com.smartlearn.dto.CategorieRequest;
import com.smartlearn.dto.CategorieResponse;
import com.smartlearn.dto.EntityMapper;
import com.smartlearn.entity.Categorie;
import com.smartlearn.exception.BadRequestException;
import com.smartlearn.exception.DuplicateResourceException;
import com.smartlearn.exception.ResourceNotFoundException;
import com.smartlearn.repository.CategorieRepository;
import com.smartlearn.repository.FormationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final FormationRepository formationRepository;

    public CategorieService(CategorieRepository categorieRepository, FormationRepository formationRepository) {
        this.categorieRepository = categorieRepository;
        this.formationRepository = formationRepository;
    }

    @Transactional(readOnly = true)
    public List<CategorieResponse> findAll() {
        return categorieRepository.findAll().stream()
                .map(EntityMapper::toCategorieResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategorieResponse findById(Long id) {
        return EntityMapper.toCategorieResponse(getCategorie(id));
    }

    public CategorieResponse create(CategorieRequest request) {
        if (categorieRepository.existsByNomIgnoreCase(request.getNom())) {
            throw new DuplicateResourceException("Une catégorie avec ce nom existe déjà");
        }
        Categorie categorie = new Categorie();
        categorie.setNom(request.getNom());
        categorie.setDescription(request.getDescription());
        return EntityMapper.toCategorieResponse(categorieRepository.save(categorie));
    }

    public CategorieResponse update(Long id, CategorieRequest request) {
        Categorie categorie = getCategorie(id);
        categorieRepository.findByNomIgnoreCase(request.getNom())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Une catégorie avec ce nom existe déjà");
                });
        categorie.setNom(request.getNom());
        categorie.setDescription(request.getDescription());
        return EntityMapper.toCategorieResponse(categorieRepository.save(categorie));
    }

    public void delete(Long id) {
        Categorie categorie = getCategorie(id);
        if (formationRepository.countByCategorieId(id) > 0) {
            throw new BadRequestException("Impossible de supprimer une catégorie qui contient des formations");
        }
        categorieRepository.delete(categorie);
    }

    public Categorie getCategorie(Long id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + id));
    }
}
