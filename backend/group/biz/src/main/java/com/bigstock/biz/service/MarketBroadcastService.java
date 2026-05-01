package com.bigstock.biz.service;


import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class MarketBroadcastService {

    private final SimpMessagingTemplate template;
    
    private final FirebaseMessaging firebaseMessaging;

    public void broadcastMarketData(String topic,Object payload) {
        template.convertAndSend(topic, payload);
    }

    public void broadcastMarketData(String newsDataInfoStr, String title, String content) {
        try {

            Message message = Message.builder()
            	    .setTopic("market-news")
            	    .setNotification(Notification.builder()
            	        .setTitle("最新快訊")
            	        .setBody(StringUtils.isNotBlank(title) ? title : content)
            	        .build())
            	    .putData("type", "NEWS")
            	    .putData("data", newsDataInfoStr)
            	    .build();
            firebaseMessaging.send(message);

        } catch (Exception e) {
            log.error("Error sending FCM", e);
        }
    }
}
