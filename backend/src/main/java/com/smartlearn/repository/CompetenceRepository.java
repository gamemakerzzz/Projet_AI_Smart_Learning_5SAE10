package com.smartlearn.repository;

import com.smartlearn.entity.Competence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetenceRepository extends JpaRepository<Competence, Long> {
    long countByApprenantRole(com.smartlearn.entity.Role role);


    List<Competence> findByApprenantId(Long apprenantId);
}
