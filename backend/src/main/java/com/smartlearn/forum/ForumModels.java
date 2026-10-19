package com.smartlearn.forum;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class ForumModels {
    private ForumModels() {}
    public record Draft(@NotBlank @Size(min=8, max=180) String title,
                        @NotBlank @Size(min=15, max=6000) String body, @Positive Long formationId,
                        @Size(max=6) List<@NotBlank @Size(max=28) String> tags) {
        public Draft(String title, String body, Long formationId) { this(title,body,formationId,List.of()); }
    }
    public record ReplyDraft(@NotBlank @Size(min=2, max=6000) String body) {}
    public record Acceptance(@Positive Long replyId) {}
    public record Topic(Long id, Long authorId, String authorName, Long formationId, String formationTitle,
                        String title, String body, Long acceptedReplyId, long replyCount, Instant createdAt, Instant updatedAt,
                        List<String> tags) {}
    public record Detail(Topic discussion, List<Reply> replies) {}
    public record Topics(List<Topic> items, long total, int page, int totalPages) {}
    public record Candidate(Long id, String title, String body) {}
    public record Match(Long discussionId, String title, String relationship, String explanation, boolean resolved) {}
    public record Analysis(List<Match> matches, String summary, int scannedCount, long availableCount,
                           long durationMs, boolean cached, boolean aiUsed, String model, String promptVersion) {}
}
