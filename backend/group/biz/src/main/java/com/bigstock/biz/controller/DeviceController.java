package com.bigstock.biz.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.biz.service.FcmService;
import com.bigstock.biz.service.TurnstileService;
import com.bigstock.sharedComponent.dto.FcmRegisterRequest;
import com.bigstock.sharedComponent.dto.FcmVerifyRequest;
import com.bigstock.sharedComponent.dto.WebFcmRegisterRequest;
import com.google.firebase.messaging.FirebaseMessagingException;

import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class DeviceController {

    private final FcmService fcmService;
    private final TurnstileService turnstileService;

    // APP：原本流程不變
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody FcmRegisterRequest request) {

        fcmService.sendDeviceChallenge(request);

        return ResponseEntity.ok(Map.of(
                "message",
                "Device registered. FCM challenge sent."
        ));
    }

    // WEB：新增 Turnstile 保護
    @PostMapping("/web/register")
    public ResponseEntity<?> registerWeb(
            @RequestBody WebFcmRegisterRequest request) {

        turnstileService.verifyOrThrow(
                request.getTurnstileToken()
        );

        fcmService.sendDeviceChallenge(request);

        return ResponseEntity.ok(Map.of(
                "message",
                "Device registered. FCM challenge sent."
        ));
    }

    // 原本的裝置驗證不變
    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @RequestBody FcmVerifyRequest request)
            throws FirebaseMessagingException {

        fcmService.verify(request);

        return ResponseEntity.ok(Map.of(
                "message",
                "Device verified successfully."
        ));
    }
}