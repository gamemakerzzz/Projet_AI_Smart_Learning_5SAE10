package com.smartlearn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.flashcards.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:flashcards;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "smartlearn.admin.password=test-admin-password"})
@AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(roles = "USER")
class FlashcardTests {
    static final String SOURCE = "A primary key uniquely identifies each row in a database table. Two students can share a name but each student has a unique identifier.";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PdfTextExtractor extractor;
    @Autowired FlashcardRepository savedCards;
    @Autowired FlashcardDeckRepository savedDecks;
    @MockitoBean FlashcardAiClient ai;

    byte[] pdf() throws Exception {
        try (var document = new PDDocument(); var bytes = new ByteArrayOutputStream()) {
            var page = new PDPage(); document.addPage(page);
            try (var content = new PDPageContentStream(document, page)) {
                content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.newLineAtOffset(30, 700); content.showText(SOURCE); content.endText();
            }
            document.save(bytes); return bytes.toByteArray();
        }
    }
    void ready() throws Exception {
        when(ai.configured()).thenReturn(true); when(ai.model()).thenReturn("gpt-6-luna");
        when(ai.generate(anyString(), anyString(), eq("pdf_flashcards"), anyMap())).thenReturn(mapper.readTree("""
            {"title":"Databases","cards":[
            {"question":"What is a primary key?","answer":"It uniquely identifies a row.","page":1,"source":"A primary key uniquely identifies each row in a database table."},
            {"question":"Invented card","answer":"Unsupported","page":1,"source":"This quote is not in the PDF document anywhere."}]}
            """));
    }
    @Test void publicFlowIsPrivateToSessionAndExplanationIsCached() throws Exception {
        ready();
        var result = mvc.perform(multipart("/api/flashcards").file(new MockMultipartFile("file", "lesson.pdf", "application/pdf", pdf()))
            .with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.cards.length()").value(1)).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        String id = mapper.readTree(result.getResponse().getContentAsString()).path("cards").get(0).path("id").asText();
        mvc.perform(get("/api/flashcards").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/flashcards")).andExpect(status().isNotFound());
        mvc.perform(post("/api/flashcards/cards/" + id + "/explain").with(csrf())).andExpect(status().isNotFound());
        when(ai.generate(anyString(), anyString(), eq("flashcard_explanation"), anyMap())).thenReturn(mapper.readTree(
            "{\"explanation\":\"A unique row identifier.\",\"example\":\"A student ID.\",\"takeaway\":\"Every row is unique.\"}"));
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/flashcards/cards/" + id + "/explain").session(session).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.example").value("A student ID."));
        verify(ai, times(1)).generate(anyString(), anyString(), eq("flashcard_explanation"), anyMap());
        mvc.perform(put("/api/flashcards/cards/" + id).session(session).with(csrf()).contentType("application/json")
            .content("{\"question\":\"Explain a primary key\",\"answer\":\"A unique identifier\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/flashcards/cards/" + id + "/explain").session(session).with(csrf())).andExpect(status().isOk());
        verify(ai, times(2)).generate(anyString(), anyString(), eq("flashcard_explanation"), anyMap());
        mvc.perform(delete("/api/flashcards").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/flashcards").session(session)).andExpect(status().isNotFound());
    }
    @Test void savedDecksSurviveFreshSessionsAndRejectOtherBrowsers() throws Exception {
        ready();
        var result = mvc.perform(multipart("/api/flashcards").file(new MockMultipartFile("file", "saved.pdf", "application/pdf", pdf()))
            .with(csrf())).andExpect(status().isOk()).andReturn();
        var browser = result.getResponse().getCookie(FlashcardOwner.COOKIE);
        assertNotNull(browser); assertTrue(browser.isHttpOnly()); assertTrue(browser.getMaxAge() > 0);
        var deck = mapper.readTree(result.getResponse().getContentAsString());
        String deckId = deck.path("id").asText(), cardId = deck.path("cards").get(0).path("id").asText();
        assertTrue(savedDecks.existsById(deckId)); assertTrue(savedCards.existsById(cardId));
        mvc.perform(get("/api/flashcards/decks/" + deckId).cookie(browser)).andExpect(status().isOk());
        mvc.perform(get("/api/flashcards/decks").cookie(browser)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(deckId));
        mvc.perform(get("/api/flashcards/decks/" + deckId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/flashcards/decks")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/flashcards/cards/" + cardId).with(csrf()).contentType("application/json")
            .content("{\"question\":\"Other browser\",\"answer\":\"Not allowed\"}")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/flashcards/cards/" + cardId + "/review").cookie(browser).with(csrf()).contentType("application/json")
            .content("{\"remembered\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/flashcards/decks/" + deckId).cookie(browser)).andExpect(jsonPath("$.cards[0].remembered").value(true));
        when(ai.generate(anyString(), anyString(), eq("flashcard_explanation"), anyMap())).thenReturn(mapper.readTree(
            "{\"explanation\":\"Persistent explanation\",\"example\":\"Example\",\"takeaway\":\"Takeaway\"}"));
        mvc.perform(post("/api/flashcards/cards/" + cardId + "/explain").cookie(browser).with(csrf())).andExpect(status().isOk());
        when(ai.configured()).thenReturn(false);
        mvc.perform(post("/api/flashcards/cards/" + cardId + "/explain").cookie(browser).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.explanation").value("Persistent explanation"));
        verify(ai, times(1)).generate(anyString(), anyString(), eq("flashcard_explanation"), anyMap());
        mvc.perform(delete("/api/flashcards/decks/" + deckId).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/flashcards/decks/" + deckId).cookie(browser).with(csrf())).andExpect(status().isNoContent());
        assertFalse(savedCards.existsById(cardId)); assertFalse(savedDecks.existsById(deckId));
    }
    @Test void multipleDocumentsRemainIndependentAndDeletingOneKeepsTheOther() throws Exception {
        ready();
        var first = mvc.perform(multipart("/api/flashcards")
            .file(new MockMultipartFile("file", "first.pdf", "application/pdf", pdf())).with(csrf()))
            .andExpect(status().isOk()).andReturn();
        var browser = first.getResponse().getCookie(FlashcardOwner.COOKIE);
        var firstDeck = mapper.readTree(first.getResponse().getContentAsString());
        String firstId = firstDeck.path("id").asText();
        String cardId = firstDeck.path("cards").get(0).path("id").asText();
        mvc.perform(patch("/api/flashcards/cards/" + cardId + "/review").cookie(browser).with(csrf())
            .contentType("application/json").content("{\"remembered\":true}"))
            .andExpect(status().isOk());
        var second = mvc.perform(multipart("/api/flashcards")
            .file(new MockMultipartFile("file", "second.pdf", "application/pdf", pdf())).cookie(browser).with(csrf()))
            .andExpect(status().isOk()).andReturn();
        String secondId = mapper.readTree(second.getResponse().getContentAsString()).path("id").asText();
        assertNotEquals(firstId, secondId);
        mvc.perform(get("/api/flashcards/decks").cookie(browser))
            .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/flashcards/decks/" + firstId).cookie(browser))
            .andExpect(jsonPath("$.filename").value("first.pdf"))
            .andExpect(jsonPath("$.cards[0].remembered").value(true));
        mvc.perform(get("/api/flashcards/decks/" + secondId).cookie(browser))
            .andExpect(jsonPath("$.filename").value("second.pdf"))
            .andExpect(jsonPath("$.cards[0].remembered").value(false));
        mvc.perform(delete("/api/flashcards/decks/" + secondId).cookie(browser).with(csrf()))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/flashcards/decks/" + firstId).cookie(browser))
            .andExpect(status().isOk()).andExpect(jsonPath("$.cards[0].remembered").value(true));
    }
    @Test void signedInAccessDoesNotOpenManagementOrDisableCsrf() throws Exception {
        when(ai.model()).thenReturn("gpt-6-luna");
        mvc.perform(get("/api/flashcards/status")).andExpect(status().isOk())
            .andExpect(jsonPath("$.maxUnits").value(300)).andExpect(jsonPath("$.maxTextCharacters").value(600000));
        mvc.perform(get("/api/apprenants")).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/flashcards").file(new MockMultipartFile("file", "test.pdf", "application/pdf", pdf())))
            .andExpect(status().isForbidden());
    }
    @Test @org.springframework.security.test.context.support.WithAnonymousUser
    void requiresSmartLearnLoginForCardsVideosAndOpenAiConnection() throws Exception {
        for (String path : new String[]{"/api/flashcards/status", "/api/flashcards", "/api/flashcards/decks", "/api/flashcards/video/capabilities", "/api/flashcards/decks/test/video/media"})
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/flashcards/openai/connect").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/flashcards").file(new MockMultipartFile("file", "test.pdf", "application/pdf", pdf())).with(csrf()))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/flashcards/openai/callback").param("state", "invalid")).andExpect(status().isSeeOther());
    }
    @Test void anyVisitorCanStartOpenAiSignInButMutationsStillNeedCsrf() throws Exception {
        mvc.perform(post("/api/flashcards/openai/connect")).andExpect(status().isForbidden());
        var result = mvc.perform(post("/api/flashcards/openai/connect").with(csrf())).andExpect(status().isOk())
            .andExpect(jsonPath("$.url").isString()).andReturn();
        String url = mapper.readTree(result.getResponse().getContentAsString()).path("url").asText();
        assertTrue(url.startsWith("https://auth.openai.com/api/accounts/authorize?"));
        assertTrue(url.contains("client_id=dynamic_agent_client"));
        mvc.perform(delete("/api/flashcards/openai").with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/flashcards/openai/callback").param("state", "invalid"))
            .andExpect(status().isSeeOther()).andExpect(header().string("Location", "http://localhost:4200/#/flashcards?openai=failed"));
    }
    @Test void rejectsBadInputAndMissingConfigurationWithoutCallingAi() throws Exception {
        ready();
        mvc.perform(multipart("/api/flashcards").file(new MockMultipartFile("file", "bad.pdf", "application/pdf", "not a pdf".getBytes()))
            .with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/flashcards").file(new MockMultipartFile("file", "test.pdf", "application/pdf", pdf()))
            .param("count", "200").with(csrf())).andExpect(status().isBadRequest());
        when(ai.configured()).thenReturn(false);
        mvc.perform(multipart("/api/flashcards").file(new MockMultipartFile("file", "test.pdf", "application/pdf", pdf()))
            .with(csrf())).andExpect(status().isServiceUnavailable());
        verify(ai, never()).generate(anyString(), anyString(), anyString(), anyMap());
    }
    @Test void rejectsBlankScannedAndInvalidPageRanges() throws Exception {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> extractor.extract(new MockMultipartFile("file", "test.pdf", "application/pdf", pdf()), 2, 1));
        try (var document = new PDDocument(); var bytes = new ByteArrayOutputStream()) {
            document.addPage(new PDPage()); document.save(bytes);
            var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> extractor.extract(new MockMultipartFile("file", "scan.pdf", "application/pdf", bytes.toByteArray()), 1, null));
            assertEquals(422, error.getStatusCode().value());
        }
    }
}
