package com.smartlearn.flashcards;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.springframework.http.HttpStatus.*;

@Service
public class LocalSpeechClient {
    private final Path python, models;
    private static final Map<String, String> MODELS = Map.of("fr", "fr_FR-siwis-medium", "en", "en_US-lessac-high", "ar", "ar_JO-kareem-medium");
    public LocalSpeechClient(@Value("${smartlearn.speech.local-python:.local/voice-runtime/Scripts/python.exe}") String python,
                             @Value("${smartlearn.speech.local-models:.local/voices}") String models) {
        this.python = Path.of(python).toAbsolutePath().normalize(); this.models = Path.of(models).toAbsolutePath().normalize();
    }
    public List<String> languages() {
        if (!Files.isRegularFile(python)) return List.of();
        return MODELS.entrySet().stream().filter(entry -> installed(entry.getValue())).map(Map.Entry::getKey).sorted().toList();
    }
    private boolean installed(String model) {
        try { return Files.size(models.resolve(model + ".onnx")) > 1_000_000 && Files.size(models.resolve(model + ".onnx.json")) > 100; }
        catch (java.io.IOException e) { return false; }
    }
    public String voice(String language) {
        String model = MODELS.get(language);
        if (model == null) throw new ResponseStatusException(BAD_REQUEST, "Langue de narration non prise en charge.");
        return "Piper · " + model;
    }
    public void synthesize(String language, String text, Path work, Path output) {
        if (!languages().contains(language)) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Le modèle vocal gratuit n'est pas installé pour cette langue.");
        if (text == null || text.isBlank() || text.length() > 650) throw new ResponseStatusException(BAD_REQUEST, "Texte de narration invalide.");
        Process process = null;
        try {
            Path script = work.resolve("local-speech.py"), input = work.resolve("narration.txt");
            if (!Files.exists(script)) try (var resource = new ClassPathResource("local-speech.py").getInputStream()) { Files.copy(resource, script); }
            Files.writeString(input, text);
            var builder = new ProcessBuilder(python.toString(), script.toAbsolutePath().toString(), "--models", models.toString(),
                "--language", language, "--text-file", input.toAbsolutePath().toString(), "--output-file", output.toAbsolutePath().toString())
                .directory(work.toFile()).redirectErrorStream(true).redirectOutput(work.resolve("speech-process.log").toFile());
            builder.environment().put("PYTHONUTF8", "1");
            process = builder.start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) throw new ResponseStatusException(GATEWAY_TIMEOUT, "La narration locale met trop de temps à répondre.");
            if (process.exitValue() != 0 || !Files.isRegularFile(output) || Files.size(output) < 44)
                throw new ResponseStatusException(BAD_GATEWAY, "Le modèle vocal local n'a pas pu créer l'audio. Vérifiez l'installation de Piper.");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Narration interrompue."); }
        catch (java.io.IOException e) { throw new ResponseStatusException(BAD_GATEWAY, "Impossible de démarrer le modèle vocal local."); }
        finally { if (process != null && process.isAlive()) process.destroyForcibly(); }
    }
}
