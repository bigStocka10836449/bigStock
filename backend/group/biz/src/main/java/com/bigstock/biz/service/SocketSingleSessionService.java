package com.bigstock.biz.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.biz.component.NodeIdentity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocketSingleSessionService {

    private final StringRedisTemplate redis;
    private final NodeIdentity nodeIdentity;

    public void singleEnforce(String userId, String newSessionId) {

        String userKey = "ws:user:" + userId;
        String oldSessionId = redis.opsForValue().get(userKey);

        //TODO 未來需要使用lua 防止 race condition的情形，這裡暫時先不處理
        if (oldSessionId != null && !oldSessionId.equals(newSessionId)) {
            publishKick(oldSessionId);
        }

        //增加TTL，預防Node 重啟時過去的session 變成垃圾資料
        redis.opsForValue().set(userKey, newSessionId, Duration.ofMinutes(30));
        redis.opsForValue().set("ws:session:" + newSessionId, nodeIdentity.getNodeId(), Duration.ofMinutes(30));
    }


    private void publishKick(String sessionId) {
        redis.convertAndSend("WS_KICK", sessionId);
    }
}