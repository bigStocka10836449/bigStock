package com.bigstock.biz.component;

import java.security.Principal;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.bigstock.biz.service.SocketSingleSessionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConnectInterceptor implements ChannelInterceptor {

    private final SocketSingleSessionService socketSingleSessionService;
    private final StringRedisTemplate redis;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;
        String userId = accessor.getUser().getName();
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();
        if (principal == null || sessionId == null) return message;
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            socketSingleSessionService.singleEnforce(userId, sessionId);
        }


		// 若使用者持續使用websocket，則延長TTL
		redis.expire("ws:user:" + userId, Duration.ofMinutes(30));
		redis.expire("ws:session:" + sessionId, Duration.ofMinutes(30));
        return message;
    }
}