package com.smartlearn.flashcards;

import java.util.List;

public final class FlashcardModels {
    private FlashcardModels() {}
    public record Page(int number, String text) {}
    public record Document(String filename, int totalPages, List<Page> pages, String sourceLabel) {
        public Document(String filename, int totalPages, List<Page> pages) { this(filename, totalPages, pages, "Page"); }
    }
    public record Card(String id, String question, String answer, int page, String source, boolean remembered) {
        public Card(String id, String question, String answer, int page, String source) { this(id, question, answer, page, source, false); }
    }
    public record ReviewRequest(boolean remembered) {}
    public record Deck(String id, String title, String filename, String language, String difficulty,
                       int totalPages, List<Card> cards, String sourceLabel) {
        public Deck(String id, String title, String filename, String language, String difficulty, int totalPages, List<Card> cards) {
            this(id, title, filename, language, difficulty, totalPages, cards, "Page");
        }
    }
    public record Explanation(String explanation, String example, String takeaway) {}
    public record EditRequest(
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 1000) String question,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 3000) String answer) {}
}
