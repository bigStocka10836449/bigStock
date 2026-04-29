package com.bigstock.sharedComponent.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.FcmRecord;

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
}
