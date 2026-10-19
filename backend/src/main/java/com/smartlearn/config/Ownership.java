package com.smartlearn.config;

import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.CompetenceRepository;
import com.smartlearn.repository.ExamenRepository;
import com.smartlearn.repository.NotificationRepository;
import com.smartlearn.repository.SeanceRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("ownership")
public class Ownership {
    private final ApprenantRepository users;
    private final CompetenceRepository competences;
    private final ExamenRepository examenRepository;
    private final SeanceRepository seanceRepository;
    private final NotificationRepository notificationRepository;
    public Ownership(ApprenantRepository users, CompetenceRepository competences, ExamenRepository examenRepository,
                     SeanceRepository seanceRepository, NotificationRepository notificationRepository) {
        this.users = users;
        this.competences = competences;
        this.examenRepository = examenRepository;
        this.seanceRepository = seanceRepository;
        this.notificationRepository = notificationRepository;
    }
    public boolean learner(Authentication auth, Long id) {
        return auth != null && users.findById(id)
            .map(a -> a.getEmail().equalsIgnoreCase(auth.getName())).orElse(false);
    }
    public boolean competence(Authentication auth, Long id) {
        return auth != null && competences.findById(id)
            .map(c -> learner(auth, c.getApprenant().getId())).orElse(false);
    }
    public boolean examen(Authentication auth, Long id) {
        return auth != null && examenRepository.findById(id)
            .map(e -> learner(auth, e.getApprenant().getId())).orElse(false);
    }
    public boolean seance(Authentication auth, Long id) {
        return auth != null && seanceRepository.findById(id)
            .map(s -> learner(auth, s.getApprenant().getId())).orElse(false);
    }
    public boolean notification(Authentication auth, Long id) {
        return auth != null && notificationRepository.findById(id)
            .map(n -> learner(auth, n.getApprenant().getId())).orElse(false);
    }
}
