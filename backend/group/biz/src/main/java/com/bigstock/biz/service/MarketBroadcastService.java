package com.bigstock.biz.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MarketBroadcastService {

    private final SimpMessagingTemplate template;

    public void broadcastMarketData(String topic,Object payload) {
        template.convertAndSend(topic, payload);
    }
}
