package com.smartlearn.flashcards;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.awt.font.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.text.AttributedString;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class VideoRenderer {
    private final Path root;
    private final String ffmpeg;
    private final ObjectMapper mapper;
    private List<String> languages;
    private final AzureSpeechClient speech;
    private final LocalSpeechClient localSpeech;
    @Value("${smartlearn.speech.provider:local}")
    private String speechProvider = "local";
    private Boolean ffmpegAvailable;
    @org.springframework.beans.factory.annotation.Autowired
    public VideoRenderer(ObjectMapper mapper, @Value("${smartlearn.video.work-dir:.local/video-work}") String work,
                         @Value("${smartlearn.video.ffmpeg:}") String executable, AzureSpeechClient speech, LocalSpeechClient localSpeech) {
        this.mapper = mapper; this.root = Path.of(work).toAbsolutePath().normalize();
        this.ffmpeg = executable.isBlank() ? bundledFfmpeg() : executable;
        this.speech = speech;
        this.localSpeech = localSpeech;
    }
    public VideoRenderer(ObjectMapper mapper, String work, String executable) {
        this(mapper, work, executable, null, null);
    }
    private String bundledFfmpeg() {
        Path binaries = Path.of(".local/video-tools/imageio_ffmpeg/binaries");
        if (Files.isDirectory(binaries)) try (var paths = Files.list(binaries)) {
            return paths.filter(p -> p.getFileName().toString().matches("ffmpeg.*\\.exe"))
                .findFirst().map(p -> p.toAbsolutePath().toString()).orElse("ffmpeg");
        } catch (IOException ignored) { }
        return "ffmpeg";
    }
    public synchronized List<String> languages() {
        if (localSpeech != null || speech != null) {
            boolean local = speechProvider.equals("local");
            List<String> supported = local ? (localSpeech == null ? List.of() : localSpeech.languages())
                : (speechProvider.equals("azure") && speech != null && speech.configured() ? List.of("fr", "en", "ar") : List.of());
            if (supported.isEmpty()) return supported;
            if (ffmpegAvailable == null) {
                Path probe = null;
                try { probe = workspace(); run(probe, List.of(ffmpeg, "-version"), 10); ffmpegAvailable = true; }
                catch (Exception e) { ffmpegAvailable = false; }
                finally { cleanup(probe); }
            }
            return ffmpegAvailable ? supported : List.of();
        }
        if (languages != null) return languages;
        Path work = null;
        try {
            if (!System.getProperty("os.name").startsWith("Windows")) return languages = List.of();
            work = workspace();
            run(work, List.of(ffmpeg, "-version"), 10);
            run(work, List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-File", speechScript(work).toString(), "-Voices"), 20);
            var result = mapper.readTree(Files.readString(work.resolve("process.log")));
            var supported = new ArrayList<String>();
            if (result.isTextual()) supported.add(result.asText());
            else result.forEach(value -> supported.add(value.asText()));
            return languages = supported.stream().filter(Set.of("fr", "en", "ar")::contains).distinct().toList();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Local video renderer unavailable; check FFmpeg and installed Windows speech voices.");
            return languages = List.of();
        } finally { cleanup(work); }
    }
    public String readinessMessage() {
        if (speechProvider.equals("local") && localSpeech != null && localSpeech.languages().isEmpty()) return "Installez les modèles vocaux gratuits Piper sur le serveur pour activer la narration.";
        if (speechProvider.equals("azure") && speech != null && !speech.configured()) return "Azure Speech n'est pas configuré. Renseignez la clé et la région dans les paramètres du serveur pour activer la voix naturelle.";
        return languages().isEmpty() ? "Le moteur vidéo FFmpeg est indisponible sur le serveur." : "";
    }
    public String voice(String language) {
        if (speechProvider.equals("local") && localSpeech != null) return localSpeech.voice(language);
        return speech == null ? "Voix système" : "Azure Speech · " + speech.voice(language);
    }
    public boolean localNarration() { return speechProvider.equals("local"); }
    public VideoLesson.Rendered render(VideoLesson lesson, Consumer<String> progress) throws Exception {
        Path work = workspace();
        try {
            Path legacySpeech = speech == null && localSpeech == null ? speechScript(work) : null;
            StringBuilder clips = new StringBuilder(), vtt = new StringBuilder("WEBVTT\n\n");
            double elapsed = 0;
            for (int i = 0; i < lesson.scenes().size(); i++) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                progress.accept("Création de la diapositive " + (i + 1) + " / " + lesson.scenes().size() + "…");
                var scene = lesson.scenes().get(i);
                Path text = work.resolve("narration.txt"), audio = work.resolve("narration.wav");
                Files.writeString(text, scene.narration());
                if (speechProvider.equals("local") && localSpeech != null) localSpeech.synthesize(lesson.language(), scene.narration(), work, audio);
                else if (speech != null) speech.synthesize(lesson.language(), scene.narration(), audio);
                else run(work, List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-File", legacySpeech.toString(), "-Language", lesson.language(), "-TextFile", text.toString(), "-OutputFile", audio.toString()), 60);
                double duration;
                try (var stream = AudioSystem.getAudioInputStream(audio.toFile())) {
                    duration = stream.getFrameLength() / stream.getFormat().getFrameRate();
                }
                if (!Double.isFinite(duration) || duration < 0.1 || duration > 120) throw new IOException("Invalid narration duration");
                var captions = captions(scene.narration());
                int characters = captions.stream().mapToInt(String::length).sum();
                StringBuilder frames = new StringBuilder();
                double local = 0;
                for (int j = 0; j < captions.size(); j++) {
                    String name = "frame-" + j + ".png";
                    slide(work.resolve(name), lesson, scene, i, captions.get(j));
                    double length = duration * captions.get(j).length() / characters;
                    frames.append("file '").append(name).append("'\nduration ").append(String.format(Locale.ROOT, "%.6f", length)).append('\n');
                    vtt.append(timestamp(elapsed + local)).append(" --> ").append(timestamp(elapsed + local + length))
                        .append('\n').append(escapeCaption(captions.get(j))).append("\n\n");
                    local += length;
                }
                frames.append("file 'frame-").append(captions.size() - 1).append(".png'\n");
                Files.writeString(work.resolve("frames.txt"), frames);
                String clip = "scene-" + i + ".mp4";
                run(work, List.of(ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-f", "concat", "-safe", "1", "-i", "frames.txt",
                    "-i", "narration.wav", "-vf", "fps=24", "-c:v", "libx264", "-preset", "ultrafast", "-crf", "24",
                    "-pix_fmt", "yuv420p", "-threads", "2", "-c:a", "aac", "-b:a", "128k", "-t", String.format(Locale.ROOT, "%.6f", duration), clip), 180);
                clips.append("file '").append(clip).append("'\n"); elapsed += duration;
            }
            progress.accept("Assemblage de la vidéo…");
            Files.writeString(work.resolve("clips.txt"), clips);
            run(work, List.of(ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-f", "concat", "-safe", "1", "-i", "clips.txt",
                "-c", "copy", "-movflags", "+faststart", "lesson.mp4"), 60);
            Path result = work.resolve("lesson.mp4");
            if (Files.size(result) > 64L * 1024 * 1024) throw new IOException("Video exceeds storage limit");
            return new VideoLesson.Rendered(Files.readAllBytes(result), vtt.toString(), elapsed);
        } finally { cleanup(work); }
    }
    static List<String> captions(String text) {
        var result = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String word : text.strip().split("\\s+")) {
            if (!line.isEmpty() && line.length() + word.length() > 115) { result.add(line.toString()); line.setLength(0); }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) result.add(line.toString());
        return result;
    }
    private String escapeCaption(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private String timestamp(double value) {
        long millis = Math.round(value * 1000);
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", millis / 3600000, millis / 60000 % 60, millis / 1000 % 60, millis % 1000);
    }
    private Path workspace() throws IOException { Files.createDirectories(root); return Files.createTempDirectory(root, "lesson-"); }
    private Path speechScript(Path work) throws IOException {
        Path file = work.resolve("speech.ps1");
        try (var input = new ClassPathResource("video-speech.ps1").getInputStream()) { Files.copy(input, file); }
        return file;
    }
    private void run(Path work, List<String> args, int seconds) throws Exception {
        var builder = new ProcessBuilder(args).directory(work.toFile()).redirectErrorStream(true)
            .redirectOutput(work.resolve("process.log").toFile());
        var process = builder.start();
        try {
            if (!process.waitFor(seconds, TimeUnit.SECONDS)) throw new IOException("Video process timed out");
            if (process.exitValue() != 0) throw new IOException("Video process failed: " + process.exitValue());
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }
    private void cleanup(Path work) {
        if (work == null || !work.normalize().getParent().equals(root)) return;
        try (var files = Files.walk(work)) {
            for (var file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
        } catch (IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not remove temporary video workspace"); }
    }
    private void slide(Path file, VideoLesson lesson, VideoLesson.Scene scene, int index, String caption) throws IOException {
        var image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(20, 33, 52), 1280, 720, new Color(37, 65, 78)));
            g.fillRect(0, 0, 1280, 720);
            g.setColor(new Color(104, 222, 184)); g.fillRoundRect(52, 46, 8, 460, 8, 8);
            draw(g, "SMARTLEARN  /  " + (index + 1) + " · " + lesson.scenes().size(), 82, 64, 1110, 19, new Color(104, 222, 184), 1);
            draw(g, scene.title(), 82, 112, 1110, 38, Color.WHITE, 2);
            int y = 224;
            for (String bullet : scene.bullets()) { draw(g, "• " + bullet, 86, y, 1080, 26, new Color(231, 238, 245), 2); y += 70; }
            String label = switch (lesson.language()) { case "en" -> "ILLUSTRATIVE EXAMPLE"; case "ar" -> "مثال توضيحي"; default -> "EXEMPLE ILLUSTRATIF"; };
            g.setColor(new Color(255, 255, 255, 18)); g.fillRoundRect(78, 446, 1120, 112, 14, 14);
            draw(g, label, 98, 466, 1070, 16, new Color(104, 222, 184), 1);
            draw(g, scene.example(), 98, 498, 1070, 22, Color.WHITE, 2);
            draw(g, lesson.sourceLabel() + " " + scene.page() + "  ·  " + lesson.title(), 82, 580, 1110, 17, new Color(192, 210, 222), 1);
            g.setColor(new Color(8, 15, 26)); g.fillRect(0, 618, 1280, 102);
            draw(g, caption, 70, 638, 1140, 23, Color.WHITE, 2);
            g.setColor(new Color(104, 222, 184)); g.fillRect(0, 714, 1280 * (index + 1) / lesson.scenes().size(), 6);
        } finally { g.dispose(); }
        ImageIO.write(image, "PNG", file.toFile());
    }
    private void draw(Graphics2D g, String text, int x, int y, int width, int size, Color color, int maxLines) {
        var font = new Font("SansSerif", Font.PLAIN, size); g.setColor(color);
        var attributed = new AttributedString(text); attributed.addAttribute(java.awt.font.TextAttribute.FONT, font);
        var measurer = new LineBreakMeasurer(attributed.getIterator(), g.getFontRenderContext());
        for (int n = 0; n < maxLines && measurer.getPosition() < text.length(); n++) {
            var layout = measurer.nextLayout(width); y += (int) Math.ceil(layout.getAscent());
            layout.draw(g, layout.isLeftToRight() ? x : x + width - layout.getAdvance(), y);
            y += (int) Math.ceil(layout.getDescent() + layout.getLeading() + 5);
        }
    }
}
