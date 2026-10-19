package com.smartlearn.controller;

import com.smartlearn.repository.*;
import com.smartlearn.entity.Role;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final FormationRepository formations;
    private final CategorieRepository categories;
    private final ApprenantRepository learners;
    private final CompetenceRepository skills;
    public DashboardController(FormationRepository formations, CategorieRepository categories,
                               ApprenantRepository learners, CompetenceRepository skills) {
        this.formations = formations; this.categories = categories;
        this.learners = learners; this.skills = skills;
    }
    public record Summary(long formations, long categories, long apprenants, long competences, long profils) {}
    @GetMapping
    @Transactional(readOnly = true)
    public Summary summary() {
        return new Summary(formations.count(), categories.count(), learners.countByRole(Role.USER),
            skills.countByApprenantRole(Role.USER), learners.countByRoleAndProfilIsNotNull(Role.USER));
    }
}
