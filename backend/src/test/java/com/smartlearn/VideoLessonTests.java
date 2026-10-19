package com.smartlearn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.flashcards.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static com.smartlearn.flashcards.FlashcardModels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:videos;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "smartlearn.admin.password=test-admin-password"})
@AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(roles = "USER")
class VideoLessonTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired FlashcardStore decks;
    @Autowired FlashcardVideoRepository videos;
    @Autowired VideoLessonService service;
    @MockitoBean FlashcardAiClient ai;
    @MockitoBean VideoRenderer renderer;
    static final String SOURCE = "A primary key uniquely identifies each row in a database table.";
    Cookie browser;
    String id;
    @BeforeEach void setup() throws Exception {
        var result = mvc.perform(get("/api/flashcards/decks")).andReturn();
        browser = result.getResponse().getCookie(FlashcardOwner.COOKIE);
        var digest = java.security.MessageDigest.getInstance("SHA-256").digest(browser.getValue().getBytes(StandardCharsets.UTF_8));
        String owner = HexFormat.of().formatHex(digest);
        id = UUID.randomUUID().toString();
        decks.save(owner, new Deck(id, "Databases", "lesson.pdf", "en", "beginner", 1,
            List.of(new Card(UUID.randomUUID().toString(), "Primary key?", "Unique identifier", 1, SOURCE))),
            new Document("lesson.pdf", 1, List.of(new Page(1, SOURCE))));
        when(renderer.languages()).thenReturn(List.of("fr", "en"));
        when(renderer.readinessMessage()).thenReturn("");
        when(renderer.voice(anyString())).thenReturn("Azure Speech · Test voice");
        when(renderer.render(any(), any())).thenReturn(new VideoLesson.Rendered("fake-mp4-for-http-tests".getBytes(StandardCharsets.UTF_8), "WEBVTT\n\n", 30));
        var scene = Map.of("title", "Primary keys", "bullets", List.of("A unique identifier", "One key per row"),
            "example", "A student identifier.", "narration", "A primary key uniquely identifies a row. For an illustrative example, imagine a student identifier.", "sourceId", "S1");
        when(ai.generate(anyString(), anyString(), eq("document_video_lesson"), anyMap()))
            .thenReturn(mapper.valueToTree(Map.of("title", "Database lesson", "scenes", List.of(scene, scene, scene))));
    }
    @Test void videoPersistsIsPrivateAndSupportsPlaybackSeekingAndCascadeDeletion() throws Exception {
        mvc.perform(get("/api/flashcards/decks/" + id + "/video").cookie(browser))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("none"));
        mvc.perform(post("/api/flashcards/decks/" + id + "/video").cookie(browser)).andExpect(status().isForbidden());
        mvc.perform(post("/api/flashcards/decks/" + id + "/video").with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/api/flashcards/decks/" + id + "/video").cookie(browser).with(csrf())).andExpect(status().isAccepted());
        awaitReady();
        mvc.perform(get("/api/flashcards/decks/" + id + "/video").cookie(browser))
            .andExpect(jsonPath("$.status").value("ready")).andExpect(jsonPath("$.lesson.scenes[0].page").value(1));
        mvc.perform(get("/api/flashcards/decks/" + id + "/video/media")).andExpect(status().isNotFound());
        mvc.perform(get("/api/flashcards/decks/" + id + "/video/media").cookie(browser).header("Range", "bytes=0-3"))
            .andExpect(status().isPartialContent()).andExpect(content().bytes("fake".getBytes(StandardCharsets.UTF_8)));
        mvc.perform(get("/api/flashcards/decks/" + id + "/video/media?download=true").cookie(browser))
            .andExpect(status().isOk()).andExpect(header().string("Content-Disposition", "attachment; filename=\"smartlearn-lesson.mp4\""));
        mvc.perform(get("/api/flashcards/decks/" + id + "/video/captions").cookie(browser)).andExpect(content().string("WEBVTT\n\n"));
        service.recover();
        mvc.perform(post("/api/flashcards/decks/" + id + "/video").cookie(browser).with(csrf())).andExpect(jsonPath("$.status").value("ready"));
        verify(ai, times(1)).generate(anyString(), anyString(), eq("document_video_lesson"), anyMap());
        when(ai.configured()).thenReturn(false);
        mvc.perform(post("/api/flashcards/decks/" + id + "/video?regenerate=true").cookie(browser).with(csrf())).andExpect(status().isAccepted());
        awaitReady();
        verify(ai, times(1)).generate(anyString(), anyString(), eq("document_video_lesson"), anyMap());
        mvc.perform(get("/api/flashcards/decks/" + id + "/video").cookie(browser)).andExpect(jsonPath("$.lesson.narrator").value("Azure Speech · Test voice"));
        mvc.perform(delete("/api/flashcards/decks/" + id).cookie(browser).with(csrf())).andExpect(status().isNoContent());
        assertFalse(videos.existsById(id));
    }
    @Test void rejectsUnsupportedNarrationLanguageBeforeSpendingAiAndCanRetryAfterRendererFailure() throws Exception {
        when(renderer.languages()).thenReturn(List.of("fr"));
        mvc.perform(post("/api/flashcards/decks/" + id + "/video").cookie(browser).with(csrf())).andExpect(status().isServiceUnavailable());
        verifyNoInteractions(ai);
        when(renderer.languages()).thenReturn(List.of("en"));
        when(renderer.render(any(), any())).thenThrow(new java.io.IOException("render failed"));
        mvc.perform(post("/api/flashcards/decks/" + id + "/video").cookie(browser).with(csrf())).andExpect(status().isAccepted());
        for (int n = 0; n < 100; n++) {
            String state = mapper.readTree(mvc.perform(get("/api/flashcards/decks/" + id + "/video").cookie(browser)).andReturn().getResponse().getContentAsString()).path("status").asText();
            if (state.equals("failed")) break;
            Thread.sleep(20);
        }
        mvc.perform(get("/api/flashcards/decks/" + id + "/video").cookie(browser)).andExpect(jsonPath("$.status").value("failed"));
        doReturn(new VideoLesson.Rendered(new byte[]{1, 2, 3}, "WEBVTT\n\n", 1)).when(renderer).render(any(), any());
        mvc.perform(post("/api/flashcards/decks/" + id + "/video").cookie(browser).with(csrf())).andExpect(status().isAccepted());
        awaitReady();
    }
    @Test void rejectsInventedSourceIdsAndDoesNotStartRenderer() throws Exception {
        var scene = Map.of("title", "Unsupported", "bullets", List.of("One", "Two"), "example", "Example", "narration", "Text", "sourceId", "S999");
        when(ai.generate(anyString(), anyString(), eq("document_video_lesson"), anyMap()))
            .thenReturn(mapper.valueToTree(Map.of("title", "Bad lesson", "scenes", List.of(scene, scene, scene))));
        mvc.perform(post("/api/flashcards/decks/" + id + "/video").cookie(browser).with(csrf())).andExpect(status().isBadGateway());
        verify(renderer, never()).render(any(), any());
        verify(ai, times(2)).generate(anyString(), anyString(), eq("document_video_lesson"), anyMap());
        mvc.perform(get("/api/flashcards/decks/" + id + "/video").cookie(browser))
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("source choisie")));
    }
    void awaitReady() throws Exception {
        for (int n = 0; n < 100; n++) {
            String state = mapper.readTree(mvc.perform(get("/api/flashcards/decks/" + id + "/video").cookie(browser)).andReturn().getResponse().getContentAsString()).path("status").asText();
            if (state.equals("ready")) return;
            if (state.equals("failed")) fail("Video rendering failed");
            Thread.sleep(20);
        }
        fail("Video rendering did not finish");
    }
}
