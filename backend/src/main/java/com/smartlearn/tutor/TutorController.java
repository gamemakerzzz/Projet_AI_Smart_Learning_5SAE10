package com.smartlearn.tutor;

import com.smartlearn.entity.Apprenant;
import com.smartlearn.repository.ApprenantRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static com.smartlearn.tutor.TutorModels.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/tutor")
public class TutorController {

    private final TutorService service;
    private final ApprenantRepository apprenantRepo;

    public TutorController(TutorService service, ApprenantRepository apprenantRepo) {
        this.service = service;
        this.apprenantRepo = apprenantRepo;
    }

    // ─── Status ─────────────────────────────────────────────────────────────────

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "configured", service.configured(),
            "model", service.model(),
            "features", List.of("chat", "explain", "summarize", "mindmap", "history")
        );
    }

    // ─── Chat ───────────────────────────────────────────────────────────────────

    @PostMapping("/chat")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatResponse chat(@Valid @RequestBody ChatRequest req, Authentication auth) {
        return service.chat(req, actor(auth).getId());
    }

    // ─── Explain ────────────────────────────────────────────────────────────────

    @PostMapping("/explain")
    public ExplainResponse explain(@Valid @RequestBody ExplainRequest req) {
        return service.explain(req);
    }

    // ─── Summarize ──────────────────────────────────────────────────────────────

    @PostMapping("/summarize")
    public SummarizeResponse summarize(@Valid @RequestBody SummarizeRequest req) {
        return service.summarize(req);
    }

    // ─── Mind Map ───────────────────────────────────────────────────────────────

    @PostMapping("/mindmap")
    public MindMapResponse mindMap(@Valid @RequestBody MindMapRequest req) {
        return service.mindMap(req);
    }

    // ─── History ────────────────────────────────────────────────────────────────

    @GetMapping("/conversations")
    public List<ConversationSummary> listConversations(Authentication auth) {
        return service.listConversations(actor(auth).getId());
    }

    @GetMapping("/conversations/{id}")
    public ConversationDetail getConversation(@PathVariable Long id, Authentication auth) {
        return service.getConversation(id, actor(auth).getId());
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable Long id, Authentication auth) {
        service.deleteConversation(id, actor(auth).getId());
    }

    // ─── Private ────────────────────────────────────────────────────────────────

    private Apprenant actor(Authentication auth) {
        return apprenantRepo.findByEmailIgnoreCase(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Utilisateur introuvable."));
    }
}
