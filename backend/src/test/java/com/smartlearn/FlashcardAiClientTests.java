package com.smartlearn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.flashcards.FlashcardAiClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class FlashcardAiClientTests {
    private String events(Object... events) throws Exception {
        var mapper = new ObjectMapper(); var stream = new StringBuilder(": heartbeat\r\n\r\n");
        for (Object event : events) stream.append("data: ").append(mapper.writeValueAsString(event)).append("\r\n\r\n");
        return stream.append("data: [DONE]\r\n\r\n").toString();
    }
    private com.fasterxml.jackson.databind.JsonNode streamed(String body) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/responses", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        try {
            var personal = org.mockito.Mockito.mock(com.smartlearn.flashcards.OpenAiConnection.class);
            org.mockito.Mockito.when(personal.connected()).thenReturn(true);
            org.mockito.Mockito.when(personal.accessToken()).thenReturn("test-oauth-token");
            var client = new FlashcardAiClient(new ObjectMapper(), "", "missing-test-key", "gpt-6-luna",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/responses");
            org.springframework.test.util.ReflectionTestUtils.setField(client, "connection", personal);
            return client.generate("instructions", "source", "deck", FlashcardAiClient.objectSchema(Map.of("title", Map.of("type", "string"))));
        } finally { server.stop(0); }
    }
    private Map<String, Object> completed() {
        return Map.of("type", "response.completed", "response", Map.of("status", "completed", "output", java.util.List.of()));
    }
    @Test void recoversFinalTextWhenCompletedEnvelopeOmitsOutput() throws Exception {
        var done = Map.of("type", "response.output_text.done", "output_index", 1, "content_index", 0, "text", "{\"title\":\"Recovered\"}");
        assertEquals("Recovered", streamed(events(done, completed())).path("title").asText());
    }
    @Test void recoversCompletedMessageItemsAndContentParts() throws Exception {
        var part = Map.of("type", "output_text", "text", "{\"title\":\"Recovered\"}");
        var item = Map.of("type", "response.output_item.done", "output_index", 1, "item", Map.of("type", "message", "content", java.util.List.of(part)));
        assertEquals("Recovered", streamed(events(item, completed())).path("title").asText());
        var content = Map.of("type", "response.content_part.done", "output_index", 1, "content_index", 0, "part", part);
        assertEquals("Recovered", streamed(events(content, completed())).path("title").asText());
    }
    @Test void acceptsAccumulatedTextOnlyAfterSuccessfulCompletion() throws Exception {
        var first = Map.of("type", "response.output_text.delta", "output_index", 1, "content_index", 0, "delta", "{\"title\":");
        var second = Map.of("type", "response.output_text.delta", "output_index", 1, "content_index", 0, "delta", "\"Recovered\"}");
        assertEquals("Recovered", streamed(events(first, second, completed())).path("title").asText());
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> streamed(events(first, second)));
        assertEquals(502, error.getStatusCode().value());
    }
    @Test void refusesFailedOrRefusedStreamsEvenWhenTextWasAlreadyReceived() throws Exception {
        var done = Map.of("type", "response.output_text.done", "text", "{\"title\":\"Do not accept\"}");
        for (String type : java.util.List.of("response.failed", "response.incomplete", "error", "response.refusal.done")) {
            var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> streamed(events(done, Map.of("type", type), completed())));
            assertEquals(type.contains("refusal") ? 422 : 502, error.getStatusCode().value());
        }
    }
    @Test void personalAccountUsesStreamingContractWithoutUnsupportedTokenLimit() throws Exception {
        var mapper = new ObjectMapper();
        var received = new AtomicReference<com.fasterxml.jackson.databind.JsonNode>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/responses", exchange -> {
            received.set(mapper.readTree(exchange.getRequestBody()));
            String completed = "{\"type\":\"response.completed\",\"response\":{\"status\":\"completed\",\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\"{\\\"title\\\":\\\"Personal deck\\\"}\"}]}]}}";
            byte[] bytes = ("event: response.completed\ndata: " + completed + "\n\ndata: [DONE]\n\n").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        try {
            var personal = org.mockito.Mockito.mock(com.smartlearn.flashcards.OpenAiConnection.class);
            org.mockito.Mockito.when(personal.connected()).thenReturn(true);
            org.mockito.Mockito.when(personal.accessToken()).thenReturn("test-oauth-token");
            var client = new FlashcardAiClient(mapper, "", "missing-test-key", "gpt-6-luna",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/responses");
            org.springframework.test.util.ReflectionTestUtils.setField(client, "connection", personal);
            var result = client.generate("instructions", "source", "deck", FlashcardAiClient.objectSchema(Map.of("title", Map.of("type", "string"))));
            assertEquals("Personal deck", result.path("title").asText());
            assertTrue(received.get().path("stream").asBoolean());
            assertTrue(received.get().path("input").isArray());
            assertFalse(received.get().has("max_output_tokens"));
        } finally { server.stop(0); }
    }
    @Test void sendsLunaStructuredResponseRequestAndHandlesOutput() throws Exception {
        var mapper = new ObjectMapper();
        var received = new AtomicReference<com.fasterxml.jackson.databind.JsonNode>();
        var authorization = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/responses", exchange -> {
            received.set(mapper.readTree(exchange.getRequestBody()));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] result = "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"{\\\"title\\\":\\\"Deck\\\"}\"}]}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, result.length);
            try (var output = exchange.getResponseBody()) { output.write(result); }
        });
        server.start();
        try {
            var client = new FlashcardAiClient(mapper, "test-only-key", "missing-test-key", "gpt-6-luna",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/responses");
            var response = client.generate("instructions", "source", "deck", FlashcardAiClient.objectSchema(Map.of("title", Map.of("type", "string"))));
            assertEquals("Deck", response.path("title").asText());
            assertEquals("Bearer test-only-key", authorization.get());
            assertEquals("gpt-6-luna", received.get().path("model").asText());
            assertFalse(received.get().path("store").asBoolean());
            assertEquals("json_schema", received.get().path("text").path("format").path("type").asText());
            assertTrue(received.get().path("text").path("format").path("strict").asBoolean());
        } finally { server.stop(0); }
    }
    @Test void missingCredentialProducesAnActionableError() {
        var client = new FlashcardAiClient(new ObjectMapper(), "", "nonexistent-test-key-file", "gpt-6-luna", "http://127.0.0.1:1");
        assertFalse(client.configured());
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> client.generate("instructions", "source", "deck", Map.of()));
        assertEquals(503, error.getStatusCode().value());
    }
}
