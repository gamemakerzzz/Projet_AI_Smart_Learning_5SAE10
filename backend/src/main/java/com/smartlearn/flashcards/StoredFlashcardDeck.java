package com.smartlearn.flashcards;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "flashcard_decks", indexes = @Index(name = "idx_flashcard_deck_owner", columnList = "owner_key,updated_at"))
public class StoredFlashcardDeck {
    @Id @Column(length = 36) String id;
    @Column(name = "owner_key", nullable = false, length = 64) String ownerKey;
    @Column(nullable = false, length = 200) String title;
    @Column(nullable = false, length = 180) String filename;
    @Column(nullable = false, length = 8) String language;
    @Column(nullable = false, length = 20) String difficulty;
    @Column(nullable = false) int totalPages;
    @Column(nullable = false, length = 20) String sourceLabel;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") String sourceJson;
    @Column(nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") List<StoredFlashcard> cards = new ArrayList<>();
    @OneToOne(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    StoredFlashcardVideo video;
    protected StoredFlashcardDeck() {}
}
