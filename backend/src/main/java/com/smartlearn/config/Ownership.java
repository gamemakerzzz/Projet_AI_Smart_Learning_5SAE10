package com.smartlearn.config;

import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.CompetenceRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("ownership")
public class Ownership {
    private final ApprenantRepository users;
    private final CompetenceRepository competences;
    public Ownership(ApprenantRepository users, CompetenceRepository competences) {
        this.users = users;
        this.competences = competences;
    }
    public boolean learner(Authentication auth, Long id) {
        return auth != null && users.findById(id)
            .map(a -> a.getEmail().equalsIgnoreCase(auth.getName())).orElse(false);
    }
    public boolean competence(Authentication auth, Long id) {
        return auth != null && competences.findById(id)
            .map(c -> learner(auth, c.getApprenant().getId())).orElse(false);
    }
}
