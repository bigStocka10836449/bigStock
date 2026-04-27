package com.bigstock.biz.service;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.FcmRegisterRequest;
import com.bigstock.sharedComponent.dto.FcmVerifyRequest;
import com.bigstock.sharedComponent.service.FcmRecordService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {
	
	private final FcmRecordService fcmRecordService;

    public void sendDeviceChallenge(FcmRegisterRequest request) {
        try {
        	String challengeId= fcmRecordService.register(request);
            Message message = Message.builder()
                    .setToken(request.getFcmToken())
                    .putData("type", "DEVICE_VERIFY")
                    .putData("challengeId", challengeId)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM sent successfully: " + response);

        } catch (FirebaseMessagingException e) {
            throw new RuntimeException("Failed to send FCM challenge", e);
        }
    }
    public void verify(FcmVerifyRequest request) {
    	fcmRecordService.verify(request);
    }
}