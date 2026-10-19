package com.smartlearn.forum;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReplyRepository extends JpaRepository<Reply, Long> {
    List<Reply> findByDiscussionIdOrderByCreatedAtAscIdAsc(Long discussionId);
    List<Reply> findByDiscussionId(Long discussionId, org.springframework.data.domain.Pageable pageable);
    long countByDiscussionId(Long discussionId);
    void deleteByDiscussionId(Long discussionId);
}
