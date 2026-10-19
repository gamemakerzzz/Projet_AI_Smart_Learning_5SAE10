package com.smartlearn.tutor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

import static org.springframework.http.HttpStatus.*;

/**
 * Client AI pour le Tutor — utilise le format Chat Completions (/v1/chat/completions).
 * Compatible avec : Groq, OpenAI, OpenRouter, Gemini (via proxy OpenAI).
 * Fonctionne en parallèle de FlashcardAiClient (Responses API).
 */
@Service
public class TutorAiClient {

    private static final Logger log = LoggerFactory.getLogger(TutorAiClient.class);

    private final ObjectMapper mapper;
    private final String key;
    private final String keyFile;
    private final String model;
    private final String url;
    private final HttpClient http;

    public TutorAiClient(
            ObjectMapper mapper,
            @Value("${smartlearn.ai.api-key:}") String key,
            @Value("${smartlearn.ai.key-file:.local/openai-api-key.txt}") String keyFile,
            @Value("${smartlearn.tutor.ai.model:${smartlearn.ai.model:gpt-4o-mini}}") String model,
            @Value("${smartlearn.tutor.ai.url:https://api.groq.com/openai/v1/chat/completions}") String url) {
        this.mapper = mapper;
        this.key = key;
        this.keyFile = keyFile;
        this.model = model;
        this.url = url;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private String apiKey() {
        if (!key.isBlank()) return key.strip();
        try { return Files.readString(Path.of(keyFile)).strip(); } catch (Exception e) { return ""; }
    }

    public boolean configured() { return !apiKey().isBlank(); }
    public String model() { return model; }

    /**
     * Appel AI avec prompt système + message utilisateur.
     * Retourne le texte brut de la réponse.
     */
    public String chat(String systemPrompt, String userMessage) {
        String credential = apiKey();
        if (credential.isBlank()) throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                "L'IA n'est pas configuree. Ajoutez votre cle API dans .local/openai-api-key.txt");

        try {
            // Construire le body JSON Chat Completions
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.7);
            body.put("max_tokens", 3000);

            ArrayNode messages = mapper.createArrayNode();
            messages.add(mapper.createObjectNode()
                    .put("role", "system").put("content", systemPrompt));
            messages.add(mapper.createObjectNode()
                    .put("role", "user").put("content", userMessage));
            body.set("messages", messages);

            // Requête HTTP
            var request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + credential)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());

            return handleResponse(response);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT,
                    "L'IA met trop de temps a repondre. Reessayez.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Requete IA interrompue.");
        } catch (Exception e) {
            log.error("TutorAiClient error: {}", e.getMessage());
            throw new ResponseStatusException(BAD_GATEWAY,
                    "Connexion au service IA impossible. Reessayez.");
        }
    }

    /**
     * Appel AI multi-tour (historique de conversation).
     */
    public String chatMultiTurn(String systemPrompt, List<Map<String, String>> history, String userMessage) {
        String credential = apiKey();
        if (credential.isBlank()) throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                "L'IA n'est pas configuree. Ajoutez votre cle API dans .local/openai-api-key.txt");

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.7);
            body.put("max_tokens", 3000);

            ArrayNode messages = mapper.createArrayNode();
            // Système
            messages.add(mapper.createObjectNode()
                    .put("role", "system").put("content", systemPrompt));
            // Historique
            for (var msg : history) {
                messages.add(mapper.createObjectNode()
                        .put("role", msg.getOrDefault("role", "user"))
                        .put("content", msg.getOrDefault("content", "")));
            }
            // Message actuel
            messages.add(mapper.createObjectNode()
                    .put("role", "user").put("content", userMessage));
            body.set("messages", messages);

            var request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + credential)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT, "L'IA met trop de temps a repondre.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Requete IA interrompue.");
        } catch (Exception e) {
            log.error("TutorAiClient chatMultiTurn error: {}", e.getMessage());
            throw new ResponseStatusException(BAD_GATEWAY, "Connexion au service IA impossible.");
        }
    }

    private String handleResponse(HttpResponse<String> response) throws Exception {
        int status = response.statusCode();

        if (status == 401 || status == 403) throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                "Cle API invalide ou expiree. Verifiez votre cle dans .local/openai-api-key.txt");

        if (status == 429) throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                "Quota IA atteint. Attendez quelques secondes et reessayez.");

        if (status == 400) {
            log.warn("TutorAiClient 400: {}", response.body());
            throw new ResponseStatusException(BAD_REQUEST, "Requete IA invalide.");
        }

        if (status != 200) {
            log.warn("TutorAiClient unexpected status {}: {}", status, response.body());
            throw new ResponseStatusException(BAD_GATEWAY, "Erreur du service IA (code " + status + ").");
        }

        JsonNode json = mapper.readTree(response.body());

        // Extraire le contenu : choices[0].message.content
        JsonNode content = json.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText("").isBlank()) {
            log.warn("TutorAiClient empty content: {}", response.body());
            throw new ResponseStatusException(BAD_GATEWAY, "L'IA n'a pas retourne de reponse.");
        }

        return content.asText().strip();
    }
}
