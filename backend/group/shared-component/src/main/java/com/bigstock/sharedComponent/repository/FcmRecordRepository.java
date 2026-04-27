package com.bigstock.sharedComponent.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigstock.sharedComponent.entity.FcmRecord;

public interface FcmRecordRepository extends JpaRepository<FcmRecord, String> {

    Optional<FcmRecord> findByFcmToken(
            String fcmToken
    );
}
