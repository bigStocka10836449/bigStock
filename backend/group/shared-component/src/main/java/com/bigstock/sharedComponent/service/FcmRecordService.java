package com.bigstock.sharedComponent.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.dto.FcmRegisterRequest;
import com.bigstock.sharedComponent.dto.FcmVerifyRequest;
import com.bigstock.sharedComponent.entity.FcmRecord;
import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.repository.FcmRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FcmRecordService {
	
	private final FcmRecordRepository fcmRecordRepository;

	private final CacheOperatorService cacheOperatorService;
	
	public FcmRecord getFcmRecord(String token) {
		
	}
	
	public String register(FcmRegisterRequest request) {

		if (request.getFcmToken() == null || request.getFcmToken().isBlank()) {
			throw new IllegalArgumentException("fcmToken is required");
		}

		String challengeId = UUID.randomUUID().toString();

		FcmRecord device = new FcmRecord();

		device.setFcmToken(request.getFcmToken());
		device.setStatus("0");
		device.setChallenge(challengeId);
		device.setChallengeExpiresAt(
				Date.from(LocalDateTime.now().plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant()));
		device.setLastSeenAt(Date.from(LocalDateTime.now().plusMinutes(120).atZone(ZoneId.systemDefault()).toInstant()));

		fcmRecordRepository.save(device);
		
		return challengeId;
	}

	public void verify(FcmVerifyRequest request) {

		FcmRecord fcmRecord = fcmRecordRepository.findById(request.getFcmToken())
				.orElseThrow(() -> new RuntimeException("Device not found"));

		if (fcmRecord.getChallenge() == null) {
			throw new RuntimeException("No challenge found");
		}

		if (!fcmRecord.getChallenge().equals(request.getChallenge())) {
			throw new RuntimeException("Invalid challenge");
		}

		if (fcmRecord.getChallengeExpiresAt()
				.before(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))) {
			throw new RuntimeException("Challenge expired");
		}

		fcmRecord.setStatus("1");
		fcmRecord.setChallenge(null);
		fcmRecord.setChallengeExpiresAt(null);
		fcmRecord.setLastSeenAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));

		fcmRecordRepository.save(fcmRecord);
	}

	@Transactional(readOnly = true)
	public FcmRecord validateVerifiedDevice(String fcmToken) {

		FcmRecord device = fcmRecordRepository.findByFcmToken(fcmToken)
				.orElseThrow(() -> new RuntimeException("Invalid device or FCM token"));

		if (!"1".equals(device.getStatus())) {
		    throw new RuntimeException("Device is not verified");
		}

		return device;
	}
	
	public FcmRecord save(FcmRecord fcmRecord, String cacheKey) {
		return fcmRecordRepository.save(fcmRecord);
	}
}
