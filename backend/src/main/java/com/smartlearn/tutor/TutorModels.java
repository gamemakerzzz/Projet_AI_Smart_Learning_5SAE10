package com.smartlearn.tutor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class TutorModels {

    private TutorModels() {}

    // ─── Request classes (Jackson-friendly, no records) ──────────────────────

    public static class ChatRequest {
        @NotBlank @Size(max = 4000)
        private String message;
        @jakarta.validation.constraints.Positive private Long conversationId;
        @jakarta.validation.constraints.Positive private Long formationId;
        public ChatRequest() {}
        public String getMessage() { return message; }
        public void setMessage(String m) { this.message = m; }
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long v) { this.conversationId = v; }
        public Long getFormationId() { return formationId; }
        public void setFormationId(Long v) { this.formationId = v; }
    }

    public static class ExplainRequest {
        @NotBlank @Size(max = 2000)
        private String concept;
        @Size(max=2000) private String context;
        @Size(max=80) private String level;
        public ExplainRequest() {}
        public String getConcept() { return concept; }
        public void setConcept(String v) { this.concept = v; }
        public String getContext() { return context; }
        public void setContext(String v) { this.context = v; }
        public String getLevel() { return level != null ? level : "intermédiaire"; }
        public void setLevel(String v) { this.level = v; }
    }

    public static class SummarizeRequest {
        @NotBlank @Size(max = 16000)
        private String text;
        @jakarta.validation.constraints.Pattern(regexp="fr|en|ar") private String language;
        public SummarizeRequest() {}
        public String getText() { return text; }
        public void setText(String v) { this.text = v; }
        public String getLanguage() { return language != null ? language : "fr"; }
        public void setLanguage(String v) { this.language = v; }
    }

    public static class MindMapRequest {
        @NotBlank @Size(max = 4000)
        private String topic;
        @Size(max=2000) private String context;
        @jakarta.validation.constraints.Pattern(regexp="fr|en|ar") private String language;
        public MindMapRequest() {}
        public String getTopic() { return topic; }
        public void setTopic(String v) { this.topic = v; }
        public String getContext() { return context; }
        public void setContext(String v) { this.context = v; }
        public String getLanguage() { return language != null ? language : "fr"; }
        public void setLanguage(String v) { this.language = v; }
    }

    // ─── Response records (read-only, records OK) ─────────────────────────────

    public record ConversationSummary(
        Long id, String title, Long formationId,
        int messageCount, Instant createdAt, Instant updatedAt
    ) {}

    public record MessageDto(
        Long id, String role, String content, Instant createdAt
    ) {}

    public record ConversationDetail(
        Long id, String title, Long formationId,
        List<MessageDto> messages, Instant createdAt, Instant updatedAt
    ) {}

    public record ChatResponse(
        Long conversationId, String title,
        MessageDto userMessage, MessageDto assistantMessage
    ) {}

    public record ExplainResponse(
        String explanation, String example, String analogy, String keyTakeaway
    ) {}

    public record SummarizeResponse(
        String summary, List<String> keyPoints, String estimatedReadTime
    ) {}

    public record MindMapNode(
        String label, String description, List<MindMapNode> children
    ) {}

    public record MindMapResponse(
        String topic, MindMapNode root, String language
    ) {}
}
