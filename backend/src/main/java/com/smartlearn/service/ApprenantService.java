package com.smartlearn.service;

import com.smartlearn.dto.ApprenantRequest;
import com.smartlearn.dto.ApprenantResponse;
import com.smartlearn.dto.ApprenantUpdateRequest;
import com.smartlearn.dto.CompetenceRequest;
import com.smartlearn.dto.CompetenceResponse;
import com.smartlearn.dto.EntityMapper;
import com.smartlearn.dto.ProfilRequest;
import com.smartlearn.dto.ProfilResponse;
import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Competence;
import com.smartlearn.entity.Profil;
import com.smartlearn.exception.DuplicateResourceException;
import com.smartlearn.exception.ResourceNotFoundException;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.CompetenceRepository;
import com.smartlearn.repository.ProfilRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ApprenantService {

    private final ApprenantRepository apprenantRepository;
    private final ProfilRepository profilRepository;
    private final CompetenceRepository competenceRepository;
    private final PasswordEncoder passwordEncoder;

    public ApprenantService(ApprenantRepository apprenantRepository,
                            ProfilRepository profilRepository,
                            CompetenceRepository competenceRepository,
                            PasswordEncoder passwordEncoder) {
        this.apprenantRepository = apprenantRepository;
        this.profilRepository = profilRepository;
        this.competenceRepository = competenceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<ApprenantResponse> findAll() {
        return apprenantRepository.findAllWithProfil().stream()
                .filter(a -> a.getRole() == com.smartlearn.entity.Role.USER)
                .map(EntityMapper::toApprenantResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprenantResponse findById(Long id) {
        return EntityMapper.toApprenantResponse(getApprenant(id));
    }

    public ApprenantResponse create(ApprenantRequest request) {
        if (apprenantRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("Un apprenant avec cet email existe déjà");
        }
        Apprenant apprenant = new Apprenant();
        apprenant.setNom(request.getNom());
        apprenant.setPrenom(request.getPrenom());
        apprenant.setEmail(request.getEmail());
        apprenant.setPassword(passwordEncoder.encode(request.getPassword()));
        return EntityMapper.toApprenantResponse(apprenantRepository.save(apprenant));
    }

    public ApprenantResponse update(Long id, ApprenantUpdateRequest request) {
        Apprenant apprenant = getApprenant(id);
        apprenantRepository.findByEmailIgnoreCase(request.getEmail())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Un apprenant avec cet email existe déjà");
                });
        apprenant.setNom(request.getNom());
        apprenant.setPrenom(request.getPrenom());
        apprenant.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            apprenant.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return EntityMapper.toApprenantResponse(apprenantRepository.save(apprenant));
    }

    public void delete(Long id) {
        Apprenant apprenant = getApprenant(id);
        apprenantRepository.delete(apprenant);
    }

    @Transactional(readOnly = true)
    public ProfilResponse getProfil(Long apprenantId) {
        getApprenant(apprenantId);
        Profil profil = profilRepository.findByApprenantId(apprenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable pour l'apprenant : " + apprenantId));
        return EntityMapper.toProfilResponse(profil);
    }

    public ProfilResponse upsertProfil(Long apprenantId, ProfilRequest request) {
        Apprenant apprenant = getApprenant(apprenantId);
        Profil profil = profilRepository.findByApprenantId(apprenantId).orElseGet(() -> {
            Profil created = new Profil();
            created.setApprenant(apprenant);
            apprenant.setProfil(created);
            return created;
        });
        profil.setNiveau(request.getNiveau());
        profil.setObjectif(request.getObjectif());
        profil.setDomaineInteret(request.getDomaineInteret());
        return EntityMapper.toProfilResponse(profilRepository.save(profil));
    }

    @Transactional(readOnly = true)
    public List<CompetenceResponse> findCompetences(Long apprenantId) {
        getApprenant(apprenantId);
        return competenceRepository.findByApprenantId(apprenantId).stream()
                .map(EntityMapper::toCompetenceResponse)
                .toList();
    }

    public CompetenceResponse addCompetence(Long apprenantId, CompetenceRequest request) {
        Apprenant apprenant = getApprenant(apprenantId);
        Competence competence = new Competence();
        competence.setNom(request.getNom());
        competence.setNiveau(request.getNiveau());
        competence.setApprenant(apprenant);
        return EntityMapper.toCompetenceResponse(competenceRepository.save(competence));
    }

    public CompetenceResponse updateCompetence(Long competenceId, CompetenceRequest request) {
        Competence competence = competenceRepository.findById(competenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Compétence introuvable : " + competenceId));
        competence.setNom(request.getNom());
        competence.setNiveau(request.getNiveau());
        return EntityMapper.toCompetenceResponse(competenceRepository.save(competence));
    }

    public void deleteCompetence(Long competenceId) {
        Competence competence = competenceRepository.findById(competenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Compétence introuvable : " + competenceId));
        competenceRepository.delete(competence);
    }

    public Apprenant getApprenant(Long id) {
        return apprenantRepository.findByIdWithProfil(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant introuvable : " + id));
    }
}
