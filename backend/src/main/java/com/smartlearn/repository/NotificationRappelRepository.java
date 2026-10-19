package com.smartlearn.repository;

import com.smartlearn.entity.NotificationRappel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRappelRepository extends JpaRepository<NotificationRappel, Long> {
    boolean existsByTypeRappelAndCibleTypeAndCibleIdAndApprenantId(
            String typeRappel, String cibleType, Long cibleId, Long apprenantId);
    
    List<NotificationRappel> findByCibleTypeAndCibleId(String cibleType, Long cibleId);
    
    void deleteByCibleTypeAndCibleId(String cibleType, Long cibleId);
}