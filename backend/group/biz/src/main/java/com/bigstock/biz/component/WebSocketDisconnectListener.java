package com.bigstock.biz.component;

import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.RequiredArgsConstructor;

/**
 * 監聽websocket 斷開事件
 */
@Component
@RequiredArgsConstructor
public class WebSocketDisconnectListener {

    private final StringRedisTemplate redis;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();

        if (sessionId == null) return;

        // 斷開時需要把對應的redis seesionId一併清除
        redis.delete("ws:session:" + sessionId);

        if (principal == null) return;

        String userId = principal.getName();
        String userKey = "ws:user:" + userId;

        String currentSession = redis.opsForValue().get(userKey);

        //將對應的ws:user資料一併移除
        if (sessionId.equals(currentSession)) {
            redis.delete(userKey);
        }
    }
}
