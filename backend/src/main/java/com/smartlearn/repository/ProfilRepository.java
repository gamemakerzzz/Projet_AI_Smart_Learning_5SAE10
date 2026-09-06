package com.smartlearn.repository;

import com.smartlearn.entity.Profil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfilRepository extends JpaRepository<Profil, Long> {

    Optional<Profil> findByApprenantId(Long apprenantId);
}
