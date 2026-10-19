
package com.smartlearn.controller;

import com.smartlearn.dto.NotificationRequest;
import com.smartlearn.dto.NotificationResponse;
import com.smartlearn.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}")
    public List<NotificationResponse> getAllByApprenant(
            @PathVariable("apprenantId") Long apprenantId) {
        return notificationService.getAllByApprenant(apprenantId);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}/type/{type}")
    public List<NotificationResponse> getAllByApprenantAndType(
            @PathVariable("apprenantId") Long apprenantId,
            @PathVariable("type") String type) {
        return notificationService.getAllByApprenantAndType(apprenantId, type);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}/statut/{statut}")
    public List<NotificationResponse> getAllByApprenantAndStatut(
            @PathVariable("apprenantId") Long apprenantId,
            @PathVariable("statut") String statut) {
        return notificationService.getAllByApprenantAndStatut(apprenantId, statut);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @GetMapping("/apprenant/{apprenantId}/unread-count")
    public Map<String, Long> getUnreadCount(
            @PathVariable("apprenantId") Long apprenantId) {

        Map<String, Long> result = new HashMap<>();
        result.put("count", notificationService.countUnreadByApprenant(apprenantId));

        return result;
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @PostMapping("/apprenant/{apprenantId}")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse create(
            @PathVariable("apprenantId") Long apprenantId,
            @Valid @RequestBody NotificationRequest request) {
        return notificationService.create(apprenantId, request);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.notification(authentication, #id)")
    @PutMapping("/{id}")
    public NotificationResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody NotificationRequest request) {
        return notificationService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.notification(authentication, #id)")
    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable("id") Long id) {
        return notificationService.markAsRead(id);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @PatchMapping("/apprenant/{apprenantId}/mark-all-read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@PathVariable("apprenantId") Long apprenantId) {
        notificationService.markAllAsRead(apprenantId);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.notification(authentication, #id)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        notificationService.delete(id);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.learner(authentication, #apprenantId)")
    @DeleteMapping("/apprenant/{apprenantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllByApprenant(@PathVariable("apprenantId") Long apprenantId) {
        notificationService.deleteAllByApprenant(apprenantId);
    }
}
