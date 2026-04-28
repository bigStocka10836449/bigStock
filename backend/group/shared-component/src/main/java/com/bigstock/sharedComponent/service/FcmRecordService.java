package com.bigstock.sharedComponent.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.dto.FcmRegisterRequest;
import com.bigstock.sharedComponent.dto.FcmVerifyRequest;
import com.bigstock.sharedComponent.entity.FcmRecord;
import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.repository.FcmRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FcmRecordService {
	
	private final FcmRecordRepository fcmRecordRepository;

	private final CacheOperatorService cacheOperatorService;
	
	private final RedissonClient redissonClient;
	
	public FcmRecord getFcmRecord(String token) {
		FcmRecord fcmRecord = cacheOperatorService.getSnapshotData("ultraLongLivedCache", "FcmRecord:Valid:" + token, FcmRecord.class);
		
if(ObjectUtils.isEmpty(fcmRecord)) {
			return fcmRecord;
		} else {
			String lockKey = "lock:findByFcmToken:cacheName:ultraLongLivedCache:FcmRecord:getFcmRecord:"
					+ token;

			RLock lock = redissonClient.getLock(lockKey);
			boolean lockAcquired = false;
			try {

				lockAcquired = lock.tryLock(10, TimeUnit.MINUTES);

				if (lockAcquired) {
					fcmRecord =  cacheOperatorService.getSnapshotData("ultraLongLivedCache", "FcmRecord:Valid:" + token, FcmRecord.class);
					if(!ObjectUtils.isEmpty(fcmRecord)) {
						return fcmRecord;
					}
					fcmRecord = fcmRecordRepository.findByFcmToken(token);
							.findMarginTradingAndShortSellingInfoByDateRange(stockCode, firstDate, secondDate);
					cacheOperatorService.batchUpsertCompressedZSetSeries("ultraLongLivedCache",
							"marginTrading:compressed:" + stockCode, nonCacheMarginTradingAndShortSellingInfos,
							marginTradingAndShortSellingInfo -> marginTradingAndShortSellingInfo.getTradingDay().getTime(),
							CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
					return nonCacheMarginTradingAndShortSellingInfos;
				} else {
					throw new RuntimeException("Could not acquire lock for " + lockKey);
				}

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted while trying to acquire lock", e);
			} finally {

				if (lockAcquired && lock.isHeldByCurrentThread()) {
					lock.unlock();
				}

			}

		}
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
		cacheOperatorService.putSnapshotDataAtomic("ultraLongLivedCache", "FcmRecord:Valid:" + cacheKey, fcmRecord);
		cacheOperatorService.cleanupOldSnapshots("ultraLongLivedCache", "FcmRecord:Valid:" + cacheKey, 2);
		return fcmRecordRepository.save(fcmRecord);
	}
}
