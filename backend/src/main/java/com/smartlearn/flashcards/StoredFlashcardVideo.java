package com.smartlearn.flashcards;

import jakarta.persistence.*;

@Entity
@Table(name = "flashcard_videos")
public class StoredFlashcardVideo {
    @Id @Column(name = "deck_id", length = 36) String id;
    @MapsId @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "deck_id") StoredFlashcardDeck deck;
    @Column(nullable = false, length = 36) String jobId;
    @Column(nullable = false, length = 20) String status;
    @Column(length = 500) String message;
    @Lob @Column(columnDefinition = "LONGTEXT") String lessonJson;
    @Lob @Column(columnDefinition = "LONGBLOB") byte[] video;
    @Lob @Column(columnDefinition = "LONGTEXT") String captions;
    double duration;
    protected StoredFlashcardVideo() {}
}
