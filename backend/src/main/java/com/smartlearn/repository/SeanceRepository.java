package com.smartlearn.repository;

import com.smartlearn.entity.Seance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeanceRepository extends JpaRepository<Seance, Long> {
    List<Seance> findByApprenantId(Long apprenantId);
    List<Seance> findByApprenantIdOrderByIdAsc(Long apprenantId);
    List<Seance> findByApprenantIdAndFormationId(Long apprenantId, Long formationId);
    List<Seance> findByApprenantIdAndDateHeureDebutBetween(Long apprenantId, java.time.LocalDateTime debut, java.time.LocalDateTime fin);
    long countByApprenantId(Long apprenantId);
    long countByApprenantIdAndFormationId(Long apprenantId, Long formationId);
    long countByApprenantIdAndFormationIsNull(Long apprenantId);
    long countByApprenantIdAndFormationIsNotNull(Long apprenantId);
    long countByApprenantIdAndStatut(Long apprenantId, String statut);
}