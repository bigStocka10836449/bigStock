package com.bigstock.biz.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class MarketBroadcastService {

    private static final String MARKET_NEWS_TOPIC = "market-news";

    private final SimpMessagingTemplate template;

    private final FirebaseMessaging firebaseMessaging;

    public void broadcastMarketData(String topic, Object payload) {
        template.convertAndSend(topic, payload);
    }

    public void broadcastMarketData(String newsDataInfoStr, String title, String content) {
        try {
            String safeTitle = StringUtils.defaultIfBlank(title, "最新快訊");
            String safeContent = StringUtils.defaultIfBlank(content, safeTitle);
            String safeData = StringUtils.defaultString(newsDataInfoStr);

            Message message = Message.builder()
                    .setTopic(MARKET_NEWS_TOPIC)

                    // 共用 data：Android / iOS 都可以收到
                    .putData("type", "NEWS")
                    .putData("title", safeTitle)
                    .putData("content", safeContent)
                    .putData("data", safeData)

                    // Android：只給 data，不設定 Android notification
                    // 這樣 Android 背景時不會產生 fcm_fallback_notification_channel / FCM-Notification:xxxx
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())

                    // iOS：用 APNs aps.alert 顯示系統通知
                    // 注意：這不是 top-level setNotification，所以不會讓 Android 走 Firebase fallback notification
                    .setApnsConfig(ApnsConfig.builder()
                            .putHeader("apns-priority", "10")
                            .putHeader("apns-push-type", "alert")
                            .setAps(Aps.builder()
                                    .setAlert(ApsAlert.builder()
                                            .setTitle(safeTitle)
                                            .setBody(safeContent)
                                            .build())
                                    .setSound("default")
                                    .build())
                            .build())

                    .build();

            String response = firebaseMessaging.send(message);
            log.info("Market news FCM sent successfully, topic={}, response={}", MARKET_NEWS_TOPIC, response);

        } catch (Exception e) {
            log.error("Error sending market news FCM", e);
        }
    }
}