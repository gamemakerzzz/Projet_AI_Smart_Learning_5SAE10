package com.smartlearn.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.FormationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:forum-insights;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "smartlearn.admin.password=test-admin-password",
    "smartlearn.forum.insights.cooldown-ms=0"})
@AutoConfigureMockMvc @Transactional
class ForumInsightsTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired DiscussionRepository discussions;
    @Autowired ApprenantRepository users;
    @Autowired FormationRepository courses;
    @MockitoBean ForumAiClient ai;

    @BeforeEach void connection() { when(ai.model()).thenReturn("test-model"); when(ai.configured()).thenReturn(true); }

    Discussion topic() {
        var author=users.findByEmailIgnoreCase("ahmed@example.com").orElseThrow();
        var d=new Discussion(); d.authorId=author.getId(); d.authorName=author.getPrenom();
        d.title="Protect Angular routes with a guard"; d.body="How do I block anonymous visitors from the dashboard?";
        return discussions.saveAndFlush(d);
    }
    @Test void tagsAreGeneratedEditableAndSaved() throws Exception {
        when(ai.structured(anyString(),any(),eq("forum_topic_tags"),anyMap()))
            .thenReturn(mapper.readTree("{\"tags\":[\"angular\",\"route-guards\"],\"explanation\":\"The question concerns route access.\"}"));
        String draft="{\"title\":\"Protect Angular dashboard routes\",\"body\":\"How can I prevent anonymous access?\",\"formationId\":null}";
        mvc.perform(post("/api/forum/ai/tags").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(draft))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.tags[1]").value("route-guards"));
        String edited=draft.substring(0,draft.length()-1)+",\"tags\":[\"angular\",\"authentication\"]}";
        mvc.perform(post("/api/forum/discussions").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(edited))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.discussion.tags[1]").value("authentication"));
        mvc.perform(get("/api/forum/discussions").param("search","authentication").with(user("sarah@example.com")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }
    @Test void summariesCiteExistingContentAndRejectInventedReferences() throws Exception {
        var d=topic();
        String summary="{\"overview\":{\"text\":\"The author asks how to protect routes.\",\"sourceIds\":[\"question\"]},\"keyPoints\":[],\"proposedSolutions\":[],\"unresolvedQuestions\":[],\"acceptedAnswerSummary\":\"\"}";
        when(ai.structured(anyString(),any(),eq("forum_discussion_summary"),anyMap())).thenReturn(mapper.readTree(summary));
        mvc.perform(post("/api/forum/discussions/"+d.id+"/ai/summary").with(user("ahmed@example.com")).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.overview.sources[0].id").value("question"))
            .andExpect(jsonPath("$.result.includedReplies").value(0));
        when(ai.structured(anyString(),any(),eq("forum_discussion_summary"),anyMap())).thenReturn(mapper.readTree(summary.replace("\"question\"","\"reply-999999\"")));
        mvc.perform(post("/api/forum/discussions/"+d.id+"/ai/summary").with(user("sarah@example.com")).with(csrf()))
            .andExpect(status().isBadGateway());
    }
    @Test void learningLinksResolveRealCoursesAndRejectInventedResources() throws Exception {
        var d=topic(); long course=courses.findAll().get(0).getId();
        String selection="{\"courses\":[{\"courseId\":"+course+",\"explanation\":\"Study the prerequisites.\"}],\"resources\":[{\"resourceId\":\"angular-guards\",\"explanation\":\"Official route guard documentation.\"}]}";
        when(ai.structured(anyString(),any(),eq("forum_learning_links"),anyMap())).thenReturn(mapper.readTree(selection));
        mvc.perform(post("/api/forum/discussions/"+d.id+"/ai/learning").with(user("ahmed@example.com")).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.courses[0].url").value("/formations/"+course))
            .andExpect(jsonPath("$.result.resources[0].url").value("https://angular.dev/guide/routing/route-guards"));
        when(ai.structured(anyString(),any(),eq("forum_learning_links"),anyMap())).thenReturn(mapper.readTree(selection.replace("angular-guards","invented-resource")));
        mvc.perform(post("/api/forum/discussions/"+d.id+"/ai/learning").with(user("sarah@example.com")).with(csrf()))
            .andExpect(status().isBadGateway());
    }
}
