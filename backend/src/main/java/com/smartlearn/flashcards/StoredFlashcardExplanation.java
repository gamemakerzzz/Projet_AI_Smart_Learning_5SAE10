package com.smartlearn.flashcards;

import jakarta.persistence.*;

@Entity
@Table(name = "flashcard_explanations")
public class StoredFlashcardExplanation {
    @Id @Column(name = "card_id", length = 36) String id;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id") StoredFlashcard card;
    @Column(nullable = false, length = 2000) String explanation;
    @Column(nullable = false, length = 2000) String example;
    @Column(nullable = false, length = 2000) String takeaway;
    protected StoredFlashcardExplanation() {}
}
