package com.smartlearn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.smartlearn.flashcards.OpenAiConnection;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.*;
import org.springframework.web.context.request.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiConnectionTests {
    @TempDir Path directory;
    final ObjectMapper mapper = new ObjectMapper();
    HttpServer server;
    RSAKey rsa;
    OpenAiConnection connection;
    AtomicReference<String> nonce = new AtomicReference<>();
    AtomicReference<Map<String, String>> exchanged = new AtomicReference<>();
    AtomicInteger calls = new AtomicInteger();
    String scopes = "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct";
    @BeforeEach void setup() throws Exception {
        rsa = new RSAKeyGenerator(2048).keyID("test-signing-key").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] body = new JWKSet(rsa.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.createContext("/token", exchange -> {
            try {
                calls.incrementAndGet();
                exchanged.set(parameters(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                var claims = new JWTClaimsSet.Builder().issuer("https://auth.openai.com").subject("test-subject").audience("oaiapp_test")
                    .expirationTime(Date.from(Instant.now().plusSeconds(300))).claim("nonce", nonce.get()).build();
                var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsa.getKeyID()).build(), claims);
                jwt.sign(new RSASSASigner(rsa));
                byte[] body = mapper.writeValueAsBytes(Map.of("access_token", "private-test-token", "refresh_token", "private-refresh",
                    "token_type", "Bearer", "expires_in", 3600, "scope", scopes, "id_token", jwt.serialize()));
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            } catch (Exception e) { exchange.sendResponseHeaders(500, -1); exchange.close(); }
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        connection = new OpenAiConnection(mapper, "http://127.0.0.1:8081/api/flashcards/openai/callback",
            "https://auth.openai.com/api/accounts/authorize", base + "/token", base + "/jwks", directory.resolve("host-id").toString());
    }
    @AfterEach void cleanup() { server.stop(0); RequestContextHolder.resetRequestAttributes(); }
    Map<String, String> start(MockHttpSession session) {
        var parameters = parameters(URI.create(connection.start(session)).getRawQuery());
        nonce.set(parameters.get("nonce")); return parameters;
    }
    void bind(MockHttpSession session) {
        var request = new MockHttpServletRequest(); request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
    @Test void verifiesSignedIdentityPkceAndKeepsCredentialsInOneVisitorsSession() {
        var visitor = new MockHttpSession(); var pending = start(visitor);
        assertEquals("dynamic_agent_client", pending.get("client_id"));
        assertEquals("S256", pending.get("code_challenge_method"));
        assertTrue(pending.get("scope").contains("chatgpt.tokens.use.direct"));
        connection.complete(pending.get("state"), "test-code", "oaiapp_test", null);
        assertEquals("authorization_code", exchanged.get().get("grant_type"));
        assertNotNull(exchanged.get().get("code_verifier"));
        assertEquals(pending.get("redirect_uri"), exchanged.get().get("redirect_uri"));
        bind(visitor); assertTrue(connection.connected()); assertEquals("private-test-token", connection.accessToken());
        bind(new MockHttpSession()); assertFalse(connection.connected()); assertNull(connection.accessToken());
        assertThrows(ResponseStatusException.class, () -> connection.complete(pending.get("state"), "test-code", "oaiapp_test", null));
        assertEquals(1, calls.get());
        var returning = start(visitor); assertEquals("oaiapp_test", returning.get("client_id"));
        assertFalse(returning.containsKey("agent_name_hint"));
        bind(visitor); connection.disconnect(visitor); assertFalse(connection.connected());
    }
    @Test void refusesWrongStateNonceAndInsufficientPermissions() {
        var visitor = new MockHttpSession(); var pending = start(visitor);
        assertThrows(ResponseStatusException.class, () -> connection.complete("wrong-state", "test-code", "oaiapp_test", null));
        assertEquals(0, calls.get());
        nonce.set("wrong-nonce");
        assertThrows(ResponseStatusException.class, () -> connection.complete(pending.get("state"), "test-code", "oaiapp_test", null));
        bind(visitor); assertFalse(connection.connected());
        var another = start(visitor); scopes = "openid profile email";
        assertThrows(ResponseStatusException.class, () -> connection.complete(another.get("state"), "test-code", "oaiapp_test", null));
        assertFalse(connection.connected());
    }
    @Test void cancellationDoesNotCallTokenEndpoint() {
        var pending = start(new MockHttpSession());
        assertThrows(ResponseStatusException.class, () -> connection.complete(pending.get("state"), null, null, "access_denied"));
        assertEquals(0, calls.get());
    }
    static Map<String, String> parameters(String query) {
        Map<String, String> values = new HashMap<>();
        for (String pair : query.split("&")) { var parts = pair.split("=", 2); values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), URLDecoder.decode(parts.length > 1 ? parts[1] : "", StandardCharsets.UTF_8)); }
        return values;
    }
}
