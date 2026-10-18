package com.smartlearn.flashcards;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.springframework.http.HttpStatus.*;

/** OAuth credentials belong to a single browser session, never to the shared application. */
@Service
public class OpenAiConnection {
    static final String CONNECTION = "openai-personal-connection";
    private static final String RESOURCE = "https://api.openai.com/v1";
    private final ObjectMapper mapper;
    private final String redirect, authorizeUrl, tokenUrl;
    private final JwtDecoder decoder;
    private final String hostId;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private record Attempt(HttpSession session, String verifier, String nonce, Instant expires, String clientId, String subject) {}
    // Deliberately not a record: default toString must never expose tokens.
    static final class Connection {
        String access, refresh, clientId, subject;
        Instant expires;
        Connection(String access, String refresh, String clientId, String subject, Instant expires) {
            this.access = access; this.refresh = refresh; this.clientId = clientId; this.subject = subject; this.expires = expires;
        }
    }
    public OpenAiConnection(ObjectMapper mapper,
        @Value("${smartlearn.openai.redirect-uri:http://127.0.0.1:8081/api/flashcards/openai/callback}") String redirect,
        @Value("${smartlearn.openai.authorize-url:https://auth.openai.com/api/accounts/authorize}") String authorizeUrl,
        @Value("${smartlearn.openai.token-url:https://auth.openai.com/api/accounts/oauth/token}") String tokenUrl,
        @Value("${smartlearn.openai.jwks-url:https://auth.openai.com/.well-known/jwks.json}") String jwksUrl,
        @Value("${smartlearn.openai.host-file:.local/openai-host-id.txt}") String hostFile) {
        this.mapper = mapper; this.redirect = redirect; this.authorizeUrl = authorizeUrl; this.tokenUrl = tokenUrl;
        var jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUrl).build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("https://auth.openai.com"));
        this.decoder = jwtDecoder;
        String id;
        try {
            Path path = Path.of(hostFile);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            if (!Files.exists(path)) Files.writeString(path, "urn:uuid:" + UUID.randomUUID(), StandardOpenOption.CREATE_NEW);
            id = Files.readString(path).strip();
        } catch (Exception e) { id = "urn:uuid:" + UUID.randomUUID(); }
        hostId = id;
    }
    static HttpSession currentSession() {
        var attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes request ? request.getRequest().getSession(false) : null;
    }
    public boolean connected() { return connection(currentSession()) != null; }
    private Connection connection(HttpSession session) {
        return session != null && session.getAttribute(CONNECTION) instanceof Connection c ? c : null;
    }
    public synchronized String start(HttpSession session) {
        attempts.entrySet().removeIf(e -> e.getValue().expires().isBefore(Instant.now()) || e.getValue().session().getId().equals(session.getId()));
        if (attempts.size() >= 100) throw new ResponseStatusException(TOO_MANY_REQUESTS, "Trop de connexions en cours. Réessayez dans quelques minutes.");
        String state = random(), verifier = random(), nonce = random();
        Connection existing = connection(session);
        attempts.put(state, new Attempt(session, verifier, nonce, Instant.now().plusSeconds(300),
            existing == null ? null : existing.clientId, existing == null ? null : existing.subject));
        var parameters = new LinkedHashMap<String, String>();
        parameters.put("client_id", existing == null ? "dynamic_agent_client" : existing.clientId);
        if (existing == null) parameters.put("agent_name_hint", "SmartLearn");
        parameters.put("ext_agent_host_id", hostId); parameters.put("response_type", "code");
        parameters.put("redirect_uri", redirect); parameters.put("scope", "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct");
        parameters.put("resource", RESOURCE); parameters.put("state", state); parameters.put("nonce", nonce);
        parameters.put("code_challenge_method", "S256");
        try { parameters.put("code_challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        return authorizeUrl + "?" + form(parameters);
    }
    public void complete(String state, String code, String clientId, String error) {
        Attempt attempt = state == null ? null : attempts.remove(state);
        if (attempt == null || attempt.expires().isBefore(Instant.now())) throw new ResponseStatusException(BAD_REQUEST, "La connexion a expiré ou son état est invalide. Recommencez.");
        if (error != null) throw new ResponseStatusException(BAD_REQUEST, "La connexion OpenAI a été annulée ou refusée.");
        if (attempt.clientId() != null) {
            if (clientId != null && !clientId.equals(attempt.clientId())) throw new ResponseStatusException(BAD_REQUEST, "La connexion OpenAI ne correspond pas au compte sélectionné.");
            clientId = attempt.clientId();
        }
        if (code == null || code.length() > 4096 || clientId == null || !clientId.startsWith("oaiapp_") || clientId.length() > 256)
            throw new ResponseStatusException(BAD_REQUEST, "OpenAI n'a pas terminé l'enregistrement de l'application.");
        JsonNode tokens = exchange(Map.of("grant_type", "authorization_code", "client_id", clientId, "code", code,
            "code_verifier", attempt.verifier(), "redirect_uri", redirect, "resource", RESOURCE));
        try {
            Jwt identity = decoder.decode(tokens.path("id_token").asText());
            if (!identity.getAudience().contains(clientId) || !attempt.nonce().equals(identity.getClaimAsString("nonce")) || identity.getSubject() == null
                || (attempt.subject() != null && !attempt.subject().equals(identity.getSubject())))
                throw new IllegalArgumentException("Invalid identity");
            checkTokens(tokens);
            attempt.session().setAttribute(CONNECTION, new Connection(tokens.path("access_token").asText(), tokens.path("refresh_token").asText(),
                clientId, identity.getSubject(), expiry(tokens)));
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "Impossible de vérifier le compte ou l'autorisation IA. Reconnectez-vous et autorisez l'utilisation du forfait ChatGPT.");
        }
    }
    public String accessToken() {
        HttpSession session = currentSession();
        Connection connection = connection(session);
        if (connection == null) return null;
        synchronized (connection) {
            if (connection.expires.isBefore(Instant.now().plusSeconds(60))) {
                try {
                    if (connection.refresh.isBlank()) throw new IllegalStateException();
                    JsonNode tokens = exchange(Map.of("grant_type", "refresh_token", "client_id", connection.clientId,
                        "refresh_token", connection.refresh, "resource", RESOURCE));
                    checkTokens(tokens);
                    if (!tokens.path("id_token").asText().isBlank()) {
                        Jwt identity = decoder.decode(tokens.path("id_token").asText());
                        if (!identity.getAudience().contains(connection.clientId) || !connection.subject.equals(identity.getSubject())) throw new IllegalArgumentException("Invalid refreshed identity");
                    }
                    connection.access = tokens.path("access_token").asText(); connection.expires = expiry(tokens);
                    if (!tokens.path("refresh_token").asText().isBlank()) connection.refresh = tokens.path("refresh_token").asText();
                } catch (Exception e) { disconnect(session); throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Votre connexion OpenAI a expiré. Utilisez Continuer avec ChatGPT pour vous reconnecter."); }
            }
            return connection.access;
        }
    }
    public void disconnect(HttpSession session) {
        session.removeAttribute(CONNECTION);
        attempts.entrySet().removeIf(e -> e.getValue().session().getId().equals(session.getId()));
    }
    private Instant expiry(JsonNode tokens) { return Instant.now().plusSeconds(Math.min(86400, Math.max(1, tokens.path("expires_in").asLong(3600)))); }
    private void checkTokens(JsonNode tokens) {
        Set<String> scopes = new HashSet<>(Arrays.asList(tokens.path("scope").asText().split(" ")));
        if (tokens.path("access_token").asText().isBlank() || !scopes.contains("chatgpt.tokens.use.direct") || !scopes.contains("resource.invoke"))
            throw new ResponseStatusException(BAD_REQUEST, "Le compte n'a pas autorisé l'utilisation de l'IA avec son forfait.");
    }
    private JsonNode exchange(Map<String, String> parameters) {
        try {
            var request = HttpRequest.newBuilder(URI.create(tokenUrl)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form(parameters))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new ResponseStatusException(BAD_GATEWAY, "OpenAI a refusé la connexion. Recommencez ou vérifiez l'éligibilité de votre compte.");
            return mapper.readTree(response.body());
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Connexion interrompue."); }
        catch (java.io.IOException e) { throw new ResponseStatusException(BAD_GATEWAY, "Impossible de joindre OpenAI. Réessayez."); }
    }
    private static String random() { byte[] value = new byte[32]; new SecureRandom().nextBytes(value); return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private static String form(Map<String, String> parameters) {
        return parameters.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
    }
}
