package com.bigstock.sharedComponent.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "device_registry")
@Data
public class FcmRecord {

	@Id
    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "status")
    private String status;

    @Column(name = "challenge")
    private String challenge;

    @Column(name = "challenge_expires_at")
    private Date challengeExpiresAt;

    @Column(name = "last_seen_at")
    private Date lastSeenAt;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;
}
