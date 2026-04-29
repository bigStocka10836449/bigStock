package com.bigstock.biz.infra;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseInitializer {

    @Value("${fcm.json:}")
    private String json;

    @Value("${fcm.json-base64:}")
    private String jsonBase64;

    @Value("${fcm.config-path:}")
    private String configPath;

    @PostConstruct
    public void init() throws Exception {

        InputStream serviceAccount = null;

        // 1Highest priority: raw JSON from ENV
        if (json != null && !json.isBlank()) {
            serviceAccount = new ByteArrayInputStream(
                    json.getBytes(StandardCharsets.UTF_8)
            );
            System.out.println("Using FCM config from ENV JSON");

        // 2Base64 fallback (safe for UI/env issues)
        } else if (jsonBase64 != null && !jsonBase64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(jsonBase64);
            serviceAccount = new ByteArrayInputStream(decoded);
            System.out.println("Using FCM config from ENV Base64");

        // 3File fallback (local dev / mounted secret)
        } else if (configPath != null && !configPath.isBlank()) {
        	serviceAccount =
        		    getClass().getClassLoader().getResourceAsStream("google-services.json");
            System.out.println("Using FCM config from file path");

        } else {
            throw new RuntimeException("No FCM configuration found");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
    }
    
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
