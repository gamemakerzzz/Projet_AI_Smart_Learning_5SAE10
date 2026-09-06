package com.smartlearn.repository;

import com.smartlearn.entity.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    boolean existsByNomIgnoreCase(String nom);

    Optional<Categorie> findByNomIgnoreCase(String nom);
}
