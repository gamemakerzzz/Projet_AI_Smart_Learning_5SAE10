package com.smartlearn.service;

import com.smartlearn.dto.NotificationRequest;
import com.smartlearn.dto.NotificationResponse;
import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Notification;
import com.smartlearn.exception.ResourceNotFoundException;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final ApprenantRepository apprenantRepository;

    public NotificationService(NotificationRepository notificationRepository,
                              ApprenantRepository apprenantRepository) {
        this.notificationRepository = notificationRepository;
        this.apprenantRepository = apprenantRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllByApprenant(Long apprenantId) {
        return notificationRepository.findByApprenantIdOrderByIdAsc(apprenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllByApprenantAndType(Long apprenantId, String type) {
        return notificationRepository.findByApprenantIdAndType(apprenantId, type).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllByApprenantAndStatut(Long apprenantId, String statut) {
        return notificationRepository.findByApprenantIdAndStatut(apprenantId, statut).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(Long id) {
        Notification notification = getNotification(id);
        return toResponse(notification);
    }

    public NotificationResponse create(Long apprenantId, NotificationRequest request) {
        Apprenant apprenant = apprenantRepository.findById(apprenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant introuvable : " + apprenantId));

        Notification notification = new Notification();
        notification.setMessage(request.getMessage());
        notification.setType(request.getType() != null ? request.getType() : "INFO");
        notification.setStatut(request.getStatut() != null ? request.getStatut() : "NON_LU");
        notification.setDateCreation(java.time.LocalDateTime.now());
        notification.setApprenant(apprenant);

        Notification saved = notificationRepository.save(notification);
        log.debug("Notification créée: id={}, apprenantId={}", saved.getId(), apprenantId);
        return toResponse(saved);
    }

    public NotificationResponse update(Long id, NotificationRequest request) {
        Notification notification = getNotification(id);
        notification.setMessage(request.getMessage());
        notification.setType(request.getType() != null ? request.getType() : notification.getType());
        notification.setStatut(request.getStatut() != null ? request.getStatut() : notification.getStatut());

        Notification saved = notificationRepository.save(notification);
        log.debug("Notification mise à jour: id={}", saved.getId());
        return toResponse(saved);
    }

    public void delete(Long id) {
        Notification notification = getNotification(id);
        notificationRepository.delete(notification);
        log.debug("Notification supprimée: id={}", id);
    }

    private Notification getNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable : " + id));
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setMessage(notification.getMessage());
        response.setType(notification.getType());
        response.setStatut(notification.getStatut());
        response.setDateCreation(notification.getDateCreation());
        return response;
    }
}