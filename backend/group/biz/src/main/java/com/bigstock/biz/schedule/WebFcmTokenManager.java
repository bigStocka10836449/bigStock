package com.bigstock.biz.schedule;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.enums.DeviceType;
import com.bigstock.sharedComponent.service.FcmRecordService;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class WebFcmTokenManager {

	private final FcmRecordService fcmRecordService;

	@Scheduled(cron = "0 0 4 * * *")
	@Transactional
	public void cleanupWebFcmTokens() throws JsonProcessingException {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
		int deleted = fcmRecordService.deleteByDeviceTypeAndLastSeenAtBefore(DeviceType.WEB, cutoff);

		log.info("Cleaned expired WEB FCM devices, count={}", deleted);
	}
}
