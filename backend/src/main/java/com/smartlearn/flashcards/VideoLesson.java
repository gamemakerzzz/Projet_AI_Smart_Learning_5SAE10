package com.smartlearn.flashcards;

import java.util.List;

public record VideoLesson(String title, String language, String sourceLabel, List<Scene> scenes, String narrator) {
    public VideoLesson(String title, String language, String sourceLabel, List<Scene> scenes) { this(title, language, sourceLabel, scenes, null); }
    public record Scene(String title, List<String> bullets, String example, String narration, int page, String source) {}
    public record Status(String status, String message, double duration, VideoLesson lesson) {}
    public record Rendered(byte[] video, String captions, double duration) {}
}
