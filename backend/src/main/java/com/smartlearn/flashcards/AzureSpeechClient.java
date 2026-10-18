package com.smartlearn.flashcards;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import static org.springframework.http.HttpStatus.*;

@Service
public class AzureSpeechClient {
    private final ObjectMapper mapper;
    private final String key, region, settingsFile;
    private final Map<String, String> voices;
    private final URI testEndpoint;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    @org.springframework.beans.factory.annotation.Autowired
    public AzureSpeechClient(ObjectMapper mapper, @Value("${smartlearn.speech.key:}") String key,
        @Value("${smartlearn.speech.region:}") String region,
        @Value("${smartlearn.speech.settings-file:.local/azure-speech.json}") String settingsFile,
        @Value("${smartlearn.speech.voice-fr:fr-FR-VivienneMultilingualNeural}") String french,
        @Value("${smartlearn.speech.voice-en:en-US-JennyNeural}") String english,
        @Value("${smartlearn.speech.voice-ar:ar-SA-ZariyahNeural}") String arabic) {
        this(mapper, key, region, settingsFile, french, english, arabic, null);
    }
    // The alternate endpoint is only available to package-local tests, never through user input or application configuration.
    AzureSpeechClient(ObjectMapper mapper, String key, String region, String settingsFile,
                      String french, String english, String arabic, URI testEndpoint) {
        this.mapper = mapper; this.key = key; this.region = region; this.settingsFile = settingsFile;
        this.voices = Map.of("fr", french, "en", english, "ar", arabic); this.testEndpoint = testEndpoint;
    }
    private static final class Credentials {
        final String key, region;
        Credentials(String key, String region) { this.key = key; this.region = region; }
        boolean valid() { return !key.isBlank() && key.chars().noneMatch(Character::isISOControl) && region.matches("[a-z0-9]{2,32}"); }
    }
    private Credentials credentials() {
        String savedKey = "", savedRegion = "";
        try {
            String text = Files.readString(Path.of(settingsFile)).replaceFirst("^\\uFEFF", "");
            var settings = mapper.readTree(text);
            savedKey = settings.path("key").asText(""); savedRegion = settings.path("region").asText("");
        } catch (Exception ignored) { }
        return new Credentials((key.isBlank() ? savedKey : key).strip(), (region.isBlank() ? savedRegion : region).strip());
    }
    public boolean configured() { return credentials().valid(); }
    public Map<String, String> voices() { return voices; }
    public String voice(String language) {
        String result = voices.get(language);
        if (result == null || result.length() > 100 || !result.matches("[a-z]{2}-[A-Z]{2}-[A-Za-z0-9:_-]+")) throw new ResponseStatusException(BAD_REQUEST, "Voix Azure invalide pour cette langue.");
        return result;
    }
    public void synthesize(String language, String text, Path output) {
        var credentials = credentials();
        if (!credentials.valid()) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Configurez la clé et la région Azure Speech pour activer la narration naturelle.");
        if (text == null || text.isBlank() || text.length() > 650 || text.chars().anyMatch(c -> Character.isISOControl(c) && !Character.isWhitespace(c)))
            throw new ResponseStatusException(BAD_REQUEST, "Texte de narration invalide.");
        String voice = voice(language), locale = voice.substring(0, 5);
        String ssml = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"" + locale
            + "\"><voice name=\"" + voice + "\">" + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</voice></speak>";
        URI endpoint = testEndpoint != null ? testEndpoint : URI.create("https://" + credentials.region + ".tts.speech.microsoft.com/cognitiveservices/v1");
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(90))
                .header("Ocp-Apim-Subscription-Key", credentials.key).header("Content-Type", "application/ssml+xml; charset=utf-8")
                .header("X-Microsoft-OutputFormat", "riff-24khz-16bit-mono-pcm").header("User-Agent", "SmartLearn")
                .POST(HttpRequest.BodyPublishers.ofString(ssml, StandardCharsets.UTF_8)).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var input = response.body()) {
                int code = response.statusCode();
                if (code == 401 || code == 403) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Azure Speech a refusé la clé ou la région. Vérifiez les paramètres de la ressource Speech.");
                if (code == 429) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Le quota Azure Speech est atteint. Réessayez plus tard.");
                if (code == 400 || code == 404) throw new ResponseStatusException(BAD_GATEWAY, "La voix Azure choisie n'est pas disponible. Vérifiez la voix et sa disponibilité dans la région de votre ressource.");
                if (code != 200) throw new ResponseStatusException(BAD_GATEWAY, "Azure Speech n'a pas pu créer la narration. Réessayez.");
                byte[] bytes = input.readNBytes(8 * 1024 * 1024 + 1);
                if (bytes.length > 8 * 1024 * 1024 || bytes.length < 44 || !new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    || !new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WAVE")) throw invalidAudio();
                try (var audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes))) {
                    double duration = audio.getFrameLength() / audio.getFormat().getFrameRate();
                    if (!Double.isFinite(duration) || duration < 0.1 || duration > 120) throw invalidAudio();
                } catch (javax.sound.sampled.UnsupportedAudioFileException e) { throw invalidAudio(); }
                Files.write(output, bytes);
            }
        } catch (HttpTimeoutException e) { throw new ResponseStatusException(GATEWAY_TIMEOUT, "Azure Speech met trop de temps à répondre. Réessayez."); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Narration interrompue."); }
        catch (IOException e) { throw new ResponseStatusException(BAD_GATEWAY, "Connexion à Azure Speech ou création de l'audio impossible."); }
    }
    private ResponseStatusException invalidAudio() { return new ResponseStatusException(BAD_GATEWAY, "Azure Speech a renvoyé un audio invalide."); }
}
