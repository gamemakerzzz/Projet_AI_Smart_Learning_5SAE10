package com.smartlearn.flashcards;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface FlashcardDeckRepository extends JpaRepository<StoredFlashcardDeck, String> {
    Optional<StoredFlashcardDeck> findByIdAndOwnerKey(String id, String ownerKey);
    boolean existsByIdAndOwnerKey(String id, String ownerKey);
    Optional<StoredFlashcardDeck> findFirstByOwnerKeyOrderByUpdatedAtDesc(String ownerKey);
    interface Summary {
        String getId(); String getTitle(); String getFilename(); long getCardCount(); Instant getUpdatedAt();
    }
    @Query("select d.id as id, d.title as title, d.filename as filename, count(c) as cardCount, d.updatedAt as updatedAt " +
        "from StoredFlashcardDeck d left join d.cards c where d.ownerKey = :owner " +
        "group by d.id, d.title, d.filename, d.updatedAt order by d.updatedAt desc")
    List<Summary> listOwned(@Param("owner") String owner, Pageable pageable);
}
