package com.smartlearn.flashcards;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface FlashcardRepository extends JpaRepository<StoredFlashcard, String> {
    Optional<StoredFlashcard> findByIdAndDeckOwnerKey(String id, String ownerKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from StoredFlashcard c where c.id = :id and c.deck.ownerKey = :owner")
    Optional<StoredFlashcard> lockOwned(@Param("id") String id, @Param("owner") String owner);
}
