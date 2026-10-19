package com.smartlearn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.flashcards.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VideoRendererTests {
    @Test
    @EnabledIfSystemProperty(named = "smartlearn.test.video-renderer", matches = "true")
    void producesActualNarratedMp4WithCaptionsAndCleansTemporarySource() throws Exception {
        Path binaries = Path.of("../.local/video-tools/imageio_ffmpeg/binaries");
        String executable;
        try (var files = Files.list(binaries)) {
            executable = files.filter(p -> p.toString().endsWith(".exe")).findFirst().orElseThrow().toAbsolutePath().toString();
        }
        var renderer = new VideoRenderer(new ObjectMapper(), "../.local/video-render-test", executable);
        assertTrue(renderer.languages().contains("fr"));
        var scene = new VideoLesson.Scene("Comprendre les clés primaires", List.of("Une ligne, un identifiant unique.", "Des noms identiques, des clés différentes."),
            "Deux élèves peuvent avoir le même nom, mais pas le même identifiant.",
            "Une clé primaire identifie chaque ligne d’une table. Par exemple, deux élèves peuvent porter le même nom, mais chacun possède un identifiant distinct.",
            1, "Une clé primaire identifie chaque ligne d’une table.");
        var result = renderer.render(new VideoLesson("Les bases de données", "fr", "Page", List.of(scene, scene)), message -> {});
        assertTrue(result.video().length > 10_000);
        assertEquals("ftyp", new String(result.video(), 4, 4, java.nio.charset.StandardCharsets.US_ASCII));
        assertTrue(result.captions().startsWith("WEBVTT\n\n00:00:00.000 --> "));
        assertTrue(result.duration() > 5);
        Path output = Path.of("../.local/video-render-preview.mp4"); Files.createDirectories(output.getParent()); Files.write(output, result.video());
        try (var paths = Files.list(Path.of("../.local/video-render-test"))) { assertEquals(0, paths.count()); }
    }
}
