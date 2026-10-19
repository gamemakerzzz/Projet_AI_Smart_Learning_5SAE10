package com.smartlearn.repository;

import com.smartlearn.entity.Formation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FormationRepository extends JpaRepository<Formation, Long> {

    @Query("SELECT f FROM Formation f JOIN FETCH f.categorie")
    List<Formation> findAllWithCategorie();

    @Query("SELECT f FROM Formation f JOIN FETCH f.categorie WHERE f.id = :id")
    Optional<Formation> findByIdWithCategorie(@Param("id") Long id);

    @Query("SELECT f FROM Formation f JOIN FETCH f.categorie WHERE LOWER(f.titre) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Formation> searchByTitre(@Param("keyword") String keyword);

    @Query("SELECT f FROM Formation f JOIN FETCH f.categorie WHERE f.categorie.id = :categorieId")
    List<Formation> findByCategorieId(@Param("categorieId") Long categorieId);

    @Query("SELECT f FROM Formation f JOIN FETCH f.categorie WHERE LOWER(f.niveau) = LOWER(:niveau)")
    List<Formation> findByNiveauIgnoreCase(@Param("niveau") String niveau);

    long countByCategorieId(Long categorieId);
}
