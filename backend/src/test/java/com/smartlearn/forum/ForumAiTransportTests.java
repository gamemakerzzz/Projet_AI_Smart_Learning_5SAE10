package com.smartlearn.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.flashcards.FlashcardAiClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ForumAiTransportTests {
    @Test void allForumRequestsUseLunaWithNoReasoningEvenWhenOtherModulesUseAnotherModel() throws Exception {
        var mapper=new ObjectMapper();
        var received=new ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/responses", exchange->{
            received.add(mapper.readTree(exchange.getRequestBody()));
            var output=Map.of("status","completed","output",List.of(Map.of("content",List.of(Map.of("type","output_text","text","{\"results\":[]}")))));
            byte[] bytes=mapper.writeValueAsBytes(output);
            exchange.sendResponseHeaders(200,bytes.length);
            try(var stream=exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        try {
            var transport=new FlashcardAiClient(mapper,"test-key","missing-key-file","another-model","http://127.0.0.1:"+server.getAddress().getPort()+"/responses");
            var forum=new ForumAiClient(transport,mapper);
            forum.structured("Test instructions",Map.of("title","Public test"),"forum_topic_tags",FlashcardAiClient.objectSchema(Map.of("results",Map.of("type","array","items",Map.of("type","string")))));
            forum.classify(new ForumModels.Draft("Protect routes","How can I protect routes?",null),List.of());
            assertEquals("gpt-6-luna",forum.model()); assertEquals(2,received.size());
            for(var request:received) {
                assertEquals("gpt-6-luna",request.path("model").asText());
                assertEquals("none",request.path("reasoning").path("effort").asText());
                assertFalse(request.path("store").asBoolean());
                assertEquals("json_schema",request.path("text").path("format").path("type").asText());
            }
        } finally { server.stop(0); }
    }
}
