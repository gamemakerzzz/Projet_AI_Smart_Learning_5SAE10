package com.smartlearn.tutor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TutorConversationRepository extends JpaRepository<TutorConversation, Long> {
    List<TutorConversation> findByApprenantIdOrderByUpdatedAtDesc(Long apprenantId);
    long countByApprenantId(Long apprenantId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from TutorConversation c where c.id = :id")
    java.util.Optional<TutorConversation> lock(@org.springframework.data.repository.query.Param("id") Long id);
}
