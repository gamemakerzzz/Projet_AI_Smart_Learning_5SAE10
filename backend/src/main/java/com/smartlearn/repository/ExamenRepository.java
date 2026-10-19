
package com.smartlearn.repository;

import com.smartlearn.entity.Examen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamenRepository extends JpaRepository<Examen, Long> {

    List<Examen> findByApprenantId(Long apprenantId);

    List<Examen> findByApprenantIdOrderByIdAsc(Long apprenantId);

    List<Examen> findByApprenantIdAndFormationId(
            Long apprenantId,
            Long formationId
    );

    List<Examen> findByApprenantIdAndDateExamenAfter(
            Long apprenantId,
            java.time.LocalDateTime date
    );

    List<Examen> findByApprenantIdAndDateExamenBetween(
            Long apprenantId,
            java.time.LocalDateTime debut,
            java.time.LocalDateTime fin
    );

    long countByApprenantId(Long apprenantId);

    long countByApprenantIdAndFormationId(
            Long apprenantId,
            Long formationId
    );

    long countByApprenantIdAndFormationIsNull(Long apprenantId);

    long countByApprenantIdAndFormationIsNotNull(Long apprenantId);

    long countByApprenantIdAndCoefficient(
            Long apprenantId,
            Integer coefficient
    );

}
