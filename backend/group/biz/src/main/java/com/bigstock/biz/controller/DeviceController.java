package com.bigstock.biz.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.biz.service.FcmService;
import com.bigstock.sharedComponent.dto.FcmRegisterRequest;
import com.bigstock.sharedComponent.dto.FcmVerifyRequest;
import com.google.firebase.messaging.FirebaseMessagingException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class DeviceController {

    private final FcmService fcmService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody FcmRegisterRequest request) {
    	fcmService.sendDeviceChallenge(request);
        return ResponseEntity.ok(Map.of(
                "message", "Device registered. FCM challenge sent."
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody FcmVerifyRequest request) throws FirebaseMessagingException {
    	fcmService.verify(request);
        return ResponseEntity.ok(Map.of(
                "message", "Device verified successfully."
        ));
    }
}
