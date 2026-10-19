package com.smartlearn.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.smartlearn.forum.ForumModels.*;

class ForumAiServiceTests {
    ObjectMapper mapper=new ObjectMapper();
    Discussion topic(long id) { var d=new Discussion();d.id=id;d.title="Angular guard question";d.body="Protect a route for signed-in users";return d; }
    String result(long id,String label) { return "{\"results\":[{\"discussionId\":"+id+",\"relationship\":\""+label+"\",\"explanation\":\"Both ask about access to routes.\"}]}"; }
    @Test void rejectsUnknownMissingRepeatedAndInvalidClassifications() throws Exception {
        var d=topic(1);
        for(String json:List.of(result(99,"DUPLICATE"),result(1,"CERTAIN"),"{\"results\":[]}","{\"results\":[{\"discussionId\":1,\"relationship\":\"RELATED\",\"explanation\":\"\"}]}"))
            assertThrows(ResponseStatusException.class,()->ForumAiService.validate(mapper.readTree(json),List.of(d)));
        var duplicate=mapper.readTree(result(1,"DUPLICATE"));((com.fasterxml.jackson.databind.node.ArrayNode)duplicate.path("results")).add(duplicate.path("results").get(0));
        assertThrows(ResponseStatusException.class,()->ForumAiService.validate(duplicate,List.of(d,topic(2))));
    }
    @Test void unrelatedMatchesAreNotShownAndTitlesComeFromDatabase() throws Exception {
        assertTrue(ForumAiService.validate(mapper.readTree(result(1,"UNRELATED")),List.of(topic(1))).isEmpty());
        assertEquals("Angular guard question",ForumAiService.validate(mapper.readTree(result(1,"DUPLICATE")),List.of(topic(1))).get(0).title());
    }
    @Test void cachesIdenticalInputAndInvalidatesWhenContentChanges() throws Exception {
        var repository=mock(DiscussionRepository.class);var client=mock(ForumAiClient.class);var d=topic(1);
        when(repository.browse(any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of(d)));
        when(client.model()).thenReturn("test");when(client.classify(any(),any())).thenReturn(mapper.readTree(result(1,"DUPLICATE")));
        var service=new ForumAiService(repository,mock(ForumService.class),client,0);var draft=new Draft("Protect routes","How do I protect my dashboard?",null);
        assertFalse(service.analyze(draft,null,1L).cached());assertTrue(service.analyze(draft,null,1L).cached());
        d.body="Updated question with a different constraint";
        assertFalse(service.analyze(draft,null,1L).cached());verify(client,times(2)).classify(any(),any());
    }
    @Test void limitsCandidatesAndTruncatesExistingBodies() throws Exception {
        var repository=mock(DiscussionRepository.class);var client=mock(ForumAiClient.class);
        var topics=new ArrayList<Discussion>();var output=mapper.createObjectNode();var results=output.putArray("results");
        for(int i=1;i<=21;i++){var d=topic(i);d.body="a".repeat(6000);topics.add(d);if(i<=20)results.addObject().put("discussionId",i).put("relationship","UNRELATED").put("explanation","Different question.");}
        when(repository.browse(any(),any(),any(),any())).thenReturn(new PageImpl<>(topics));when(client.model()).thenReturn("test");when(client.classify(any(),any())).thenReturn(output);
        var service=new ForumAiService(repository,mock(ForumService.class),client,0);
        var analysis=service.analyze(new Draft("Protect routes","How do I protect my dashboard?",null),null,1L);
        assertEquals(20,analysis.scannedCount());assertEquals(21,analysis.availableCount());
        verify(client).classify(any(),argThat(c->c.size()==20 && c.stream().allMatch(x->x.body().length()==1200)));
    }
    @Test void enforcesCooldownWithoutCachingFailures() {
        var repository=mock(DiscussionRepository.class);var client=mock(ForumAiClient.class);
        when(repository.browse(any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of(topic(1))));when(client.model()).thenReturn("test");
        when(client.classify(any(),any())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"offline"));
        var service=new ForumAiService(repository,mock(ForumService.class),client,10000);var draft=new Draft("Protect routes","How do I protect my dashboard?",null);
        assertEquals(503,assertThrows(ResponseStatusException.class,()->service.analyze(draft,null,1L)).getStatusCode().value());
        assertEquals(429,assertThrows(ResponseStatusException.class,()->service.analyze(draft,null,1L)).getStatusCode().value());
        verify(client,times(1)).classify(any(),any());
    }
    @Test void emptyForumDoesNotCallAI() {
        var repository=mock(DiscussionRepository.class);var client=mock(ForumAiClient.class);
        when(repository.browse(any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of()));when(client.model()).thenReturn("test");
        var service=new ForumAiService(repository,mock(ForumService.class),client,0);
        assertFalse(service.analyze(new Draft("Protect routes","How do I protect my dashboard?",null),null,1L).aiUsed());verify(client,never()).classify(any(),any());
    }
}
