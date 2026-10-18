package com.smartlearn.flashcards;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AzureSpeechClientTests {
    @TempDir Path folder;
    HttpServer server;
    String requestBody;
    String requestKey, format;
    AtomicInteger calls = new AtomicInteger();
    int status = 200;
    byte[] payload;
    @BeforeEach void setup() throws Exception {
        var format = new AudioFormat(24000, 16, 1, true, false);
        try (var output = new ByteArrayOutputStream(); var audio = new AudioInputStream(new ByteArrayInputStream(new byte[48000]), format, 24000)) {
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, output); payload = output.toByteArray();
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/speech", exchange -> {
            calls.incrementAndGet(); requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requestKey = exchange.getRequestHeaders().getFirst("Ocp-Apim-Subscription-Key");
            this.format = exchange.getRequestHeaders().getFirst("X-Microsoft-OutputFormat");
            exchange.sendResponseHeaders(status, payload.length);
            exchange.getResponseBody().write(payload); exchange.close();
        });
        server.start();
    }
    @AfterEach void stop() { server.stop(0); }
    AzureSpeechClient client(String key, String region, String file) {
        return new AzureSpeechClient(new ObjectMapper(), key, region, file,
            "fr-FR-VivienneMultilingualNeural", "en-US-JennyNeural", "ar-SA-ZariyahNeural",
            URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/speech"));
    }
    @Test void usesVivienneMultilingualAndEscapesDocumentTextAsData() throws Exception {
        var client = client("test-secret", "francecentral", "missing");
        var output = folder.resolve("narration.wav");
        client.synthesize("fr", "Une clé <voice name=\"other\"> & un identifiant.", output);
        assertEquals("test-secret", requestKey);
        assertEquals("riff-24khz-16bit-mono-pcm", format);
        assertTrue(requestBody.contains("name=\"fr-FR-VivienneMultilingualNeural\""));
        assertTrue(requestBody.contains("&lt;voice name=\"other\"&gt; &amp;"));
        assertArrayEquals(payload, Files.readAllBytes(output));
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        var xml = factory.newDocumentBuilder().parse(new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, xml.getElementsByTagName("voice").getLength());
    }
    @Test void loadsIgnoredSettingsAndPicksUpChangesWithoutRestart() throws Exception {
        var file = folder.resolve("settings.json");
        var client = client("", "", file.toString());
        assertFalse(client.configured());
        Files.writeString(file, "\uFEFF{\"key\":\"file-secret\",\"region\":\"westeurope\"}");
        assertTrue(client.configured());
        client.synthesize("en", "An English lesson.", folder.resolve("en.wav"));
        assertEquals("file-secret", requestKey); assertTrue(requestBody.contains("en-US-JennyNeural"));
        Files.writeString(file, "{\"key\":\"\",\"region\":\"\"}");
        assertFalse(client.configured());
    }
    @Test void rejectsMissingCredentialsInvalidRegionsAndInvalidAudioWithoutPersistingIt() {
        var output = folder.resolve("audio.wav");
        assertThrows(ResponseStatusException.class, () -> client("", "", "missing").synthesize("fr", "Bonjour", output));
        assertFalse(client("secret", "evil.example/path", "missing").configured());
        assertEquals(0, calls.get());
        payload = "not audio".getBytes(StandardCharsets.UTF_8);
        var failure = assertThrows(ResponseStatusException.class, () -> client("secret", "westeurope", "missing").synthesize("fr", "Bonjour", output));
        assertEquals(502, failure.getStatusCode().value()); assertFalse(Files.exists(output));
    }
    @Test void reportsAuthenticationQuotaAndVoiceErrorsWithoutLeakingProviderBodies() {
        var client = client("secret", "westeurope", "missing");
        payload = "private provider details".getBytes(StandardCharsets.UTF_8);
        for (int code : new int[]{401, 403, 429, 400, 404, 500}) {
            status = code;
            var error = assertThrows(ResponseStatusException.class, () -> client.synthesize("ar", "مرحبا", folder.resolve("audio.wav")));
            assertFalse(error.getMessage().contains("private provider"));
            assertEquals(code == 401 || code == 403 || code == 429 ? 503 : 502, error.getStatusCode().value());
        }
    }
}
