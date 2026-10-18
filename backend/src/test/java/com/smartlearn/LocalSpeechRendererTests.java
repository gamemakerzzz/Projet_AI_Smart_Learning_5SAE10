package com.smartlearn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.flashcards.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import javax.sound.sampled.AudioSystem;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalSpeechRendererTests {
    @Test
    @EnabledIfSystemProperty(named = "smartlearn.test.local-speech", matches = "true")
    void createsFreeNeuralAudioAndAnActualVideoWithoutCloudCredentials() throws Exception {
        var local = new LocalSpeechClient("../.local/voice-runtime/Scripts/python.exe", "../.local/voices");
        assertTrue(local.languages().containsAll(List.of("fr", "en", "ar")));
        Path work = Path.of("../.local/piper-audio-test").toAbsolutePath(); Files.createDirectories(work);
        var texts = Map.of("fr", "Bonjour ! Dans cette leçon, nous allons comprendre les clés primaires. Chaque ligne possède un identifiant unique. Prenons un exemple simple : deux élèves peuvent avoir le même nom, mais chacun a son propre numéro.",
            "en", "Welcome! In this lesson, we will learn how a primary key gives each row in a database a unique identifier.",
            "ar", "مرحباً بكم. في هذا الدرس، سنتعرف على المفتاح الأساسي الذي يميز كل سجل في قاعدة البيانات.");
        for (var entry : texts.entrySet()) {
            Path audio = work.resolve(entry.getKey() + ".wav");
            local.synthesize(entry.getKey(), entry.getValue(), work, audio);
            try (var stream = AudioSystem.getAudioInputStream(audio.toFile())) {
                assertTrue(stream.getFrameLength() / stream.getFormat().getFrameRate() > 1);
                assertEquals(1, stream.getFormat().getChannels());
            }
            Files.copy(audio, Path.of("../.local/piper-" + entry.getKey() + "-sample.wav"), StandardCopyOption.REPLACE_EXISTING);
        }
        String executable;
        try (var files = Files.list(Path.of("../.local/video-tools/imageio_ffmpeg/binaries"))) {
            executable = files.filter(p -> p.toString().endsWith(".exe")).findFirst().orElseThrow().toAbsolutePath().toString();
        }
        var renderer = new VideoRenderer(new ObjectMapper(), "../.local/piper-video-test", executable, null, local);
        assertTrue(renderer.localNarration()); assertTrue(renderer.languages().contains("fr"));
        assertEquals("Piper · fr_FR-siwis-medium", renderer.voice("fr"));
        var scene = new VideoLesson.Scene("Une ligne, un identifiant", List.of("Chaque ligne a une clé unique.", "Deux noms identiques, deux identifiants."),
            "Deux élèves peuvent porter le même nom et avoir des numéros différents.", texts.get("fr"), 1, "Une clé primaire identifie chaque ligne.");
        var rendered = renderer.render(new VideoLesson("Les clés primaires", "fr", "Page", List.of(scene)), message -> {});
        assertTrue(rendered.video().length > 10_000);
        assertEquals("ftyp", new String(rendered.video(), 4, 4, java.nio.charset.StandardCharsets.US_ASCII));
        assertTrue(rendered.captions().contains("Bonjour"));
        Files.write(Path.of("../.local/piper-video-preview.mp4"), rendered.video());
        try (var files = Files.list(Path.of("../.local/piper-video-test"))) { assertEquals(0, files.count()); }
        try (var files = Files.list(work)) { for (var file : files.toList()) Files.delete(file); }
        Files.delete(work);
    }
}
