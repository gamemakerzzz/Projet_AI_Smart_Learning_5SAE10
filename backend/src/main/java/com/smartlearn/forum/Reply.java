package com.smartlearn.forum;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "forum_replies", indexes = @Index(name = "forum_reply_discussion", columnList = "discussionId"))
public class Reply {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long discussionId;
    @Column(nullable = false) public Long authorId;
    @Column(nullable = false) public String authorName;
    @Column(nullable = false, length = 6000) public String body;
    @Column(nullable = false) public Instant createdAt = Instant.now();
    @Column(nullable = false) public Instant updatedAt = Instant.now();
}
