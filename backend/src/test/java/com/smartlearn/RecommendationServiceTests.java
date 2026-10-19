package com.smartlearn;

import com.smartlearn.entity.*;
import com.smartlearn.repository.*;
import com.smartlearn.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RecommendationServiceTests {
    private RecommendationService service;
    private MockRestServiceServer server;
    @BeforeEach void setup() {
        var users = mock(ApprenantService.class);
        var courses = mock(FormationRepository.class);
        var skills = mock(CompetenceRepository.class);
        Apprenant learner = new Apprenant(); learner.setId(1L);
        Competence skill = new Competence(); skill.setNom("Python");
        Formation course = new Formation(); course.setId(12L); course.setTitre("Python basics");
        course.setNiveau("Débutant"); course.setPrix(BigDecimal.ZERO);
        when(users.getApprenant(1L)).thenReturn(learner);
        when(skills.findByApprenantId(1L)).thenReturn(List.of(skill));
        when(courses.findAllWithCategorie()).thenReturn(List.of(course));
        service = new RecommendationService(users, courses, skills);
        ReflectionTestUtils.setField(service, "mlaServiceUrl", "http://localhost:8000");
        server = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(service, "restTemplate")).build();
    }
    @Test void identifiesSuccessfulMachineLearningResults() {
        server.expect(requestTo("http://localhost:8000/recommend"))
            .andRespond(withSuccess("[{\"formationId\":12,\"score\":0.8}]", MediaType.APPLICATION_JSON));
        var result = service.getRecommendations(1L);
        assertEquals("TF_IDF", result.get(0).getMethode());
        assertEquals(0.8, result.get(0).getScore());
        server.verify();
    }
    @Test void identifiesKeywordFallbackWhenPythonIsUnavailable() {
        server.expect(requestTo("http://localhost:8000/recommend"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        var result = service.getRecommendations(1L);
        assertEquals("MOTS_CLES", result.get(0).getMethode());
        assertEquals(1.0, result.get(0).getScore());
        server.verify();
    }
}
