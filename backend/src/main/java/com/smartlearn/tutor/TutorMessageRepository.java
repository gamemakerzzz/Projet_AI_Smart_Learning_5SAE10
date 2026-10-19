package com.smartlearn.tutor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TutorMessageRepository extends JpaRepository<TutorMessage, Long> {
    List<TutorMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);
    long countByConversationId(Long conversationId);
    void deleteByConversationId(Long conversationId);
}
