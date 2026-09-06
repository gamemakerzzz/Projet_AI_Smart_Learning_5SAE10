package com.smartlearn.repository;

import com.smartlearn.entity.Apprenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApprenantRepository extends JpaRepository<Apprenant, Long> {

    long countByRole(com.smartlearn.entity.Role role);
    long countByRoleAndProfilIsNotNull(com.smartlearn.entity.Role role);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Apprenant> findByEmailIgnoreCase(String email);

    @Query("SELECT DISTINCT a FROM Apprenant a LEFT JOIN FETCH a.profil")
    List<Apprenant> findAllWithProfil();

    @Query("SELECT a FROM Apprenant a LEFT JOIN FETCH a.profil WHERE a.id = :id")
    Optional<Apprenant> findByIdWithProfil(@Param("id") Long id);
}
