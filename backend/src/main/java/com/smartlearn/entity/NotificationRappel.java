package com.smartlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_rappels")
public class NotificationRappel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String typeRappel; // EXAMEN_24H, EXAMEN_1H, SEANCE_15MIN, etc.

    @Column(nullable = false, length = 200)
    private String cibleType; // EXAMEN, SEANCE

    @Column(nullable = false)
    private Long cibleId;

    @Column(nullable = false)
    private Long apprenantId;

    @Column(nullable = false)
    private LocalDateTime dateEnvoi;

    public NotificationRappel() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTypeRappel() {
        return typeRappel;
    }

    public void setTypeRappel(String typeRappel) {
        this.typeRappel = typeRappel;
    }

    public String getCibleType() {
        return cibleType;
    }

    public void setCibleType(String cibleType) {
        this.cibleType = cibleType;
    }

    public Long getCibleId() {
        return cibleId;
    }

    public void setCibleId(Long cibleId) {
        this.cibleId = cibleId;
    }

    public Long getApprenantId() {
        return apprenantId;
    }

    public void setApprenantId(Long apprenantId) {
        this.apprenantId = apprenantId;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }
}