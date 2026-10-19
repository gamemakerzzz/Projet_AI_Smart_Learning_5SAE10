package com.smartlearn.forum;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "forum_discussions", indexes = @Index(name = "forum_course_updated", columnList = "formationId,updatedAt"))
public class Discussion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long authorId;
    @Column(nullable = false) public String authorName;
    public Long formationId;
    public String formationTitle;
    @Column(nullable = false, length = 180) public String title;
    @Column(nullable = false, length = 6000) public String body;
    @Column(length = 180) public String tags = "";
    public Long acceptedReplyId;
    @Column(nullable = false) public Instant createdAt = Instant.now();
    @Column(nullable = false) public Instant updatedAt = Instant.now();
}
