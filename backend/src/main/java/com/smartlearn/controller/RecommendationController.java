package com.smartlearn.controller;

import com.smartlearn.dto.RecommendationResponse;
import com.smartlearn.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #id)")
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/apprenant/{id}")
    public List<RecommendationResponse> getRecommendations(@PathVariable Long id) {
        return recommendationService.getRecommendations(id);
    }
}
