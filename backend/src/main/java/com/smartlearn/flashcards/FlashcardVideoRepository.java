package com.smartlearn.flashcards;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface FlashcardVideoRepository extends JpaRepository<StoredFlashcardVideo, String> {
    Optional<StoredFlashcardVideo> findByIdAndDeckOwnerKey(String id, String owner);
    interface Metadata {
        String getStatus(); String getMessage(); String getLessonJson(); double getDuration();
    }
    @Query("select v.status as status, v.message as message, v.lessonJson as lessonJson, v.duration as duration " +
        "from StoredFlashcardVideo v where v.id = :id and v.deck.ownerKey = :owner")
    Optional<Metadata> metadata(@Param("id") String id, @Param("owner") String owner);
    @Modifying @Transactional
    @Query("update StoredFlashcardVideo v set v.status = :status, v.message = :message where v.id = :id and v.jobId = :job")
    int stage(@Param("id") String id, @Param("job") String job, @Param("status") String status, @Param("message") String message);
    @Modifying @Transactional
    @Query("update StoredFlashcardVideo v set v.status = 'failed', v.message = 'Génération interrompue par le redémarrage. Relancez la vidéo.' where v.status in ('writing', 'rendering')")
    int interruptUnfinished();
}
