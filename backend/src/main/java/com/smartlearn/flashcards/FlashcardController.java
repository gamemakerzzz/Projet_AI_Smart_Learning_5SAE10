package com.smartlearn.flashcards;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Semaphore;
import static com.smartlearn.flashcards.FlashcardModels.*;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/flashcards")
public class FlashcardController {
    private final DocumentTextExtractor extractor;
    private final FlashcardService service;
    private final FlashcardAiClient ai;
    private final FlashcardStore store;
    private final FlashcardOwner owner;
    private final Semaphore slots = new Semaphore(2);
    private final Map<String, int[]> usage = new HashMap<>();
    private long hour = -1;
    private int totalCalls;
    @org.springframework.beans.factory.annotation.Value("${smartlearn.ai.rate-limit-enabled:true}")
    private boolean rateLimitEnabled = true;
    public FlashcardController(DocumentTextExtractor extractor, FlashcardService service, FlashcardAiClient ai, FlashcardStore store, FlashcardOwner owner) {
        this.extractor = extractor; this.service = service; this.ai = ai; this.store = store; this.owner = owner;
    }
    @jakarta.annotation.PostConstruct
    void logLimits() { org.slf4j.LoggerFactory.getLogger(FlashcardController.class).info("AI hourly rate limits enabled: {}", rateLimitEnabled); }
    @GetMapping("/status")
    public Map<String, Object> status() { return Map.of("configured", ai.configured(), "model", ai.model(), "connected", ai.connected(),
        "maxUnits", DocumentLimits.MAX_UNITS, "maxTextCharacters", DocumentLimits.MAX_TEXT_CHARACTERS); }

    @PostMapping(consumes = "multipart/form-data")
    public Deck generate(@RequestParam MultipartFile file, @RequestParam(defaultValue = "10") int count,
        @RequestParam(defaultValue = "fr") String language, @RequestParam(defaultValue = "beginner") String difficulty,
        @RequestParam(defaultValue = "1") int firstPage, @RequestParam(required = false) Integer lastPage,
        HttpServletRequest request, HttpServletResponse response) {
        service.validateOptions(count, language, difficulty);
        if (!ai.configured()) throw new ResponseStatusException(SERVICE_UNAVAILABLE,
            "Connectez votre compte avec Continuer avec ChatGPT pour générer vos cartes.");
        reserve(request.getRemoteAddr(), true);
        try {
            String ownerKey = owner.key(request, response);
            Document document = extractor.extract(file, firstPage, lastPage);
            Deck deck = service.generate(document, count, language, difficulty);
            return store.save(ownerKey, deck, document);
        } finally { slots.release(); }
    }
    @GetMapping
    public Deck current(HttpServletRequest request, HttpServletResponse response) { return store.latest(owner.key(request, response)); }

    @GetMapping("/decks")
    public List<FlashcardDeckRepository.Summary> library(HttpServletRequest request, HttpServletResponse response) {
        return store.list(owner.key(request, response));
    }
    @GetMapping("/decks/{deckId}")
    public Deck load(@PathVariable String deckId, HttpServletRequest request, HttpServletResponse response) {
        return store.load(owner.key(request, response), deckId);
    }
    @DeleteMapping("/decks/{deckId}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable String deckId, HttpServletRequest request, HttpServletResponse response) {
        store.delete(owner.key(request, response), deckId);
    }

    @DeleteMapping
    @ResponseStatus(NO_CONTENT)
    public void clear(HttpServletRequest request, HttpServletResponse response) { store.deleteLatest(owner.key(request, response)); }

    @PutMapping("/cards/{cardId}")
    public Card edit(@PathVariable String cardId, @Valid @RequestBody EditRequest body, HttpServletRequest request, HttpServletResponse response) {
        return store.edit(owner.key(request, response), cardId, body);
    }
    @PatchMapping("/cards/{cardId}/review")
    public Card review(@PathVariable String cardId, @RequestBody ReviewRequest body, HttpServletRequest request, HttpServletResponse response) {
        return store.review(owner.key(request, response), cardId, body.remembered());
    }
    @PostMapping("/cards/{cardId}/explain")
    public Explanation explain(@PathVariable String cardId, HttpServletRequest request, HttpServletResponse response) {
        String ownerKey = owner.key(request, response);
        var study = store.study(ownerKey, cardId);
        if (study.explanation() != null) return study.explanation();
        reserve(request.getRemoteAddr(), false);
        try {
            Explanation explanation = service.explain(study.deck(), study.card(), study.document());
            return store.explain(ownerKey, cardId, study.card(), explanation);
        } finally { slots.release(); }
    }
    private synchronized void reserve(String address, boolean generation) {
        if (!rateLimitEnabled) {
            if (!slots.tryAcquire()) throw new ResponseStatusException(TOO_MANY_REQUESTS, "Deux demandes sont déjà en cours. Réessayez dans un instant.");
            return;
        }
        long currentHour = Instant.now().getEpochSecond() / 3600;
        if (hour != currentHour) { usage.clear(); totalCalls = 0; hour = currentHour; }
        if (!usage.containsKey(address) && usage.size() >= 1000)
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "Le service est occupé. Réessayez plus tard.");
        int[] counts = usage.computeIfAbsent(address, key -> new int[2]);
        if (totalCalls >= 100 || counts[0] >= 40 || (generation && counts[1] >= 5))
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "Limite de demandes atteinte. Réessayez à l'heure suivante.");
        if (!slots.tryAcquire()) throw new ResponseStatusException(TOO_MANY_REQUESTS, "Deux demandes sont déjà en cours. Réessayez dans un instant.");
        counts[0]++; if (generation) counts[1]++; totalCalls++;
    }
}
