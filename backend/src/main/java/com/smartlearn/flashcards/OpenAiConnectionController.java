package com.smartlearn.flashcards;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/flashcards/openai")
public class OpenAiConnectionController {
    private final OpenAiConnection connection;
    public OpenAiConnectionController(OpenAiConnection connection) { this.connection = connection; }
    @PostMapping("/connect")
    public Map<String, String> connect(HttpSession session) { return Map.of("url", connection.start(session)); }
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(HttpSession session) { connection.disconnect(session); }
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String state, @RequestParam(required = false) String code,
        @RequestParam(name = "client_id", required = false) String clientId, @RequestParam(required = false) String error) {
        String result;
        try { connection.complete(state, code, clientId, error); result = "connected"; }
        catch (org.springframework.web.server.ResponseStatusException e) { result = error != null ? "cancelled" : "failed"; }
        return ResponseEntity.status(HttpStatus.SEE_OTHER).cacheControl(CacheControl.noStore()).header("Referrer-Policy", "no-referrer")
            .location(URI.create("http://localhost:4200/#/flashcards?openai=" + result)).build();
    }
}
