package com.smartlearn;

import com.fasterxml.jackson.databind.*;
import com.smartlearn.flashcards.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.smartlearn.flashcards.FlashcardModels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VideoLessonPlannerTests {
    final ObjectMapper mapper = new ObjectMapper();
    final FlashcardAiClient ai = mock(FlashcardAiClient.class);
    final VideoLessonPlanner planner = new VideoLessonPlanner(ai);
    static final String SOURCE = "A primary key uniquely identifies each row in a database table.";
    final Deck deck = new Deck("id", "Database", "lesson.docx", "fr", "beginner", 40, List.of());
    final Document document = new Document("lesson.docx", 40, List.of(new Page(27, SOURCE)), "Section");

    JsonNode response(String sourceId, String narration) {
        var scene = Map.of("title", "Primary keys", "bullets", List.of("Unique identifier", "One per row"),
            "example", "Illustration: a student ID.", "narration", narration, "sourceId", sourceId);
        return mapper.valueToTree(Map.of("title", "Database lesson", "scenes", List.of(scene, scene, scene)));
    }
    @Test void attachesVerifiedQuoteAndOriginalSectionNumber() {
        when(ai.generate(anyString(), anyString(), anyString(), anyMap())).thenReturn(response("S1", "A primary key identifies a row."));
        var lesson = planner.plan(deck, document);
        assertEquals("Section", lesson.sourceLabel());
        assertEquals(27, lesson.scenes().get(0).page());
        assertEquals(SOURCE, lesson.scenes().get(0).source());
        verify(ai, times(1)).generate(anyString(), anyString(), anyString(), anyMap());
    }
    @Test void automaticallyRepairsOversizedNarration() {
        when(ai.generate(anyString(), anyString(), anyString(), anyMap()))
            .thenReturn(response("S1", "x".repeat(651)), response("S1", "A corrected explanation."));
        assertEquals("A corrected explanation.", planner.plan(deck, document).scenes().get(0).narration());
        verify(ai).generate(contains("scenes[0].narration.length"), anyString(), anyString(), anyMap());
        verify(ai, times(2)).generate(anyString(), anyString(), anyString(), anyMap());
    }
    @Test void rejectsUnknownSourcesAfterOneRepair() {
        when(ai.generate(anyString(), anyString(), anyString(), anyMap())).thenReturn(response("S999", "Explanation"));
        var error = assertThrows(ResponseStatusException.class, () -> planner.plan(deck, document));
        assertEquals(502, error.getStatusCode().value());
        assertTrue(error.getReason().contains("source choisie"));
        verify(ai, times(2)).generate(anyString(), anyString(), anyString(), anyMap());
    }
    @Test void samplesLongDocumentsIncludingLastPageWithoutUnboundedInput() {
        var pages = new ArrayList<Page>();
        for (int n = 1; n <= 300; n++) pages.add(new Page(n, ("Page " + n + " explains database concepts. ").repeat(60)));
        when(ai.generate(anyString(), anyString(), anyString(), anyMap())).thenAnswer(call -> {
            String input = call.getArgument(1);
            assertTrue(input.length() < 50000);
            assertTrue(input.contains("unit=\"300\""));
            assertTrue(input.contains("unit=\"1\""));
            return response("S1", "Explanation");
        });
        planner.plan(deck, new Document("long.pdf", 300, pages));
    }
    @Test void rejectsInsufficientTextBeforeCallingAi() {
        var error = assertThrows(ResponseStatusException.class,
            () -> planner.plan(deck, new Document("empty.pdf", 1, List.of(new Page(1, "")))));
        assertEquals(422, error.getStatusCode().value());
        verifyNoInteractions(ai);
    }
    @Test void doesNotRetryProviderErrors() {
        when(ai.generate(anyString(), anyString(), anyString(), anyMap()))
            .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "Quota"));
        assertEquals(429, assertThrows(ResponseStatusException.class, () -> planner.plan(deck, document)).getStatusCode().value());
        verify(ai, times(1)).generate(anyString(), anyString(), anyString(), anyMap());
    }
}
