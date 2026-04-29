package com.bigstock.biz.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.FcmRegisterRequest;
import com.bigstock.sharedComponent.dto.FcmVerifyRequest;
import com.bigstock.sharedComponent.service.FcmRecordService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.TopicManagementResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {
	
	private final FcmRecordService fcmRecordService;

	private final FirebaseMessaging firebaseMessaging;
	
    public void sendDeviceChallenge(FcmRegisterRequest request) {
        try {
        	String challengeId= fcmRecordService.register(request);
            Message message = Message.builder()
                    .setToken(request.getFcmToken())
                    .putData("type", "DEVICE_VERIFY")
                    .putData("challengeId", challengeId)
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("FCM sent successfully: " + response);

        } catch (FirebaseMessagingException e) {
            throw new RuntimeException("Failed to send FCM challenge", e);
        }
    }
    public void verify(FcmVerifyRequest request) throws FirebaseMessagingException {
    	fcmRecordService.verify(request);
    	subscribeMarketNews(request.getFcmToken());
    }
    
    public void subscribeMarketNews(String fcmToken) throws FirebaseMessagingException {
        TopicManagementResponse response =
                firebaseMessaging.subscribeToTopic(
                        List.of(fcmToken),
                        "market-news"
                );

        log.info("Subscribe topic success={}, failure={}",
                response.getSuccessCount(),
                response.getFailureCount());
    }
}