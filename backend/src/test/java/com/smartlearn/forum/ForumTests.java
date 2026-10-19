package com.smartlearn.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.repository.ApprenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.http.MediaType;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:forum-tests;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","smartlearn.admin.password=test-admin-password"})
@AutoConfigureMockMvc @Transactional
class ForumTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired DiscussionRepository discussions; @Autowired ReplyRepository replies; @Autowired ApprenantRepository users;
    @MockitoBean ForumAiClient ai;
    String draft="{\"title\":\"How do Angular guards work?\",\"body\":\"I need to block anonymous access to the dashboard.\",\"formationId\":null}";
    Discussion topic(String email) {
        var user=users.findByEmailIgnoreCase(email).orElseThrow(); var d=new Discussion(); d.authorId=user.getId();d.authorName=user.getPrenom();d.title="Protect Angular routes with a guard";d.body="Block anonymous visitors from opening the dashboard.";return discussions.saveAndFlush(d);
    }
    @Test void anonymousAndMissingCsrfAreRejected() throws Exception {
        mvc.perform(get("/api/forum/discussions")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/forum/ai/check").with(user("ahmed@example.com")).contentType(MediaType.APPLICATION_JSON).content(draft)).andExpect(status().isForbidden());
        mvc.perform(post("/api/forum/discussions").with(user("ahmed@example.com")).contentType(MediaType.APPLICATION_JSON).content(draft)).andExpect(status().isForbidden());
    }
    @Test void learnerCanCreateBrowseEditAndDelete() throws Exception {
        var result=mvc.perform(post("/api/forum/discussions").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(draft))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.discussion.title").value("How do Angular guards work?")).andExpect(jsonPath("$.discussion.email").doesNotExist()).andReturn();
        long id=mapper.readTree(result.getResponse().getContentAsString()).path("discussion").path("id").asLong();
        mvc.perform(get("/api/forum/discussions").param("search","ANGULAR").with(user("sarah@example.com"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(put("/api/forum/discussions/"+id).with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(draft.replace("guards work","routes work"))).andExpect(status().isOk());
        mvc.perform(delete("/api/forum/discussions/"+id).with(user("ahmed@example.com")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/forum/discussions/"+id).with(user("ahmed@example.com"))).andExpect(status().isNotFound());
    }
    @Test void otherLearnerCannotEditDeleteOrAcceptButAdminCanModerate() throws Exception {
        var d=topic("ahmed@example.com");
        mvc.perform(put("/api/forum/discussions/"+d.id).with(user("sarah@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(draft)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/forum/discussions/"+d.id).with(user("sarah@example.com")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/forum/discussions/"+d.id+"/accepted-reply").with(user("sarah@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"replyId\":null}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/forum/discussions/"+d.id).with(user("admin@smartlearn.local").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
    }
    @Test void acceptReplyAndEditingAcceptedReplyReopensDiscussion() throws Exception {
        var d=topic("ahmed@example.com"); String url="/api/forum/discussions/"+d.id;
        var result=mvc.perform(post(url+"/replies").with(user("sarah@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Use a route guard and verify permissions on the backend.\"}"))
            .andExpect(status().isCreated()).andReturn();
        long id=mapper.readTree(result.getResponse().getContentAsString()).path("replies").get(0).path("id").asLong();
        mvc.perform(put(url+"/accepted-reply").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"replyId\":"+id+"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.discussion.acceptedReplyId").value(id));
        mvc.perform(put(url+"/replies/"+id).with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Overwrite another learner's answer.\"}")).andExpect(status().isForbidden());
        mvc.perform(put(url+"/replies/"+id).with(user("sarah@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Updated answer with details.\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.discussion.acceptedReplyId").isEmpty());
        mvc.perform(delete(url).with(user("ahmed@example.com")).with(csrf())).andExpect(status().isNoContent());
        assertEquals(0,replies.countByDiscussionId(d.id));
    }
    @Test void cannotAcceptReplyFromAnotherDiscussion() throws Exception {
        var a=topic("ahmed@example.com");var b=topic("sarah@example.com");var r=new Reply();r.discussionId=b.id;r.authorId=b.authorId;r.authorName=b.authorName;r.body="A different answer";replies.saveAndFlush(r);
        mvc.perform(put("/api/forum/discussions/"+a.id+"/accepted-reply").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"replyId\":"+r.id+"}"))
            .andExpect(status().isNotFound());
    }
    @Test void validatesTextCourseAndPagination() throws Exception {
        mvc.perform(post("/api/forum/discussions").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(draft.replace("How do Angular guards work?","        "))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/forum/discussions").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(draft.replace("null","999999"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/forum/discussions").param("page","-1").with(user("ahmed@example.com"))).andExpect(status().isBadRequest());
    }
    @Test void aiUsesRealCandidatesAndReturnsValidatedResults() throws Exception {
        var d=topic("ahmed@example.com");when(ai.model()).thenReturn("test-model");
        when(ai.classify(any(),any())).thenReturn(mapper.readTree("{\"results\":[{\"discussionId\":"+d.id+",\"relationship\":\"DUPLICATE\",\"explanation\":\"Both ask how to block anonymous access.\"}]}"));
        mvc.perform(post("/api/forum/ai/check").with(user("ahmed@example.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(draft))
            .andExpect(status().isOk()).andExpect(jsonPath("$.matches[0].discussionId").value(d.id)).andExpect(jsonPath("$.aiUsed").value(true));
        verify(ai).classify(any(),argThat(c->c.size()==1 && c.get(0).id().equals(d.id)));
    }
}
