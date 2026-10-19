package com.smartlearn.repository;

import com.smartlearn.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByApprenantId(Long apprenantId);
    List<Notification> findByApprenantIdOrderByIdAsc(Long apprenantId);
    List<Notification> findByApprenantIdAndStatut(Long apprenantId, String statut);
    List<Notification> findByApprenantIdAndType(Long apprenantId, String type);
    long countByApprenantId(Long apprenantId);
    long countByApprenantIdAndStatut(Long apprenantId, String statut);
    long countByApprenantIdAndType(Long apprenantId, String type);
}