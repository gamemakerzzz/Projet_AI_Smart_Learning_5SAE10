package com.smartlearn.repository;

import com.smartlearn.entity.Chapitre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapitreRepository extends JpaRepository<Chapitre, Long> {

    List<Chapitre> findByFormationIdOrderByOrdreAsc(Long formationId);
}
