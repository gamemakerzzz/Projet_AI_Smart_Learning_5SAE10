package com.smartlearn.tutor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TutorConversationRepository extends JpaRepository<TutorConversation, Long> {
    List<TutorConversation> findByApprenantIdOrderByUpdatedAtDesc(Long apprenantId);
    long countByApprenantId(Long apprenantId);
}
