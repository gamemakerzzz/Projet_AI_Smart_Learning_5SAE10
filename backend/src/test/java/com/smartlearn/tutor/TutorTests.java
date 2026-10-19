package com.smartlearn.tutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.repository.FormationRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tutor-tests;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","smartlearn.admin.password=test-admin-password"})
@AutoConfigureMockMvc
class TutorTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired TutorConversationRepository conversations;
    @Autowired TutorMessageRepository messages;
    @Autowired FormationRepository courses;
    @MockitoBean TutorAiClient ai;
    @BeforeEach void setup() {
        messages.deleteAll(); conversations.deleteAll();
        when(ai.configured()).thenReturn(true); when(ai.model()).thenReturn("test-model"); when(ai.provider()).thenReturn("Groq");
        when(ai.chatMultiTurn(anyString(),anyList(),anyString())).thenAnswer(call->{
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),"Provider calls must not hold a database transaction");
            return "A primary key identifies a unique row.";
        });
    }
    long chat(String body) throws Exception {
        var result=mvc.perform(post("/api/tutor/chat").with(user("ahmed@example.com")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("conversationId").asLong();
    }
    @Test void requiresAuthenticationCsrfAndValidInput() throws Exception {
        mvc.perform(get("/api/tutor/status")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/tutor/chat").with(user("ahmed@example.com")).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Hello\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/tutor/chat").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/tutor/chat").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Hello\",\"formationId\":999999}"))
            .andExpect(status().isBadRequest());
        verify(ai,never()).chatMultiTurn(anyString(),anyList(),anyString());
    }
    @Test void chatUsesCourseChaptersAndPersistedMultiTurnHistory() throws Exception {
        long course=courses.findAll().stream().filter(c->c.getTitre().equals("Java pour débutants")).findFirst().orElseThrow().getId();
        long id=chat("{\"message\":\"Explain Java classes\",\"formationId\":"+course+"}");
        assertEquals(1,conversations.count()); assertEquals(2,messages.count());
        mvc.perform(get("/api/tutor/conversations/"+id).with(user("ahmed@example.com"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].role").value("user")).andExpect(jsonPath("$.messages[1].role").value("assistant"));
        chat("{\"message\":\"Give an example\",\"conversationId\":"+id+"}");
        verify(ai).chatMultiTurn(contains("Syntaxe"),argThat(history->history.size()==2 && history.get(0).get("content").equals("Explain Java classes")),eq("Give an example"));
        mvc.perform(get("/api/tutor/conversations").with(user("ahmed@example.com"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].messageCount").value(4));
        mvc.perform(get("/api/tutor/conversations").with(user("sarah@example.com"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void conversationIsPrivateAndDeletionRemovesMessages() throws Exception {
        long id=chat("{\"message\":\"Explain primary keys\"}");
        mvc.perform(get("/api/tutor/conversations/"+id).with(user("sarah@example.com"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tutor/conversations/"+id).with(user("sarah@example.com")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/tutor/chat").with(user("sarah@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"Continue\",\"conversationId\":"+id+"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tutor/conversations/"+id).with(user("ahmed@example.com")).with(csrf())).andExpect(status().isNoContent());
        assertEquals(0,messages.count()); assertEquals(0,conversations.count());
    }
    @Test void providerFailureDoesNotCreateEmptyHistory() throws Exception {
        when(ai.chatMultiTurn(anyString(),anyList(),anyString())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,"Provider unavailable"));
        mvc.perform(post("/api/tutor/chat").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"Explain primary keys\"}")).andExpect(status().isBadGateway());
        assertEquals(0,messages.count()); assertEquals(0,conversations.count());
    }
    @Test void explainSummarizeAndMindmapReturnValidatedResults() throws Exception {
        when(ai.chatJson(anyString(),anyString())).thenReturn("{\"explanation\":\"A unique identifier\",\"example\":\"Student number\",\"analogy\":\"A passport\",\"keyTakeaway\":\"Every row is unique\"}");
        mvc.perform(post("/api/tutor/explain").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"concept\":\"Primary key\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.example").value("Student number"));
        when(ai.chatJson(anyString(),anyString())).thenReturn("{\"summary\":\"Keys identify rows\",\"keyPoints\":[\"Unique\",\"Not null\"],\"estimatedReadTime\":\"1 minute\"}");
        mvc.perform(post("/api/tutor/summarize").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Primary keys identify rows uniquely.\",\"language\":\"en\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.keyPoints.length()").value(2));
        var leaf=Map.of("label","Leaf","description","Detail","children",List.of());
        var branch=Map.of("label","Branch","description","Concept","children",List.of(leaf,leaf));
        String map=mapper.writeValueAsString(Map.of("root",Map.of("label","Keys","description","Database keys","children",List.of(branch,branch,branch))));
        when(ai.chatJson(anyString(),anyString())).thenReturn(map);
        mvc.perform(post("/api/tutor/mindmap").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"topic\":\"Database keys\",\"language\":\"en\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.root.children.length()").value(3)).andExpect(jsonPath("$.root.children[0].children.length()").value(2));
    }
    @Test void invalidProviderOutputIsRejectedInsteadOfShowingEmptySuccess() throws Exception {
        when(ai.chatJson(anyString(),anyString())).thenReturn("{}");
        for(String endpoint:List.of("explain","summarize","mindmap")) {
            String input=switch(endpoint){case "explain"->"{\"concept\":\"Keys\"}";case "summarize"->"{\"text\":\"Keys identify rows.\"}";default->"{\"topic\":\"Keys\"}";};
            mvc.perform(post("/api/tutor/"+endpoint).with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isBadGateway());
        }
        mvc.perform(post("/api/tutor/mindmap").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"topic\":\"Keys\",\"language\":\"de\"}"))
            .andExpect(status().isBadRequest());
    }
}
