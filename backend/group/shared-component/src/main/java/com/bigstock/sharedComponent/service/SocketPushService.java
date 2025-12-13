package com.bigstock.sharedComponent.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.MQPayload;
import com.corundumstudio.socketio.SocketIOClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocketPushService {

	private final Map<String, SocketIOClient> localClientMap = new ConcurrentHashMap<>();

//	public final RedissonClient redissonClient;

	public final RabbitTemplate rabbitTemplate;
	
	private static final String SESSION_PREFIX = "socket:session:";
	
	private static final String SESSION_REVER_PREFIX = "socket:session_reverse:";

//	public void storeSessionIfAbsent(String sessionId, String userId, Duration ttl) {
//		redissonClient.getBucket(SESSION_PREFIX + userId).set(sessionId, ttl);
//		redissonClient.getBucket(SESSION_REVER_PREFIX + sessionId).set(userId, ttl);
//	}

//	public String getUserId(String sessionId) {
//		RBucket<String> bucket = redissonClient.getBucket(SESSION_REVER_PREFIX + sessionId);
//		return bucket.get();
//	}

//	public String getSessionId(String userId) {
//        return (String) redissonClient.getBucket(SESSION_PREFIX + userId).get();
//    }
	
//	public void registerClient(String userId, SocketIOClient client) {
//		localClientMap.put(userId, client);
//		redissonClient.getBucket(SESSION_PREFIX + userId).setIfAbsent(client.getSessionId(),  Duration.ofMinutes(30));
//	}

//	public void removeClient(SocketIOClient client) {
//		localClientMap.values().removeIf(c -> c.getSessionId().equals(client.getSessionId()));
//	}
//    public void sendToUser(String userId, Object data) {
//        SocketIOClient client = localClientMap.get(userId);
//        if (client != null) {
//            client.sendEvent("update", data);
//        } else {
//        	 RBucket<String> clientId = redissonClient.getBucket(SESSION_PREFIX + userId);
//            if (clientId.get() != null) {
//                MQPayload payload = new MQPayload(userId, data.toString());
//                rabbitTemplate.convertAndSend("exchange.socketio", "push", payload);
//            }
//        }
//    }
    
    public Map<String, SocketIOClient> getLocalClientMap() {
        return localClientMap;
    }
    

//    public String getClientId(String userId) {
//        RBucket<String> bucket = redissonClient.getBucket(SESSION_REVER_PREFIX + userId);
//        return bucket.get();
//    }
}
