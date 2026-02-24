package com.bigstock.biz.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.biz.dto.MarketSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocketPushService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public static final String REDIS_CHANNEL = "market:broadcast";

    private volatile MarketSnapshot latestSnapshot;

    /**
     * Publish snapshot to Redis
     * (Scheduler should call this)
     * @throws JsonProcessingException 
     */
    public void publish(MarketSnapshot snapshot) throws JsonProcessingException {
    	  ObjectMapper objectMapper = new ObjectMapper();
        redisTemplate.convertAndSend(REDIS_CHANNEL, objectMapper.writeValueAsString(snapshot));
    }

    /**
     * Called by Redis subscriber
     */
    public void broadcast(MarketSnapshot snapshot) {
        latestSnapshot = snapshot;

        // Broadcast to ALL connected WebSocket clients
        messagingTemplate.convertAndSend("/topic/market", snapshot);
    }

    public MarketSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }
}