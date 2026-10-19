package com.smartlearn.service;

import com.smartlearn.entity.NotificationRappel;
import com.smartlearn.repository.NotificationRappelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class NotificationRappelService {

    private static final Logger log = LoggerFactory.getLogger(NotificationRappelService.class);

    private final NotificationRappelRepository rappelRepository;

    public NotificationRappelService(NotificationRappelRepository rappelRepository) {
        this.rappelRepository = rappelRepository;
    }

    /**
     * Vérifie si un rappel a déjà été envoyé pour un événement donné.
     */
    public boolean isRappelEnvoye(String typeRappel, String cibleType, Long cibleId, Long apprenantId) {
        return rappelRepository.existsByTypeRappelAndCibleTypeAndCibleIdAndApprenantId(
                typeRappel, cibleType, cibleId, apprenantId);
    }

    /**
     * Enregistre un rappel comme envoyé.
     */
    public void enregistrerRappel(String typeRappel, String cibleType, Long cibleId, Long apprenantId) {
        NotificationRappel rappel = new NotificationRappel();
        rappel.setTypeRappel(typeRappel);
        rappel.setCibleType(cibleType);
        rappel.setCibleId(cibleId);
        rappel.setApprenantId(apprenantId);
        rappel.setDateEnvoi(java.time.LocalDateTime.now());
        rappelRepository.save(rappel);
        log.debug("Rappel enregistré: type={}, cible={}, apprenantId={}", typeRappel, cibleType + ":" + cibleId, apprenantId);
    }

    /**
     * Supprime tous les rappels associés à une entité (ex: un examen supprimé).
     */
    public void supprimerRappelsParCible(String cibleType, Long cibleId) {
        rappelRepository.deleteByCibleTypeAndCibleId(cibleType, cibleId);
        log.debug("Rappels supprimés pour cible={}:{}", cibleType, cibleId);
    }
}