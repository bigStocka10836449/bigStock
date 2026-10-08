package com.bigstock.sharedComponent.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.entity.FcmRecord;
import com.bigstock.sharedComponent.enums.DeviceType;

public interface FcmRecordRepository extends JpaRepository<FcmRecord, String> {

    Optional<FcmRecord> findByFcmToken(
            String fcmToken
    );
    
    @Query("""
			 SELECT e
			 FROM FcmRecord e
			 WHERE e.allowedJwt = :jwt
			""")
	Optional<FcmRecord> findByAllowedJwt(@Param("jwt") String token);
    
    @Transactional
    int deleteByDeviceTypeAndLastSeenAtBefore(
            DeviceType deviceType,
            LocalDateTime cutoff
    );
}
