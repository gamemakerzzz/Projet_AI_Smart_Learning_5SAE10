package com.smartlearn.forum;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface DiscussionRepository extends JpaRepository<Discussion, Long> {
    @Query("select d from Discussion d where (:course is null or d.formationId = :course) " +
        "and (:state = 'all' or (:state = 'resolved' and d.acceptedReplyId is not null) or (:state = 'open' and d.acceptedReplyId is null)) " +
        "and (:search = '' or locate(:search, lower(d.title)) > 0 or locate(:search, lower(d.body)) > 0 or locate(:search, lower(coalesce(d.tags, ''))) > 0)")
    Page<Discussion> browse(@Param("course") Long course, @Param("state") String state, @Param("search") String search, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Discussion d where d.id = :id")
    Optional<Discussion> lock(@Param("id") Long id);
}
