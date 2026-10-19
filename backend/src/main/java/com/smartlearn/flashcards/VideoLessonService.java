package com.smartlearn.flashcards;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import java.util.concurrent.*;
import static org.springframework.http.HttpStatus.*;

@Service
public class VideoLessonService {
    private final VideoLessonPlanner planner;
    private final VideoRenderer renderer;
    private final VideoLessonStore videos;
    private final FlashcardVideoRepository repository;
    private final FlashcardStore decks;
    private final Semaphore slot = new Semaphore(1);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r, "lesson-video-renderer"); t.setDaemon(true); return t; });
    public VideoLessonService(VideoLessonPlanner planner, VideoRenderer renderer, VideoLessonStore videos,
                              FlashcardVideoRepository repository, FlashcardStore decks) {
        this.planner = planner; this.renderer = renderer; this.videos = videos; this.repository = repository; this.decks = decks;
    }
    @EventListener(ApplicationReadyEvent.class)
    public void recover() { repository.interruptUnfinished(); }
    @PreDestroy
    public void stop() { executor.shutdownNow(); }
    public VideoLesson.Status generate(String owner, String id) {
        return generate(owner, id, false);
    }
    public VideoLesson.Status generate(String owner, String id, boolean regenerate) {
        var previous = videos.status(owner, id);
        if ((previous.status().equals("ready") && !regenerate) || previous.status().equals("rendering") || previous.status().equals("writing")) return previous;
        var deck = decks.load(owner, id);
        if (!renderer.languages().contains(deck.language())) throw new ResponseStatusException(SERVICE_UNAVAILABLE,
            "La narration est indisponible pour cette langue. Vérifiez le modèle vocal et FFmpeg sur le serveur.");
        if (!slot.tryAcquire()) throw new ResponseStatusException(TOO_MANY_REQUESTS, "Une vidéo est déjà en préparation. Réessayez dans un instant.");
        String job = UUID.randomUUID().toString();
        try {
            // Keep the AI request on the visitor's request thread: OAuth credentials stay in its HTTP session.
            previous = videos.status(owner, id);
            if (!previous.status().equals("none") && !previous.status().equals("failed") && !(regenerate && previous.status().equals("ready"))) { slot.release(); return previous; }
            var cachedLesson = regenerate || previous.status().equals("failed") ? previous.lesson() : null;
            videos.begin(owner, id, job);
            var script = cachedLesson != null ? cachedLesson : planner.plan(deck, decks.document(owner, id));
            var lesson = new VideoLesson(script.title(), script.language(), script.sourceLabel(), script.scenes(), renderer.voice(script.language()));
            videos.plan(id, job, lesson);
            executor.submit(() -> {
                try {
                    var output = renderer.render(lesson, message -> repository.stage(id, job, "rendering", message));
                    videos.complete(id, job, output);
                } catch (Exception e) {
                    repository.stage(id, job, "failed", e instanceof ResponseStatusException status ? status.getReason()
                        : "La création de la vidéo a échoué. Vérifiez le modèle vocal et FFmpeg sur le serveur.");
                    org.slf4j.LoggerFactory.getLogger(getClass()).warn("Video rendering failed: {}", e.getClass().getSimpleName());
                } finally { slot.release(); }
            });
            return new VideoLesson.Status("rendering", "Préparation de la narration et des diapositives…", 0, lesson);
        } catch (RuntimeException e) {
            repository.stage(id, job, "failed", e instanceof ResponseStatusException status ? status.getReason()
                : "Le scénario n'a pas pu être préparé. Reconnectez l’IA si nécessaire et réessayez.");
            slot.release(); throw e;
        }
    }
}
