package com.smartlearn.forum;

import java.time.Instant;
import java.util.List;

public final class ForumInsightModels {
    private ForumInsightModels() {}
    public record ContextReply(Long id,String body,boolean accepted) {}
    public record ThreadContext(Long discussionId,String title,String body,Long formationId,Long acceptedReplyId,
                                Instant updatedAt,List<ContextReply> replies,long totalReplies,boolean truncated) {}
    public record SourceRef(String id,String label) {}
    public record Evidence(String text,List<SourceRef> sources) {}
    public record ThreadSummary(Evidence overview,List<Evidence> keyPoints,List<Evidence> proposedSolutions,
                                List<Evidence> unresolvedQuestions,String acceptedAnswerSummary,Long acceptedReplyId,
                                int includedReplies,long totalReplies,boolean truncated,Instant discussionUpdatedAt) {}
    public record TagSuggestions(List<String> tags,String explanation) {}
    public record CourseCandidate(Long id,String title,String description,String level,String keywords) {}
    public record LearningResource(String id,String title,String description,String url) {}
    public record ResourceCandidate(String id,String title,String description) {}
    public record LearningLink(String id,String title,String url,String explanation,boolean external) {}
    public record LearningSuggestions(List<LearningLink> courses,List<LearningLink> resources,int coursesConsidered,
                                      int resourcesConsidered,String scope,int includedReplies,long totalReplies,
                                      boolean truncated,Instant discussionUpdatedAt) {}
    public record Metadata(long durationMs,boolean cached,String model,String promptVersion) {}
    public record Insight<T>(T result,Metadata meta) {}
}
