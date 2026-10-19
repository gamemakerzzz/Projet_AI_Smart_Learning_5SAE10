package com.smartlearn.tutor;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tutor_conversations")
public class TutorConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private long version;

    @Column(nullable = false)
    private Long apprenantId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    /** Context: null = général, non-null = lié à une formation */
    @Column
    private Long formationId;

    protected TutorConversation() {}

    public TutorConversation(Long apprenantId, String title, Long formationId) {
        this.apprenantId = apprenantId;
        this.title = title;
        this.formationId = formationId;
    }

    public Long getId() { return id; }
    public Long getApprenantId() { return apprenantId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; this.updatedAt = Instant.now(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getFormationId() { return formationId; }
}
