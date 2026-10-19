package com.smartlearn.flashcards;

import jakarta.servlet.http.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/flashcards")
public class VideoLessonController {
    private final VideoLessonService service;
    private final VideoLessonStore store;
    private final VideoRenderer renderer;
    private final FlashcardOwner owner;
    public VideoLessonController(VideoLessonService service, VideoLessonStore store, VideoRenderer renderer, FlashcardOwner owner) {
        this.service = service; this.store = store; this.renderer = renderer; this.owner = owner;
    }
    @GetMapping("/video/capabilities")
    public Map<String, Object> capabilities() {
        var languages = renderer.languages();
        return Map.of("available", !languages.isEmpty(), "languages", languages,
            "message", renderer.readinessMessage(), "narrationLocal", renderer.localNarration());
    }
    @GetMapping("/decks/{id}/video")
    public VideoLesson.Status status(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
        return store.status(owner.key(request, response), id);
    }
    @PostMapping("/decks/{id}/video")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public VideoLesson.Status generate(@PathVariable String id, @RequestParam(defaultValue = "false") boolean regenerate,
                                      HttpServletRequest request, HttpServletResponse response) {
        return service.generate(owner.key(request, response), id, regenerate);
    }
    @GetMapping("/decks/{id}/video/media")
    public ResponseEntity<Resource> media(@PathVariable String id, @RequestParam(defaultValue = "false") boolean download,
                                         HttpServletRequest request, HttpServletResponse response) {
        var content = store.media(owner.key(request, response), id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.valueOf("video/mp4"))
            .header(HttpHeaders.CONTENT_DISPOSITION, (download ? "attachment" : "inline") + "; filename=\"smartlearn-lesson.mp4\"")
            .body(new ByteArrayResource(content.video()));
    }
    @GetMapping("/decks/{id}/video/captions")
    public ResponseEntity<Resource> captions(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.valueOf("text/vtt;charset=UTF-8"))
            .body(new ByteArrayResource(store.media(owner.key(request, response), id).captions().getBytes(StandardCharsets.UTF_8)));
    }
}
