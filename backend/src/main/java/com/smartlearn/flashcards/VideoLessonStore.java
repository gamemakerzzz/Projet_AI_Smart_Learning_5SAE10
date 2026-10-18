package com.smartlearn.flashcards;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
@Transactional
public class VideoLessonStore {
    private final FlashcardVideoRepository videos;
    private final FlashcardDeckRepository decks;
    private final ObjectMapper mapper;
    public VideoLessonStore(FlashcardVideoRepository videos, FlashcardDeckRepository decks, ObjectMapper mapper) {
        this.videos = videos; this.decks = decks; this.mapper = mapper;
    }
    @Transactional(readOnly = true)
    public VideoLesson.Status status(String owner, String id) {
        if (!decks.existsByIdAndOwnerKey(id, owner)) throw missing();
        return videos.metadata(id, owner).map(this::view)
            .orElse(new VideoLesson.Status("none", "", 0, null));
    }
    public void begin(String owner, String id, String job) {
        var deck = decks.findByIdAndOwnerKey(id, owner).orElseThrow(this::missing);
        var value = videos.findById(id).orElseGet(StoredFlashcardVideo::new);
        value.deck = deck; value.jobId = job; value.status = "writing"; value.message = "L’IA prépare le scénario…";
        value.lessonJson = null; value.video = null; value.captions = null; value.duration = 0;
        videos.save(value);
    }
    public void plan(String id, String job, VideoLesson lesson) {
        var value = videos.findById(id).orElseThrow(this::missing);
        if (!value.jobId.equals(job)) throw missing();
        try { value.lessonJson = mapper.writeValueAsString(lesson); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
        value.status = "rendering"; value.message = "Préparation de la narration et des diapositives…";
    }
    public void complete(String id, String job, VideoLesson.Rendered rendered) {
        var value = videos.findById(id).orElse(null);
        if (value == null || !value.jobId.equals(job)) return;
        value.video = rendered.video(); value.captions = rendered.captions(); value.duration = rendered.duration();
        value.status = "ready"; value.message = "Votre leçon vidéo est prête.";
    }
    @Transactional(readOnly = true)
    public VideoLesson.Rendered media(String owner, String id) {
        var value = videos.findByIdAndDeckOwnerKey(id, owner).orElseThrow(this::missing);
        if (!"ready".equals(value.status)) throw new ResponseStatusException(CONFLICT, "La vidéo n'est pas encore prête.");
        return new VideoLesson.Rendered(value.video, value.captions, value.duration);
    }
    private VideoLesson.Status view(FlashcardVideoRepository.Metadata value) {
        try { return new VideoLesson.Status(value.getStatus(), value.getMessage(), value.getDuration(),
            value.getLessonJson() == null ? null : mapper.readValue(value.getLessonJson(), VideoLesson.class)); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
    private ResponseStatusException missing() { return new ResponseStatusException(NOT_FOUND, "Vidéo ou paquet introuvable dans votre bibliothèque."); }
}
