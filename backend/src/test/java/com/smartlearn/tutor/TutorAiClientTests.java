package com.smartlearn.tutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TutorAiClientTests {
    @TempDir Path directory;
    HttpServer server;
    ObjectMapper mapper=new ObjectMapper();
    com.fasterxml.jackson.databind.JsonNode request;
    String authorization;
    int responseStatus=200;
    String response="{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"Valid reply\"}}]}";
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/chat/completions",exchange->{
            request=mapper.readTree(exchange.getRequestBody()); authorization=exchange.getRequestHeaders().getFirst("Authorization");
            byte[] bytes=response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus,bytes.length);
            try(var out=exchange.getResponseBody()){out.write(bytes);}
        });server.start();
    }
    @AfterEach void stop(){server.stop(0);}
    TutorAiClient client(String key,String file){return new TutorAiClient(mapper,key,file,"qwen/qwen3.8-27b","http://127.0.0.1:"+server.getAddress().getPort()+"/chat/completions");}
    @Test void sendsCompatibleChatHistoryAndJsonModeWithDedicatedCredentials(){
        var client=client("test-groq-key","missing");
        assertEquals("Valid reply",client.chatMultiTurn("System",List.of(Map.of("role","user","content","Earlier question"),Map.of("role","assistant","content","Earlier answer")),"Next question"));
        assertEquals("Bearer test-groq-key",authorization); assertEquals(4,request.path("messages").size());
        assertEquals("Next question",request.path("messages").path(3).path("content").asText());
        assertEquals("qwen/qwen3.8-27b",request.path("model").asText()); assertEquals(3000,request.path("max_completion_tokens").asInt());
        assertEquals("none",request.path("reasoning_effort").asText());
        client.chatJson("Return JSON","Explain keys");
        assertEquals("json_object",request.path("response_format").path("type").asText());
    }
    @Test void keyFileCanBeConfiguredWithoutRestartAndMissingKeyFailsImmediately() throws Exception {
        Path file=directory.resolve("groq-key.txt");var client=client("",file.toString());assertFalse(client.configured());
        assertEquals(503,assertThrows(ResponseStatusException.class,()->client.chat("System","Hello")).getStatusCode().value());
        Files.writeString(file,"test-file-key");assertTrue(client.configured());client.chat("System","Hello");assertEquals("Bearer test-file-key",authorization);
    }
    @Test void rejectsPartialAndEmptyResponses(){
        var client=client("test","missing");
        response="{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"Partial output\"}}]}";
        assertEquals(502,assertThrows(ResponseStatusException.class,()->client.chat("System","Hello")).getStatusCode().value());
        response="{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"\"}}]}";
        assertEquals(502,assertThrows(ResponseStatusException.class,()->client.chat("System","Hello")).getStatusCode().value());
    }
    @Test void sanitizesProviderFailures(){
        var client=client("test-private-key","missing");response="private lesson text and credential";
        for(int code:List.of(401,403,429,500)){
            responseStatus=code;var error=assertThrows(ResponseStatusException.class,()->client.chat("System","Sensitive lesson"));
            assertEquals(code==429?429:code==500?502:503,error.getStatusCode().value());
            assertFalse(error.getReason().contains(response));assertFalse(error.getReason().contains("test-private-key"));
        }
    }
}
