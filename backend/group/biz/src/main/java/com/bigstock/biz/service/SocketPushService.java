package com.bigstock.biz.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.bigstock.biz.dto.MarketSnapshot;
import com.corundumstudio.socketio.SocketIOClient;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocketPushService {

    // sessionId -> client
    private final Map<String, SocketIOClient> localClientMap = new ConcurrentHashMap<>();

    private final RedissonClient redissonClient;

    public static final String REDIS_CHANNEL = "market:broadcast";

    private volatile MarketSnapshot latestSnapshot;

    @PostConstruct
    public void subscribeRedisBroadcast() {
        RTopic topic = redissonClient.getTopic(REDIS_CHANNEL);

        // Subscribe to MarketSnapshot messages from Redis
        topic.addListener(MarketSnapshot.class, (channel, snapshot) -> {
            latestSnapshot = snapshot;
            broadcast(snapshot);
        });
    }

    public void registerClient(SocketIOClient client) {
        localClientMap.put(client.getSessionId().toString(), client);

        // send latest snapshot immediately (if exists)
        MarketSnapshot snapshot = latestSnapshot;
        if (snapshot != null && client.isChannelOpen()) {
            client.sendEvent("update", snapshot);
        }
    }

    public void removeClient(SocketIOClient client) {
        localClientMap.remove(client.getSessionId().toString());
    }

    public void broadcast(Object data) {
        // Remove dead clients while broadcasting to avoid memory leak
        localClientMap.entrySet().removeIf(entry -> {
            SocketIOClient c = entry.getValue();
            if (c == null || !c.isChannelOpen()) {
                return true;
            }
            c.sendEvent("update", data);
            return false;
        });
    }

    public MarketSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }
}