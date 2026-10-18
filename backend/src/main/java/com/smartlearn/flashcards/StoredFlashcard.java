package com.smartlearn.flashcards;

import jakarta.persistence.*;

@Entity
@Table(name = "flashcards")
public class StoredFlashcard {
    @Id @Column(length = 36) String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false) StoredFlashcardDeck deck;
    @Column(name = "card_order", nullable = false) int position;
    @Column(nullable = false, length = 1000) String question;
    @Column(nullable = false, length = 3000) String answer;
    @Column(nullable = false) int sourceNumber;
    @Column(nullable = false, length = 500) String sourceQuote;
    @Column(nullable = false) boolean remembered;
    @OneToOne(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    StoredFlashcardExplanation explanation;
    protected StoredFlashcard() {}
}
