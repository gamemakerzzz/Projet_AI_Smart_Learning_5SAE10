package com.smartlearn.service;

import com.smartlearn.entity.Examen;
import com.smartlearn.entity.Notification;
import com.smartlearn.entity.NotificationRappel;
import com.smartlearn.entity.Seance;
import com.smartlearn.repository.ExamenRepository;
import com.smartlearn.repository.NotificationRepository;
import com.smartlearn.repository.SeanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de planification des rappels automatiques.
 * Vérifie périodiquement les examens et séances approchants et envoie des notifications.
 * Utilise une table de tracking (NotificationRappel) pour éviter les doublons.
 */
@Service
@Transactional
public class NotificationRappelScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationRappelScheduler.class);

    private final NotificationRepository notificationRepository;
    private final NotificationRappelService rappelService;
    private final ExamenRepository examenRepository;
    private final SeanceRepository seanceRepository;
    private final NotificationService notificationService;

    public NotificationRappelScheduler(NotificationRepository notificationRepository,
                                       NotificationRappelService rappelService,
                                       ExamenRepository examenRepository,
                                       SeanceRepository seanceRepository,
                                       NotificationService notificationService) {
        this.notificationRepository = notificationRepository;
        this.rappelService = rappelService;
        this.examenRepository = examenRepository;
        this.seanceRepository = seanceRepository;
        this.notificationService = notificationService;
    }

    /**
     * Vérification toutes les 5 minutes (300000 ms).
     * Envoie les rappels 24h et 1h avant les examens, et 15min avant les séances.
     */
    @Scheduled(fixedDelay = 300000)
    public void checkRappels() {
        log.debug("Vérification des rappels automatiques...");
        LocalDateTime now = LocalDateTime.now();

        checkExamensRappel24h(now);
        checkExamensRappel1h(now);
        checkSeancesRappel15min(now);
    }

    private void checkExamensRappel24h(LocalDateTime now) {
        LocalDateTime debut = now.plusHours(23).plusMinutes(55);
        LocalDateTime fin = now.plusHours(24).plusMinutes(5);
        List<Examen> examens = examenRepository.findByDateExamenBetween(debut, fin);

        for (Examen examen : examens) {
            Long apprenantId = examen.getApprenant().getId();
            if (!rappelService.isRappelEnvoye("EXAMEN_24H", "EXAMEN", examen.getId(), apprenantId)) {
                try {
                    Notification notif = new Notification();
                    notif.setMessage("Rappel : votre examen \"" + examen.getTitre() + "\" est dans 24h (" + 
                        examen.getDateExamen().toLocalDate() + " à " + examen.getDateExamen().toLocalTime() + ").");
                    notif.setType("EXAMEN");
                    notif.setStatut("NON_LU");
                    notif.setDateCreation(now);
                    notif.setApprenant(examen.getApprenant());
                    notificationRepository.save(notif);

                    rappelService.enregistrerRappel("EXAMEN_24H", "EXAMEN", examen.getId(), apprenantId);
                    log.info("Rappel 24h envoyé pour examenId={}, apprenantId={}", examen.getId(), apprenantId);
                } catch (Exception e) {
                    log.error("Rappel 24h échoué pour examenId={}: {}", examen.getId(), e.getMessage());
                }
            }
        }
    }

    private void checkExamensRappel1h(LocalDateTime now) {
        LocalDateTime debut = now.plusMinutes(55);
        LocalDateTime fin = now.plusMinutes(65);
        List<Examen> examens = examenRepository.findByDateExamenBetween(debut, fin);

        for (Examen examen : examens) {
            Long apprenantId = examen.getApprenant().getId();
            if (!rappelService.isRappelEnvoye("EXAMEN_1H", "EXAMEN", examen.getId(), apprenantId)) {
                try {
                    Notification notif = new Notification();
                    notif.setMessage("Rappel : votre examen \"" + examen.getTitre() + "\" commence dans 1h (" + 
                        examen.getDateExamen().toLocalTime() + ").");
                    notif.setType("EXAMEN");
                    notif.setStatut("NON_LU");
                    notif.setDateCreation(now);
                    notif.setApprenant(examen.getApprenant());
                    notificationRepository.save(notif);

                    rappelService.enregistrerRappel("EXAMEN_1H", "EXAMEN", examen.getId(), apprenantId);
                    log.info("Rappel 1h envoyé pour examenId={}, apprenantId={}", examen.getId(), apprenantId);
                } catch (Exception e) {
                    log.error("Rappel 1h échoué pour examenId={}: {}", examen.getId(), e.getMessage());
                }
            }
        }
    }

    private void checkSeancesRappel15min(LocalDateTime now) {
        LocalDateTime debut = now.plusMinutes(10);
        LocalDateTime fin = now.plusMinutes(20);
        List<Seance> seances = seanceRepository.findByDateHeureDebutBetween(debut, fin);

        for (Seance seance : seances) {
            Long apprenantId = seance.getApprenant().getId();
            if (!rappelService.isRappelEnvoye("SEANCE_15MIN", "SEANCE", seance.getId(), apprenantId)) {
                try {
                    Notification notif = new Notification();
                    notif.setMessage("Rappel : votre séance de révision \"" + seance.getTitre() + "\" commence dans 15 min.");
                    notif.setType("SEANCE");
                    notif.setStatut("NON_LU");
                    notif.setDateCreation(now);
                    notif.setApprenant(seance.getApprenant());
                    notificationRepository.save(notif);

                    rappelService.enregistrerRappel("SEANCE_15MIN", "SEANCE", seance.getId(), apprenantId);
                    log.info("Rappel séance envoyé pour seanceId={}, apprenantId={}", seance.getId(), apprenantId);
                } catch (Exception e) {
                    log.error("Rappel séance échoué pour seanceId={}: {}", seance.getId(), e.getMessage());
                }
            }
        }
    }
}