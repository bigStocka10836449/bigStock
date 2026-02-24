package com.bigstock.biz.component;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class SocketIOHandler  {

    private final SimpMessagingTemplate messagingTemplate;

    public void handleMessage(String message) {
        messagingTemplate.convertAndSend("/topic/market", message);
    }
}