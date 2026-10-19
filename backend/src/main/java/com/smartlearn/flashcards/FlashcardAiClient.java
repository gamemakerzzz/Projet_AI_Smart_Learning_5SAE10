package com.smartlearn.flashcards;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import static org.springframework.http.HttpStatus.*;

@Service
public class FlashcardAiClient {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private OpenAiConnection connection;
    private final ObjectMapper mapper;
    private final String key, keyFile, model, url;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public FlashcardAiClient(ObjectMapper mapper, @Value("${smartlearn.ai.api-key:}") String key,
        @Value("${smartlearn.ai.key-file:.local/openai-api-key.txt}") String keyFile,
        @Value("${smartlearn.ai.model:gpt-6-luna}") String model,
        @Value("${smartlearn.ai.responses-url:https://api.openai.com/v1/responses}") String url) {
        this.mapper = mapper; this.key = key; this.keyFile = keyFile; this.model = model; this.url = url;
    }
    private String apiKey() {
        if (!key.isBlank()) return key.strip();
        try { return Files.readString(Path.of(keyFile)).strip(); } catch (Exception e) { return ""; }
    }
    public boolean configured() { return connected() || !apiKey().isBlank(); }
    public boolean connected() { return connection != null && connection.connected(); }
    public String model() { return model; }
    public JsonNode generate(String instructions, String input, String name, Map<String, Object> schema) {
        return generate(instructions, input, name, schema, model, null, Duration.ofSeconds(180));
    }
    public JsonNode generate(String instructions, String input, String name, Map<String, Object> schema,
                             String requestedModel, String reasoningEffort, Duration timeout) {
        boolean personal = connected();
        String credential = personal ? connection.accessToken() : apiKey();
        if (credential.isBlank()) throw new ResponseStatusException(SERVICE_UNAVAILABLE,
            "Connectez votre compte avec Continuer avec ChatGPT pour utiliser l'IA.");
        try {
            var body = new HashMap<String, Object>();
            body.put("model", requestedModel); body.put("store", false); body.put("instructions", instructions);
            if (reasoningEffort != null) body.put("reasoning", Map.of("effort", reasoningEffort));
            body.put("input", List.of(Map.of("role", "user", "content", input)));
            body.put("text", Map.of("format", Map.of("type", "json_schema", "name", name, "strict", true, "schema", schema)));
            if (personal) body.put("stream", true); else body.put("max_output_tokens", 6000);
            var request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout)
                .header("Authorization", "Bearer " + credential).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                "Le quota IA est atteint ou le service est occupé. Réessayez plus tard.");
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                if (personal) connection.disconnect(OpenAiConnection.currentSession());
                throw new ResponseStatusException(SERVICE_UNAVAILABLE, personal
                    ? "OpenAI a refusé l'accès à l'IA. Reconnectez votre compte et vérifiez les autorisations de votre forfait."
                    : "L'accès au service IA doit être vérifié par l'administrateur.");
            }
            if (response.statusCode() != 200) throw new ResponseStatusException(BAD_GATEWAY,
                "Le service IA n'a pas pu traiter la demande. Réessayez plus tard.");
            JsonNode result = personal ? completedStream(response.body()) : mapper.readTree(response.body());
            if (!"completed".equals(result.path("status").asText())) throw new ResponseStatusException(BAD_GATEWAY,
                "La réponse IA est incomplète. Essayez de générer moins de cartes.");
            StringBuilder text = new StringBuilder();
            for (JsonNode output : result.path("output")) for (JsonNode content : output.path("content")) {
                if ("refusal".equals(content.path("type").asText())) throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
                    "Le service IA ne peut pas créer de cartes pour ce contenu.");
                if ("output_text".equals(content.path("type").asText())) text.append(content.path("text").asText());
            }
            if (!text.toString().isBlank()) {
                try {
                    JsonNode generated = mapper.readTree(text.toString());
                    if (generated != null && generated.isObject()) return generated;
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    throw new ResponseStatusException(BAD_GATEWAY, "La réponse IA n'est pas au format attendu. Réessayez avec moins de cartes.");
                }
                throw new ResponseStatusException(BAD_GATEWAY, "La réponse IA n'est pas au format attendu. Réessayez avec moins de cartes.");
            }
            org.slf4j.LoggerFactory.getLogger(FlashcardAiClient.class).warn("OpenAI completed without usable text; output item count={}", result.path("output").size());
            throw new ResponseStatusException(BAD_GATEWAY, "Le service IA n'a pas renvoyé de contenu exploitable.");
        } catch (HttpTimeoutException e) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT, "Le service IA met trop de temps à répondre. Réessayez avec moins de pages.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new ResponseStatusException(SERVICE_UNAVAILABLE, "La demande IA a été interrompue.");
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "Connexion au service IA impossible. Réessayez plus tard.");
        }
    }
    private JsonNode completedStream(String stream) throws java.io.IOException {
        JsonNode completed = null;
        var items = new TreeMap<Integer, JsonNode>();
        var parts = new TreeMap<String, JsonNode>();
        var deltas = new TreeMap<String, StringBuilder>();
        var events = new ArrayList<JsonNode>();
        StringBuilder data = new StringBuilder();
        for (String line : (stream + "\n\n").split("\\R", -1)) {
            if (line.isEmpty() && !data.isEmpty()) {
                String payload = data.toString(); data.setLength(0);
                if (!payload.equals("[DONE]")) events.add(mapper.readTree(payload));
            } else if (line.startsWith("data:")) {
                if (!data.isEmpty()) data.append('\n');
                String value = line.substring(5); data.append(value.startsWith(" ") ? value.substring(1) : value);
            }
        }
        for (JsonNode event : events) {
            String type = event.path("type").asText();
            String index = String.format(Locale.ROOT, "%08d:%08d", event.path("output_index").asInt(), event.path("content_index").asInt());
            if ("response.completed".equals(type)) completed = event.path("response");
            if (Set.of("response.failed", "response.incomplete", "error").contains(type))
                throw new ResponseStatusException(BAD_GATEWAY, "La génération OpenAI a échoué. Réessayez avec moins de pages.");
            if (type.startsWith("response.refusal."))
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Le service IA ne peut pas créer de cartes pour ce contenu.");
            if ("response.output_item.done".equals(type)) items.put(event.path("output_index").asInt(), event.path("item"));
            if ("response.content_part.done".equals(type)) parts.put(index, event.path("part"));
            if ("response.output_text.done".equals(type)) parts.put(index,
                mapper.createObjectNode().put("type", "output_text").put("text", event.path("text").asText()));
            if ("response.output_text.delta".equals(type)) deltas.computeIfAbsent(index, key -> new StringBuilder()).append(event.path("delta").asText());
        }
        if (completed == null || !"completed".equals(completed.path("status").asText()))
            throw new ResponseStatusException(BAD_GATEWAY, "La réponse OpenAI n'a pas été terminée. Réessayez.");
        // Some streamed responses finalize the text before the last response envelope.
        // Never use partial text unless a successful response.completed event was received.
        var output = mapper.createArrayNode();
        completed.path("output").forEach(output::add);
        if (!hasContent(output)) {
            output.removeAll(); items.values().forEach(output::add);
        }
        if (!hasContent(output)) {
            deltas.forEach((index, value) -> parts.putIfAbsent(index,
                mapper.createObjectNode().put("type", "output_text").put("text", value.toString())));
            var content = mapper.createArrayNode(); parts.values().forEach(content::add);
            output.removeAll(); output.add(mapper.createObjectNode().put("type", "message").set("content", content));
        }
        return mapper.createObjectNode().put("status", "completed").set("output", output);
    }
    private boolean hasContent(JsonNode outputs) {
        for (JsonNode output : outputs) for (JsonNode content : output.path("content")) {
            if ("refusal".equals(content.path("type").asText()) ||
                ("output_text".equals(content.path("type").asText()) && !content.path("text").asText().isBlank())) return true;
        }
        return false;
    }
    public static Map<String, Object> objectSchema(Map<String, Object> properties) {
        return Map.of("type", "object", "properties", properties, "required", new ArrayList<>(properties.keySet()), "additionalProperties", false);
    }
}
